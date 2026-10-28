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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

const val LENT = 0
const val BORROWED = 1
const val APP_VERSION = "1.1"

data class Payment(
    val id: String,
    val amount: Double,
    val date: Long,
    val note: String
)

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
    val returnedAt: Long? = null,
    val direction: Int = LENT,
    val phone: String = "",
    val payments: List<Payment> = emptyList()
)

data class AppAlert(
    val id: String,
    val entryId: String,
    val kind: String,
    val title: String,
    val body: String,
    val time: Long,
    val read: Boolean
)

data class Banner(
    val id: String,
    val title: String,
    val body: String,
    val kind: String,
    val entryId: String?
)

data class Person(
    val key: String,
    val name: String,
    val phone: String,
    val entries: List<Entry>
) {
    val open: List<Entry> get() = entries.filter { !it.returned }
    val owedToYou: Double
        get() = open.filter { it.isMoney && it.direction == LENT }.sumOf { owing(it) }
    val youOwe: Double
        get() = open.filter { it.isMoney && it.direction == BORROWED }.sumOf { owing(it) }
    val net: Double get() = owedToYou - youOwe
}

enum class Tone { Good, Warn, Bad, Mute }

data class Status(val label: String, val tone: Tone)

private fun zone(): ZoneId = ZoneId.systemDefault()

fun ld(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone()).toLocalDate()

fun todayMs(): Long = LocalDate.now().atStartOfDay(zone()).toInstant().toEpochMilli()

fun pickerToLocal(ms: Long): Long =
    Instant.ofEpochMilli(ms).atZone(ZoneId.of("UTC")).toLocalDate()
        .atStartOfDay(zone()).toInstant().toEpochMilli()

fun localToPicker(ms: Long): Long =
    ld(ms).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

fun fmt(ms: Long): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).format(ld(ms))

fun fmtWhen(ms: Long): String {
    val dt = Instant.ofEpochMilli(ms).atZone(zone())
    val day = if (dt.toLocalDate() == LocalDate.now()) "Today" else DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH).format(dt)
    val clock = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH).format(dt)
    return "$day · $clock"
}

fun hourLabel(hour: Int): String = when (hour) {
    0 -> "12:00 AM"
    12 -> "12:00 PM"
    in 1..11 -> "$hour:00 AM"
    else -> "${hour - 12}:00 PM"
}

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

fun statusOf(e: Entry): Status {
    if (e.returned) return Status("Returned", Tone.Good)
    val due = e.due ?: return Status("No return date", Tone.Mute)
    val d = daysBetween(todayMs(), due)
    return when {
        d < 0 -> {
            val n = -d
            val unit = if (n == 1L) "day" else "days"
            val label = if (e.direction == BORROWED) "Late by $n $unit" else "Overdue by $n $unit"
            Status(label, Tone.Bad)
        }
        d == 0L -> Status("Due today", Tone.Warn)
        d == 1L -> Status("Due tomorrow", Tone.Warn)
        d <= 3 -> Status("Due in $d days", Tone.Warn)
        else -> Status("Due in $d days", Tone.Mute)
    }
}

fun kindLabel(e: Entry): String = when {
    e.isMoney && e.direction == LENT -> "Money lent"
    e.isMoney && e.direction == BORROWED -> "Money borrowed"
    e.direction == LENT -> "Item lent"
    else -> "Item borrowed"
}

fun listSubtitle(e: Entry): String {
    val kind = when {
        e.isMoney && e.direction == LENT -> "Lent"
        e.isMoney && e.direction == BORROWED -> "Borrowed"
        e.direction == LENT -> "Your item"
        else -> "Their item"
    }
    return "$kind · ${statusOf(e).label}"
}

fun heroCaption(e: Entry): String = when {
    e.returned -> "Returned"
    e.isMoney && e.direction == LENT -> "They owe you"
    e.isMoney && e.direction == BORROWED -> "You owe"
    e.direction == LENT -> "They have"
    else -> "You have"
}

fun alertTitle(kind: String, e: Entry): String = when (kind) {
    "overdue" -> if (e.direction == BORROWED) "Late to return" else "Overdue"
    "due" -> "Due today"
    "soon" -> "Coming up"
    "payment" -> "Payment recorded"
    "returned" -> "Marked returned"
    "added" -> "Added"
    "test" -> "Alerts are on"
    else -> "Owed"
}

fun alertBody(e: Entry): String {
    val what = if (e.isMoney) inr(owing(e)) else e.desc.ifBlank { "Item" }
    val whenDue = e.due?.let { " · ${statusOf(e).label}" }.orEmpty()
    return when {
        e.isMoney && e.direction == LENT -> "${e.name} owes $what$whenDue"
        e.isMoney && e.direction == BORROWED -> "You owe ${e.name} $what$whenDue"
        e.direction == LENT -> "${e.name} has $what$whenDue"
        else -> "Return $what to ${e.name}$whenDue"
    }
}

fun reminderText(e: Entry): String {
    val what = if (e.isMoney) inr(owing(e)) else e.desc.ifBlank { "an item" }
    val due = e.due?.let { " Due ${fmt(it)}." }.orEmpty()
    return if (e.direction == LENT) {
        "Hi ${e.name}, a gentle reminder about $what.$due Thank you!"
    } else {
        "Hi ${e.name}, a note that I still have $what to return.$due Thank you!"
    }
}

fun paymentHistory(e: Entry): List<Payment> {
    if (e.payments.isNotEmpty()) return e.payments.sortedByDescending { it.date }
    if (e.paid > 0) return listOf(Payment("legacy-${e.id}", e.paid, e.date, "Earlier payments"))
    return emptyList()
}

fun applyPayment(e: Entry, id: String, amount: Double, note: String, now: Long): Entry {
    val seed = if (e.payments.isEmpty() && e.paid > 0.0) {
        listOf(Payment("legacy-${e.id}", e.paid, e.date, "Earlier payments"))
    } else {
        e.payments
    }
    val next = e.copy(
        paid = e.paid + amount,
        payments = seed + Payment(id, amount, now, note.trim())
    )
    return if (e.isMoney && owing(next) <= 0.5) next.copy(returned = true, returnedAt = todayMs()) else next
}

fun peopleOf(items: List<Entry>): List<Person> {
    return items.groupBy { it.name.trim().lowercase() }
        .map { (key, list) ->
            val latest = list.maxBy { it.date }
            val phone = list.sortedByDescending { it.date }.firstOrNull { it.phone.isNotBlank() }?.phone.orEmpty()
            val entries = list.sortedWith(compareBy<Entry> { it.returned }.thenBy { it.due ?: Long.MAX_VALUE })
            Person(key, latest.name.trim().ifBlank { "Someone" }, phone, entries)
        }
        .sortedWith(compareByDescending<Person> { it.open.isNotEmpty() }.thenBy { it.name.lowercase() })
}

fun exportText(items: List<Entry>): String {
    val open = items.filter { !it.returned }
    val sb = StringBuilder()
    sb.append("Owed · ${fmt(todayMs())}\n")
    fun section(title: String, rows: List<Entry>) {
        sb.append("\n$title\n")
        if (rows.isEmpty()) {
            sb.append("• Nothing\n")
            return
        }
        rows.forEach { e ->
            val what = if (e.isMoney) inr(owing(e)) else e.desc.ifBlank { "Item" }
            val due = e.due?.let { " · due ${fmt(it)}" }.orEmpty()
            val extra = if (e.isMoney && interest(e) > 0) " · interest ${inr(interest(e))}" else ""
            sb.append("• ${e.name} — $what$due$extra\n")
        }
    }
    section("They owe you", open.filter { it.isMoney && it.direction == LENT })
    section("You owe", open.filter { it.isMoney && it.direction == BORROWED })
    section("Things", open.filter { !it.isMoney })
    val net = open.filter { it.isMoney && it.direction == LENT }.sumOf { owing(it) } -
        open.filter { it.isMoney && it.direction == BORROWED }.sumOf { owing(it) }
    val magnitude = inr(if (net < 0) -net else net)
    val word = when {
        net > 0.5 -> "in your favour"
        net < -0.5 -> "you owe"
        else -> "all square"
    }
    sb.append("\nNet $magnitude $word")
    return sb.toString()
}

fun canNotify(ctx: Context): Boolean {
    if (Build.VERSION.SDK_INT < 33) return true
    return ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
}

fun canExact(ctx: Context): Boolean {
    val am = ctx.getSystemService(AlarmManager::class.java) ?: return true
    return if (Build.VERSION.SDK_INT >= 31) am.canScheduleExactAlarms() else true
}

class Store(ctx: Context) {
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

private const val CHANNEL = "owed"

object Reminders {
    fun schedule(ctx: Context, e: Entry) {
        cancelId(ctx, e.id)
        val due = e.due
        if (e.returned || due == null) return
        if (!Store(ctx).notificationsEnabled()) return
        val hour = Store(ctx).reminderHour()
        val lead = Store(ctx).remindDaysBefore()
        val now = System.currentTimeMillis()
        val dueAt = atHour(ld(due), hour)
        if (lead > 0) {
            val soonAt = atHour(ld(due).minusDays(lead.toLong()), hour)
            if (soonAt > now + 20_000) setAlarm(ctx, e.id, "soon", soonAt)
        }
        if (dueAt > now + 20_000) setAlarm(ctx, e.id, "due", dueAt)
        else setAlarm(ctx, e.id, "overdue", nextHour(hour))
    }

    fun cancel(ctx: Context, e: Entry) = cancelId(ctx, e.id)

    fun cancelId(ctx: Context, id: String) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        listOf("soon", "due", "overdue").forEach { kind ->
            val pi = pending(ctx, id, kind, PendingIntent.FLAG_NO_CREATE) ?: return@forEach
            am.cancel(pi)
            pi.cancel()
        }
    }

    fun notify(ctx: Context, title: String, body: String, notifyId: Int, openId: String?) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Return dates and overdue alerts"
            enableVibration(true)
        }
        nm.createNotificationChannel(channel)
        val open = Intent(ctx, MainActivity::class.java).apply {
            if (!openId.isNullOrBlank()) putExtra("open", openId)
        }
        val content = PendingIntent.getActivity(
            ctx,
            openId?.hashCode() ?: 42,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val publicVersion = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Owed reminder")
            .setContentText("You have a return reminder")
            .build()
        val notification = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(content)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()
        try {
            nm.notify(notifyId, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun setAlarm(ctx: Context, id: String, kind: String, at: Long) {
        if (at <= System.currentTimeMillis() + 20_000) return
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = pending(ctx, id, kind, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        try {
            val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (_: SecurityException) {
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } catch (_: SecurityException) {
            }
        }
    }

    private fun pending(ctx: Context, id: String, kind: String, flags: Int): PendingIntent? {
        val intent = Intent(ctx, ReminderReceiver::class.java)
            .putExtra("id", id)
            .putExtra("kind", kind)
        return PendingIntent.getBroadcast(
            ctx,
            (id + ":" + kind).hashCode(),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun atHour(date: LocalDate, hour: Int): Long =
        date.atTime(hour.coerceIn(0, 23), 0).atZone(zone()).toInstant().toEpochMilli()

    private fun nextHour(hour: Int): Long {
        var whenAt = LocalDate.now().atTime(hour.coerceIn(0, 23), 0).atZone(zone())
        if (whenAt.toInstant().toEpochMilli() <= System.currentTimeMillis() + 20_000) whenAt = whenAt.plusDays(1)
        return whenAt.toInstant().toEpochMilli()
    }
}
