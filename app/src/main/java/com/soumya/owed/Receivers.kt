package com.soumya.owed

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getStringExtra("id") ?: return
        val kind = i.getStringExtra("kind") ?: "due"
        val store = Store(c)
        LocaleHub.lang = store.language()
        LocaleHub.currency = store.currency()
        val entry = store.load().find { it.id == id }
        if (entry == null || entry.returned || entry.due == null || !store.notificationsEnabled()) {
            Reminders.cancelId(c, id)
            return
        }
        val title = alertTitle(kind, entry)
        val body = alertBody(entry)
        Reminders.notify(c, title, body, id.hashCode(), id)
        store.appendAlert(
            AppAlert(
                id = "$id:$kind:${todayMs()}",
                entryId = id,
                kind = kind,
                title = title,
                body = body,
                time = System.currentTimeMillis(),
                read = false
            )
        )
        val due = entry.due
        if (due != null && store.textsEnabled() && PhoneTexts.allowed(c) && entry.phone.isNotBlank()) {
            if (!store.alreadyTexted(entry.id, kind, due) && PhoneTexts.send(c, entry.phone, reminderText(entry), store.textSim())) {
                store.markTexted(entry.id, kind, due)
            }
        }
        Reminders.schedule(c, entry)
    }
}

object PhoneTexts {
    fun allowed(ctx: Context): Boolean {
        return ctx.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    fun send(ctx: Context, raw: String, body: String, subId: Int): Boolean {
        val number = raw.filter { it.isDigit() || it == '+' }
        if (number.count { it.isDigit() } < 6 || body.isBlank()) return false
        return try {
            val mgr = manager(ctx, subId)
            val parts = mgr.divideMessage(body)
            if (parts.size <= 1) mgr.sendTextMessage(number, null, body, null, null)
            else mgr.sendMultipartTextMessage(number, null, parts, null, null)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun manager(ctx: Context, subId: Int): SmsManager {
        val base = if (Build.VERSION.SDK_INT >= 31) {
            ctx.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        if (subId < 0) return base
        return if (Build.VERSION.SDK_INT >= 31) base.createForSubscriptionId(subId)
        else {
            @Suppress("DEPRECATION")
            SmsManager.getSmsManagerForSubscriptionId(subId)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED || i.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Store(c).load().forEach { Reminders.schedule(c, it) }
            BalanceWidget.update(c)
        }
    }
}

class SettleReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getStringExtra("id") ?: return
        val store = Store(c)
        val entry = store.load().find { it.id == id } ?: return
        val nm = c.getSystemService(android.app.NotificationManager::class.java)
        nm?.cancel(id.hashCode())
        if (entry.returned) {
            Reminders.cancel(c, entry)
            return
        }
        val next = entry.copy(returned = true, returnedAt = todayMs())
        store.save(store.load().map { if (it.id == id) next else it })
        Reminders.cancel(c, entry)
        LocaleHub.lang = store.language()
        store.appendAlert(
            AppAlert(
                id = "$id:settled:${System.currentTimeMillis()}",
                entryId = id,
                kind = "returned",
                title = t("Settled"),
                body = "${entry.name} is clear.",
                time = System.currentTimeMillis(),
                read = false
            )
        )
    }
}
