@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.soumya.owed

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import java.util.Locale
import java.util.UUID

@Composable
fun SettingsScreen(vm: OwedVm) {
    val ctx = LocalContext.current
    val ios = LocalIos.current
    val tick = rememberResumeTick()
    val phoneAlerts = remember(tick) { canNotify(ctx) }
    val exact = remember(tick) { canExact(ctx) }
    val footer = when {
        !vm.notificationsOn -> t("Reminders are paused.")
        !phoneAlerts -> t("Phone alerts are blocked, so reminders stay inside Owed until you allow them.")
        else -> t("Overdue entries remind you once a day until you mark them returned.")
    }
    LargeTitlePage(t("Settings")) {
        item { SectionLabel(t("Language")) }
        item {
            val current = Languages.find { it.code == vm.language }
            Group {
                PickRow(
                    t("Language"),
                    current?.native ?: "English",
                    Languages.map { it.code to it.native },
                    vm.language
                ) { vm.changeLanguage(it) }
            }
        }
        item { SectionLabel(t("Currency")) }
        item {
            val current = Currencies.find { it.code == vm.currency }
            Group {
                PickRow(
                    t("Currency"),
                    "${currencySymbol(vm.currency)}  ${current?.let { t(it.name) } ?: vm.currency}",
                    Currencies.map { it.code to "${currencySymbol(it.code)}  ${t(it.name)}" },
                    vm.currency
                ) { vm.changeCurrency(it) }
            }
        }
        item { SectionLabel(t("Notifications")) }
        item {
            Group {
                SwitchRow(
                    t("Alerts"),
                    t("Reminders for return dates"),
                    vm.notificationsOn
                ) { vm.setNotifications(it) }
                Hairline()
                NavRow(
                    t("Phone alerts"),
                    value = if (phoneAlerts) t("Allowed") else t("Blocked"),
                    leading = { TintIcon(Icons.Filled.Notifications, ios.blue) },
                    chevron = Build.VERSION.SDK_INT >= 33,
                    onClick = if (Build.VERSION.SDK_INT >= 33) {
                        { openAppSettings(ctx) }
                    } else {
                        null
                    }
                )
                if (Build.VERSION.SDK_INT >= 31) {
                    Hairline()
                    NavRow(
                        t("Precise timing"),
                        value = if (exact) t("On") else t("Off"),
                        leading = { TintIcon(Icons.Filled.DateRange, ios.orange) },
                        chevron = true,
                        onClick = { openExactSettings(ctx) }
                    )
                }
                Hairline()
                NavRow(
                    t("Send a test alert"),
                    leading = { TintIcon(Icons.Filled.Notifications, ios.green) },
                    chevron = true,
                    onClick = { vm.sendTest(ctx) }
                )
            }
        }
        item { SectionFooter(footer) }
        item { SectionLabel(t("Remind me")) }
        item {
            val dayLabel = when (vm.remindDays) {
                0 -> t("On the day")
                3 -> t("3 days before")
                else -> t("1 day before")
            }
            Group {
                PickRow(
                    t("Day"),
                    dayLabel,
                    listOf(
                        "0" to t("On the day"),
                        "1" to t("1 day before"),
                        "3" to t("3 days before")
                    ),
                    vm.remindDays.toString()
                ) { vm.setLead(it.toInt()) }
                Hairline()
                PickRow(
                    t("Time"),
                    hourLabel(vm.reminderHour),
                    listOf(8, 9, 12, 18, 21).map { hour -> hour.toString() to hourLabel(hour) },
                    vm.reminderHour.toString()
                ) { vm.setHour(it.toInt()) }
            }
        }
        item { SectionLabel(t("Appearance")) }
        item {
            Group {
                listOf("system" to t("System"), "light" to t("Light"), "dark" to t("Dark")).forEachIndexed { index, (id, label) ->
                    if (index > 0) Hairline()
                    CheckRow(label, vm.appearance == id) { vm.changeAppearance(id) }
                }
            }
        }
        item { SectionLabel(t("Data")) }
        item {
            val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                val wrote = try {
                    ctx.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(vm.backup().toByteArray(Charsets.UTF_8))
                    }
                    true
                } catch (_: Exception) {
                    false
                }
                if (wrote) {
                    vm.flash(t("Backup saved"), t("Keep this file. You can restore it on this phone or another."), "info", null)
                } else {
                    vm.flash(t("Couldn’t save"), t("The backup file was not written."), "soon", null)
                }
            }
            val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                val raw = try {
                    ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                } catch (_: Exception) {
                    null
                }
                if (raw != null && vm.restore(raw)) {
                    vm.flash(t("Restored"), t("Your entries are back."), "added", null)
                } else {
                    vm.flash(t("Couldn’t restore"), t("That file is not an Owed backup."), "soon", null)
                }
            }
            Group {
                NavRow(
                    t("Save a backup"),
                    leading = { TintIcon(Icons.Filled.Share, ios.blue) },
                    chevron = true,
                    onClick = { saveBackup.launch("owed-backup.json") }
                )
                Hairline()
                NavRow(
                    t("Restore a backup"),
                    leading = { TintIcon(Icons.Filled.Info, ios.green) },
                    chevron = true,
                    onClick = { openBackup.launch(arrayOf("application/json", "text/plain", "*/*")) }
                )
                Hairline()
                NavRow(
                    t("Share summary"),
                    leading = { TintIcon(Icons.Filled.Share, ios.blue) },
                    chevron = true,
                    onClick = {
                        try {
                            shareText(ctx, exportText(vm.items))
                        } catch (_: ActivityNotFoundException) {
                            vm.flash(t("Couldn’t share"), t("No app accepted the summary."), "info", null)
                        }
                    }
                )
                Hairline()
                NavRow(t("Saved entries"), value = vm.items.size.toString())
            }
        }
        item { SectionFooter(t("Owed keeps this list on your phone. Nothing is uploaded.")) }
        item { SectionLabel(t("About")) }
        item {
            Group {
                NavRow(
                    t("Version"),
                    value = APP_VERSION,
                    leading = { TintIcon(Icons.Filled.Info, Color(0xFF8E8E93)) }
                )
                Hairline()
                NavRow(t("Developer"), value = DEV_NAME)
                Hairline()
                NavRow(
                    t("Email"),
                    value = DEV_EMAIL,
                    chevron = true,
                    onClick = {
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$DEV_EMAIL")))
                        } catch (_: ActivityNotFoundException) {
                            vm.flash(t("No mail app"), t("Write to %s from another app.", DEV_EMAIL), "info", null)
                        }
                    }
                )
                Hairline()
                NavRow(
                    t("GitHub"),
                    value = "Soumyadeep765/Owed",
                    chevron = true,
                    onClick = {
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DEV_GITHUB)))
                        } catch (_: ActivityNotFoundException) {
                            vm.flash(t("Can’t open link"), DEV_GITHUB, "info", null)
                        }
                    }
                )
                Hairline()
                NavRow(
                    "Telegram",
                    value = "@devxy",
                    chevron = true,
                    onClick = {
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DEV_TELEGRAM)))
                        } catch (_: ActivityNotFoundException) {
                            vm.flash(t("Can’t open link"), DEV_TELEGRAM, "info", null)
                        }
                    }
                )
            }
        }
        item { SectionFooter(t("Track money and things, in both directions.")) }
    }
}

private fun openAppSettings(ctx: Context) {
    ctx.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
    )
}

private fun openExactSettings(ctx: Context) {
    if (Build.VERSION.SDK_INT < 31) return
    try {
        ctx.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
        )
    } catch (_: Exception) {
        openAppSettings(ctx)
    }
}
