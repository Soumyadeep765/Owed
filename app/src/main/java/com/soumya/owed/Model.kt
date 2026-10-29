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

const val LENT = 0

const val BORROWED = 1

const val APP_VERSION = "1.3"

const val DEV_NAME = "Soumyadeep Das"

const val DEV_EMAIL = "soumyadeepdas765@gmail.com"

const val DEV_GITHUB = "https://github.com/Soumyadeep765/Owed"

const val DEV_TELEGRAM = "https://t.me/devxy"

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
