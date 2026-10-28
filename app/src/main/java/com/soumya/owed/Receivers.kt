package com.soumya.owed

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("due", "Return reminders", NotificationManager.IMPORTANCE_HIGH)
        )
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(c, "due")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Return due today")
            .setContentText(i.getStringExtra("text") ?: "")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify((i.getStringExtra("id") ?: "").hashCode(), n)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED) {
            Store(c).load().forEach { Reminders.schedule(c, it) }
        }
    }
}
