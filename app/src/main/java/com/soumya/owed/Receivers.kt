package com.soumya.owed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getStringExtra("id") ?: return
        val kind = i.getStringExtra("kind") ?: "due"
        val store = Store(c)
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
        Reminders.schedule(c, entry)
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
