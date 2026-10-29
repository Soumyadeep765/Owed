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

internal fun zone(): ZoneId = ZoneId.systemDefault()

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

fun inr(v: Double): String {
    val code = LocaleHub.currency
    val currency = try {
        Currency.getInstance(code)
    } catch (_: Exception) {
        Currency.getInstance("INR")
    }
    val digits = when (currency.currencyCode) {
        "JPY", "KRW", "VND" -> 0
        "INR" -> 0
        else -> currency.defaultFractionDigits.coerceIn(0, 2)
    }
    val fmt = NumberFormat.getCurrencyInstance(moneyLocale(currency.currencyCode))
    fmt.currency = currency
    fmt.minimumFractionDigits = digits
    fmt.maximumFractionDigits = digits
    return fmt.format(v)
}

private fun moneyLocale(code: String): Locale = when (code) {
    "INR" -> Locale("en", "IN")
    "USD" -> Locale.US
    "EUR" -> Locale.GERMANY
    "GBP" -> Locale.UK
    "JPY" -> Locale.JAPAN
    "AUD" -> Locale("en", "AU")
    "CAD" -> Locale.CANADA
    "CNY" -> Locale.CHINA
    "AED" -> Locale("en", "AE")
    "SAR" -> Locale("en", "SA")
    "SGD" -> Locale("en", "SG")
    "BDT" -> Locale("en", "BD")
    "NPR" -> Locale("en", "NP")
    "KRW" -> Locale.KOREA
    "BRL" -> Locale("pt", "BR")
    "CHF" -> Locale("de", "CH")
    else -> Locale.US
}

fun daysBetween(a: Long, b: Long): Long = ChronoUnit.DAYS.between(ld(a), ld(b))

fun interest(e: Entry): Double {
    if (!e.isMoney || e.rate <= 0) return 0.0
    val end = e.returnedAt ?: todayMs()
    val d = maxOf(0L, daysBetween(e.date, end))
    return e.amount * e.rate / 100 * d / 30
}

fun owing(e: Entry): Double = maxOf(0.0, e.amount + interest(e) - e.paid)

fun statusOf(e: Entry): Status {
    if (e.returned) return Status(t("Returned"), Tone.Good)
    val due = e.due ?: return Status(t("No return date"), Tone.Mute)
    val d = daysBetween(todayMs(), due)
    return when {
        d < 0 -> {
            val n = -d
            val label = if (e.direction == BORROWED) {
                if (n == 1L) t("Late by 1 day") else t("Late by %d days", n.toInt())
            } else {
                if (n == 1L) t("Overdue by 1 day") else t("Overdue by %d days", n.toInt())
            }
            Status(label, Tone.Bad)
        }
        d == 0L -> Status(t("Due today"), Tone.Warn)
        d == 1L -> Status(t("Due tomorrow"), Tone.Warn)
        d <= 3 -> Status(t("Due in %d days", d.toInt()), Tone.Warn)
        else -> Status(t("Due in %d days", d.toInt()), Tone.Mute)
    }
}

fun kindLabel(e: Entry): String = when {
    e.isMoney && e.direction == LENT -> t("Money lent")
    e.isMoney && e.direction == BORROWED -> t("Money borrowed")
    e.direction == LENT -> t("Item lent")
    else -> t("Item borrowed")
}

fun listSubtitle(e: Entry): String {
    val kind = when {
        e.isMoney && e.direction == LENT -> t("Lent")
        e.isMoney && e.direction == BORROWED -> t("Borrowed")
        e.direction == LENT -> t("Your item")
        else -> t("Their item")
    }
    return "$kind · ${statusOf(e).label}"
}

fun heroCaption(e: Entry): String = when {
    e.returned -> t("Returned")
    e.isMoney && e.direction == LENT -> t("They owe you")
    e.isMoney && e.direction == BORROWED -> t("You owe")
    e.direction == LENT -> t("They have")
    else -> t("You have")
}

fun alertTitle(kind: String, e: Entry): String = when (kind) {
    "overdue" -> if (e.direction == BORROWED) t("Late to return") else t("Overdue")
    "due" -> t("Due today")
    "soon" -> t("Coming up")
    "payment" -> t("Payment recorded")
    "returned" -> t("Marked returned")
    "added" -> t("Added")
    "test" -> t("Alerts are on")
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
    val what = if (e.isMoney) inr(owing(e)) else e.desc.ifBlank { t("an item") }
    val due = e.due?.let { " ${t("Due %s.", fmt(it))}" }.orEmpty()
    return if (e.direction == LENT) {
        t("Hi %1\$s, a gentle reminder about %2\$s.%3\$s Thank you!", e.name, what, due)
    } else {
        t("Hi %1\$s, a note that I still have %2\$s to return.%3\$s Thank you!", e.name, what, due)
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
