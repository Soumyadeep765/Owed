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
        LocaleHub.lang = Store(ctx).language()
        LocaleHub.currency = Store(ctx).currency()
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
        val builder = Notification.Builder(ctx, CHANNEL)
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
        if (!openId.isNullOrBlank()) {
            val settle = PendingIntent.getBroadcast(
                ctx,
                openId.hashCode() xor 0x51E,
                Intent(ctx, SettleReceiver::class.java).putExtra("id", openId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                Notification.Action.Builder(
                    android.R.drawable.checkbox_on_background,
                    t("Settled"),
                    settle
                ).build()
            )
        }
        val notification = builder.build()
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
