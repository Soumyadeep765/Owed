package com.soumya.owed

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class Entry(
    val id: String,
    val name: String,
    val isMoney: Boolean,
    val amount: Double,
    val desc: String,
    val date: Long,
    val due: Long?,
    val rate: Double,
    val note: String,
    val paid: Double = 0.0,
    val returned: Boolean = false,
    val returnedAt: Long? = null
)

private fun zone(): ZoneId = ZoneId.systemDefault()

fun ld(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone()).toLocalDate()

fun todayMs(): Long = LocalDate.now().atStartOfDay(zone()).toInstant().toEpochMilli()

fun pickerToLocal(ms: Long): Long =
    Instant.ofEpochMilli(ms).atZone(ZoneId.of("UTC")).toLocalDate()
        .atStartOfDay(zone()).toInstant().toEpochMilli()

fun fmt(ms: Long): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).format(ld(ms))

fun inr(v: Double): String =
    "₹" + NumberFormat.getIntegerInstance(Locale("en", "IN")).format(Math.round(v))

fun daysBetween(a: Long, b: Long): Long = ChronoUnit.DAYS.between(ld(a), ld(b))

fun interest(e: Entry): Double {
    if (!e.isMoney || e.rate <= 0) return 0.0
    val end = e.returnedAt ?: todayMs()
    val d = maxOf(0L, daysBetween(e.date, end))
    return e.amount * e.rate / 100 * d / 30
}

fun owing(e: Entry): Double = maxOf(0.0, e.amount + interest(e) - e.paid)

class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("owed", Context.MODE_PRIVATE)

    fun load(): List<Entry> {
        val a = JSONArray(p.getString("entries", "[]"))
        return List(a.length()) {
            val o = a.getJSONObject(it)
            Entry(
                id = o.getString("id"),
                name = o.getString("name"),
                isMoney = o.getBoolean("isMoney"),
                amount = o.getDouble("amount"),
                desc = o.getString("desc"),
                date = o.getLong("date"),
                due = o.getLong("due").takeIf { d -> d >= 0 },
                rate = o.getDouble("rate"),
                note = o.getString("note"),
                paid = o.getDouble("paid"),
                returned = o.getBoolean("returned"),
                returnedAt = o.getLong("returnedAt").takeIf { d -> d >= 0 }
            )
        }
    }

    fun save(list: List<Entry>) {
        val a = JSONArray()
        list.forEach {
            a.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("isMoney", it.isMoney)
                    .put("amount", it.amount)
                    .put("desc", it.desc)
                    .put("date", it.date)
                    .put("due", it.due ?: -1L)
                    .put("rate", it.rate)
                    .put("note", it.note)
                    .put("paid", it.paid)
                    .put("returned", it.returned)
                    .put("returnedAt", it.returnedAt ?: -1L)
            )
        }
        p.edit().putString("entries", a.toString()).apply()
    }
}

object Reminders {
    private fun intent(ctx: Context, e: Entry, flags: Int): PendingIntent? {
        val text = (if (e.isMoney) inr(owing(e)) else e.desc) + " from " + e.name
        val i = Intent(ctx, ReminderReceiver::class.java)
            .putExtra("id", e.id)
            .putExtra("text", text)
        return PendingIntent.getBroadcast(ctx, e.id.hashCode(), i, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    fun schedule(ctx: Context, e: Entry) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val due = e.due
        if (e.returned || due == null) {
            cancel(ctx, e)
            return
        }
        val at = due + 9 * 3600_000L
        if (at <= System.currentTimeMillis()) {
            cancel(ctx, e)
            return
        }
        intent(ctx, e, PendingIntent.FLAG_UPDATE_CURRENT)?.let { pendingIntent ->
    am.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        at,
        pendingIntent
    )
}
    }

    fun cancel(ctx: Context, e: Entry) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        intent(ctx, e, PendingIntent.FLAG_UPDATE_CURRENT)?.let { am.cancel(it) }
    }
}
