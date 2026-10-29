package com.soumya.owed

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Currency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class Store(private val ctx: Context) {
    private val p = ctx.getSharedPreferences("owed", Context.MODE_PRIVATE)

    fun load(): List<Entry> {
        val raw = p.getString("entries", "[]") ?: "[]"
        val array = try {
            JSONArray(raw)
        } catch (_: Exception) {
            return emptyList()
        }
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                entryFrom(o)?.let { add(it) }
            }
        }
    }

    fun save(list: List<Entry>) {
        val array = JSONArray()
        list.forEach { e ->
            val payments = JSONArray()
            e.payments.forEach { pay ->
                payments.put(
                    JSONObject()
                        .put("id", pay.id)
                        .put("amount", pay.amount)
                        .put("date", pay.date)
                        .put("note", pay.note)
                )
            }
            array.put(
                JSONObject()
                    .put("id", e.id)
                    .put("name", e.name)
                    .put("isMoney", e.isMoney)
                    .put("amount", e.amount)
                    .put("desc", e.desc)
                    .put("date", e.date)
                    .put("due", e.due ?: -1L)
                    .put("rate", e.rate)
                    .put("note", e.note)
                    .put("paid", e.paid)
                    .put("returned", e.returned)
                    .put("returnedAt", e.returnedAt ?: -1L)
                    .put("direction", e.direction)
                    .put("phone", e.phone)
                    .put("payments", payments)
            )
        }
        p.edit().putString("entries", array.toString()).apply()
        BalanceWidget.update(ctx)
    }

    fun loadAlerts(): List<AppAlert> {
        val raw = p.getString("alerts", "[]") ?: "[]"
        val array = try {
            JSONArray(raw)
        } catch (_: Exception) {
            return emptyList()
        }
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val id = o.optString("id")
                if (id.isBlank()) continue
                add(
                    AppAlert(
                        id = id,
                        entryId = o.optString("entryId"),
                        kind = o.optString("kind"),
                        title = o.optString("title"),
                        body = o.optString("body"),
                        time = o.optLong("time"),
                        read = o.optBoolean("read")
                    )
                )
            }
        }
    }

    fun saveAlerts(list: List<AppAlert>) {
        val array = JSONArray()
        list.take(80).forEach { a ->
            array.put(
                JSONObject()
                    .put("id", a.id)
                    .put("entryId", a.entryId)
                    .put("kind", a.kind)
                    .put("title", a.title)
                    .put("body", a.body)
                    .put("time", a.time)
                    .put("read", a.read)
            )
        }
        p.edit().putString("alerts", array.toString()).apply()
    }

    fun appendAlert(alert: AppAlert) {
        val current = loadAlerts()
        if (current.any { it.id == alert.id }) return
        saveAlerts(listOf(alert) + current)
    }

    fun notificationsEnabled(): Boolean = p.getBoolean("notify", true)

    fun setNotificationsEnabled(on: Boolean) {
        p.edit().putBoolean("notify", on).apply()
    }

    fun reminderHour(): Int = p.getInt("hour", 9).let { if (it in 0..23) it else 9 }

    fun setReminderHour(hour: Int) {
        p.edit().putInt("hour", hour.coerceIn(0, 23)).apply()
    }

    fun remindDaysBefore(): Int = p.getInt("lead", 1).let { if (it == 0 || it == 1 || it == 3) it else 1 }

    fun setRemindDaysBefore(days: Int) {
        p.edit().putInt("lead", days).apply()
    }

    fun appearance(): String =
        p.getString("appearance", "system")?.takeIf { it == "light" || it == "dark" || it == "system" } ?: "system"

    fun setAppearance(value: String) {
        p.edit().putString("appearance", value).apply()
    }

    fun language(): String = p.getString("lang", "en")?.takeIf { code -> Languages.any { it.code == code } } ?: "en"

    fun setLanguage(code: String) {
        p.edit().putString("lang", code).apply()
    }

    fun currency(): String = p.getString("currency", "INR")?.takeIf { code -> Currencies.any { it.code == code } } ?: "INR"

    fun setCurrency(code: String) {
        p.edit().putString("currency", code).apply()
    }

    fun introSeen(): Boolean = p.getBoolean("intro", false)

    fun setIntroSeen() {
        p.edit().putBoolean("intro", true).apply()
    }

    fun lockEnabled(): Boolean = p.getBoolean("lock", false)

    fun setLockEnabled(on: Boolean) {
        p.edit().putBoolean("lock", on).apply()
    }

    fun backupJson(): String {
        return JSONObject()
            .put("app", "owed")
            .put("version", 1)
            .put("entries", JSONArray(p.getString("entries", "[]")))
            .put("alerts", JSONArray(p.getString("alerts", "[]")))
            .put("notify", notificationsEnabled())
            .put("hour", reminderHour())
            .put("lead", remindDaysBefore())
            .put("appearance", appearance())
            .put("lang", language())
            .put("currency", currency())
            .put("lock", lockEnabled())
            .toString()
    }

    fun restoreJson(raw: String): Boolean {
        val root = try {
            JSONObject(raw)
        } catch (_: Exception) {
            return false
        }
        if (root.optString("app") != "owed") return false
        val entries = root.optJSONArray("entries") ?: return false
        for (i in 0 until entries.length()) {
            val row = entries.optJSONObject(i) ?: return false
            if (entryFrom(row) == null) return false
        }
        val lang = root.optString("lang", language())
        val money = root.optString("currency", currency())
        val look = root.optString("appearance", appearance())
        val lead = root.optInt("lead", 1).let { if (it == 0 || it == 1 || it == 3) it else 1 }
        p.edit()
            .putString("entries", entries.toString())
            .putString("alerts", (root.optJSONArray("alerts") ?: JSONArray()).toString())
            .putBoolean("notify", root.optBoolean("notify", true))
            .putInt("hour", root.optInt("hour", 9).coerceIn(0, 23))
            .putInt("lead", lead)
            .putString("appearance", look.takeIf { it == "light" || it == "dark" || it == "system" } ?: "system")
            .putString("lang", lang.takeIf { code -> Languages.any { it.code == code } } ?: "en")
            .putString("currency", money.takeIf { code -> Currencies.any { it.code == code } } ?: "INR")
            .putBoolean("lock", root.optBoolean("lock", lockEnabled()))
            .apply()
        BalanceWidget.update(ctx)
        return true
    }
}

private fun entryFrom(o: JSONObject): Entry? {
    val id = o.optString("id")
    if (id.isBlank()) return null
    val payments = buildList {
        val array = o.optJSONArray("payments") ?: return@buildList
        for (i in 0 until array.length()) {
            val p = array.optJSONObject(i) ?: continue
            val pid = p.optString("id")
            val amount = p.optDouble("amount", Double.NaN)
            if (pid.isBlank() || amount.isNaN()) continue
            add(Payment(pid, amount, p.optLong("date"), p.optString("note")))
        }
    }
    return Entry(
        id = id,
        name = o.optString("name").ifBlank { "Someone" },
        isMoney = o.optBoolean("isMoney", true),
        amount = o.optDouble("amount", 0.0).let { if (it.isNaN()) 0.0 else it },
        desc = o.optString("desc"),
        date = o.optLong("date", todayMs()),
        due = o.optLong("due", -1L).takeIf { it >= 0 },
        rate = o.optDouble("rate", 0.0).let { if (it.isNaN()) 0.0 else it },
        note = o.optString("note"),
        paid = o.optDouble("paid", 0.0).let { if (it.isNaN()) 0.0 else it },
        returned = o.optBoolean("returned", false),
        returnedAt = o.optLong("returnedAt", -1L).takeIf { it >= 0 },
        direction = if (o.optInt("direction", LENT) == BORROWED) BORROWED else LENT,
        phone = o.optString("phone"),
        payments = payments
    )
}
