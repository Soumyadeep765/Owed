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
fun HomeScreen(
    vm: OwedVm,
    onAdd: () -> Unit,
    onOpen: (Entry) -> Unit,
    onEntries: () -> Unit,
    onOverdue: () -> Unit,
    onAlerts: () -> Unit
) {
    val ios = LocalIos.current
    val open = vm.items.filter { !it.returned }
    val overdue = open.filter { (it.due ?: Long.MAX_VALUE) < todayMs() }.sortedBy { it.due ?: Long.MAX_VALUE }
    val soon = open.filter { entry ->
        val due = entry.due ?: return@filter false
        daysBetween(todayMs(), due) in 0..7
    }.sortedBy { it.due ?: Long.MAX_VALUE }
    val rest = open.filter { entry ->
        overdue.none { it.id == entry.id } && soon.none { it.id == entry.id }
    }.sortedByDescending { it.date }
    val unread = vm.alerts.count { !it.read }
    val toYou = open.filter { it.isMoney && it.direction == LENT }.sumOf { owing(it) }
    val youOwe = open.filter { it.isMoney && it.direction == BORROWED }.sumOf { owing(it) }
    val net = toYou - youOwe
    val interestToYou = open.filter { it.isMoney && it.direction == LENT }.sumOf { interest(it) }
    val interestYouOwe = open.filter { it.isMoney && it.direction == BORROWED }.sumOf { interest(it) }

    LargeTitlePage("Owed", bottom = 108.dp) {
        if (vm.items.isEmpty()) {
            item {
                EmptyBlock(
                    t("Nothing owed right now"),
                    t("Keep money and things you’ve lent or borrowed in one place."),
                    t("Add an entry"),
                    onAdd
                )
            }
        } else {
        item {
            Group {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)) {
                    Text(t("Net"), color = ios.secondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(
                        inr(if (net < 0) -net else net),
                        color = ios.label,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.6).sp
                    )
                    Text(
                        when {
                            net > 0.5 -> t("in your favour")
                            net < -0.5 -> t("you owe more")
                            else -> t("you’re square")
                        },
                        color = when {
                            net > 0.5 -> ios.green
                            net < -0.5 -> ios.orange
                            else -> ios.secondary
                        },
                        fontSize = 15.sp
                    )
                }
                Hairline(0.dp)
                MoneyLine(t("Owed to you"), inr(toYou), ios.green)
                Hairline()
                MoneyLine(t("You owe"), inr(youOwe), ios.orange)
                if (interestToYou > 0.5) {
                    Hairline()
                    MoneyLine(t("Interest to you"), inr(interestToYou), ios.secondary)
                }
                if (interestYouOwe > 0.5) {
                    Hairline()
                    MoneyLine(t("Interest you owe"), inr(interestYouOwe), ios.secondary)
                }
                Hairline()
                MoneyLine(t("Open"), open.size.toString(), ios.label)
            }
        }
        if (unread > 0) {
            item { Spacer(Modifier.height(12.dp)) }
            item {
                Group {
                    NavRow(
                        "Alerts",
                        value = unread.toString(),
                        subtitle = vm.alerts.firstOrNull { !it.read }?.title,
                        leading = { TintIcon(Icons.Filled.Notifications, ios.orange) },
                        chevron = true,
                        onClick = onAlerts
                    )
                }
            }
        }
        if (overdue.isNotEmpty()) {
            item { SectionLabel("Overdue", "See all", onOverdue) }
            item { EntryGroup(overdue.take(4), onOpen) }
        }
        if (soon.isNotEmpty()) {
            item { SectionLabel("Due soon", "See all", onEntries) }
            item { EntryGroup(soon.take(4), onOpen) }
        }
        if (rest.isNotEmpty()) {
            item { SectionLabel(if (soon.isEmpty() && overdue.isEmpty()) "Open" else "Also open", "See all", onEntries) }
            item { EntryGroup(rest.take(4), onOpen) }
        }
        if (open.isEmpty()) {
            item { SectionFooter("Everything has been marked returned.") }
        }
        }
    }
}

@Composable
fun EntriesScreen(
    vm: OwedVm,
    scope: String,
    chip: String,
    onScope: (String) -> Unit,
    onChip: (String) -> Unit,
    onAdd: () -> Unit,
    onOpen: (Entry) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val rows = visibleEntries(vm.items, scope, chip, query)
    val chips = buildList {
        add("all" to t("All"))
        add("money" to t("Money"))
        add("item" to t("Items"))
        if (scope == "open") add("overdue" to t("Overdue"))
    }
    LargeTitlePage(t("Entries"), bottom = 108.dp) {
        item { IosSearch(query, { query = it }, t("Search name or note")) }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            IosSegmented(
                listOf(t("Open"), t("Settled")),
                selected = if (scope == "open") 0 else 1,
                onSelect = { index ->
                    val next = if (index == 0) "open" else "done"
                    onScope(next)
                    if (next == "done" && chip == "overdue") onChip("all")
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        item { Spacer(Modifier.height(10.dp)) }
        item { IosChips(chips, chip, onChip) }
        item { Spacer(Modifier.height(12.dp)) }
        if (rows.isEmpty()) {
            item {
                if (vm.items.isEmpty()) {
                    EmptyBlock(
                        "No entries yet",
                        "Add money or an item, with a return date if you want a reminder.",
                        "Add an entry",
                        onAdd
                    )
                } else {
                    Text(
                        "Nothing matches.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        color = LocalIos.current.secondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item { EntryGroup(rows, onOpen) }
        }
    }
}

@Composable
fun PeopleScreen(vm: OwedVm, onOpen: (Person) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val people = peopleOf(vm.items).filter { person ->
        query.isBlank() || person.name.contains(query, true) || person.phone.contains(query)
    }
    LargeTitlePage(t("People"), bottom = 108.dp) {
        item { IosSearch(query, { query = it }, t("Search people")) }
        item { Spacer(Modifier.height(12.dp)) }
        if (people.isEmpty()) {
            item {
                if (vm.items.isEmpty()) {
                    EmptyBlock(
                        t("No one yet"),
                        t("People are grouped from the names on your entries."),
                        symbol = null
                    )
                } else {
                    Text(
                        "No matches.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        color = LocalIos.current.secondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item {
                Group {
                    people.forEachIndexed { index, person ->
                        PersonRow(person) { onOpen(person) }
                        if (index != people.lastIndex) Hairline(66.dp)
                    }
                }
            }
            item { SectionFooter("Balances count open money only. Items are listed on the person.") }
        }
    }
}

@Composable
fun AlertsScreen(vm: OwedVm, onOpen: (AppAlert) -> Unit) {
    val ios = LocalIos.current
    val unread = vm.alerts.count { !it.read }
    LaunchedEffect(Unit) { vm.reloadAlerts() }
    LargeTitlePage(
        t("Alerts"),
        trailing = if (unread > 0) {
            {
                Text(
                    t("Read all"),
                    color = ios.blue,
                    fontSize = 17.sp,
                    modifier = Modifier.clickable { vm.markAllRead() }.padding(vertical = 6.dp)
                )
            }
        } else {
            null
        }
    ) {
        if (vm.alerts.isEmpty()) {
            item {
                EmptyBlock(
                    t("No alerts"),
                    "Return reminders, payments, and settled entries show up here.",
                    symbol = "!"
                )
            }
        } else {
            item {
                Group {
                    vm.alerts.forEachIndexed { index, alert ->
                        AlertRow(alert) { onOpen(alert) }
                        if (index != vm.alerts.lastIndex) Hairline(34.dp)
                    }
                }
            }
            item {
                Text(
                    "Clear alerts",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .clickable { vm.clearAlerts() }
                        .padding(vertical = 8.dp),
                    color = ios.red,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: OwedVm) {
    val ctx = LocalContext.current
    val ios = LocalIos.current
    val tick = rememberResumeTick()
    val phoneAlerts = remember(tick) { canNotify(ctx) }
    val exact = remember(tick) { canExact(ctx) }
    val footer = when {
        !vm.notificationsOn -> "Reminders are paused."
        !phoneAlerts -> "Phone alerts are blocked, so reminders stay inside Owed until you allow them."
        else -> "Overdue entries remind you once a day until you mark them returned."
    }
    LargeTitlePage(t("Settings")) {
        item { SectionLabel(t("Language")) }
        item {
            Group {
                Languages.forEachIndexed { index, option ->
                    if (index > 0) Hairline()
                    CheckRow(option.native, vm.language == option.code) {
                        vm.changeLanguage(option.code)
                    }
                }
            }
        }
        item { SectionLabel(t("Currency")) }
        item {
            Group {
                Currencies.forEachIndexed { index, option ->
                    if (index > 0) Hairline()
                    CheckRow("${currencySymbol(option.code)}  ${t(option.name)}", vm.currency == option.code) {
                        vm.changeCurrency(option.code)
                    }
                }
            }
        }
        item { SectionLabel(t("Notifications")) }
        item {
            Group {
                SwitchRow(
                    "Alerts",
                    "Reminders for return dates",
                    vm.notificationsOn
                ) { vm.setNotifications(it) }
                Hairline()
                NavRow(
                    "Phone alerts",
                    value = if (phoneAlerts) "Allowed" else "Blocked",
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
                        "Precise timing",
                        value = if (exact) "On" else "Off",
                        leading = { TintIcon(Icons.Filled.DateRange, ios.orange) },
                        chevron = true,
                        onClick = { openExactSettings(ctx) }
                    )
                }
                Hairline()
                NavRow(
                    "Send a test alert",
                    leading = { TintIcon(Icons.Filled.Notifications, ios.green) },
                    chevron = true,
                    onClick = { vm.sendTest(ctx) }
                )
            }
        }
        item { SectionFooter(footer) }
        item { SectionLabel("Remind me") }
        item {
            Group {
                listOf(0 to "On the day", 1 to "1 day before", 3 to "3 days before").forEachIndexed { index, (days, label) ->
                    if (index > 0) Hairline()
                    CheckRow(label, vm.remindDays == days) { vm.setLead(days) }
                }
            }
        }
        item { SectionLabel("Reminder time") }
        item {
            Group {
                listOf(8, 9, 12, 18, 21).forEachIndexed { index, hour ->
                    if (index > 0) Hairline()
                    CheckRow(hourLabel(hour), vm.reminderHour == hour) { vm.setHour(hour) }
                }
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
        item { SectionLabel(t("Privacy")) }
        item {
            Group {
                SwitchRow(
                    t("Phone lock"),
                    t("Face, fingerprint, or PIN before balances show"),
                    vm.lockOn
                ) { on ->
                    if (on && !phoneLockReady(ctx)) {
                        vm.flash(
                            t("Phone lock"),
                            t("Set a screen lock in phone settings first."),
                            "soon",
                            null
                        )
                    } else {
                        vm.setLock(on)
                    }
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
                            vm.flash("Couldn’t share", "No app accepted the summary.", "info", null)
                        }
                    }
                )
                Hairline()
                NavRow(t("Saved entries"), value = vm.items.size.toString())
            }
        }
        item { SectionFooter("Owed keeps this list on your phone. Nothing is uploaded.") }
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
                            vm.flash("No mail app", "Write to $DEV_EMAIL from another app.", "info", null)
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
                            vm.flash("Can’t open link", DEV_GITHUB, "info", null)
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
                            vm.flash("Can’t open link", DEV_TELEGRAM, "info", null)
                        }
                    }
                )
            }
        }
        item { SectionFooter("Track money and things, in both directions.") }
    }
}

@Composable
fun EntryScreen(
    vm: OwedVm,
    id: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPay: () -> Unit,
    onDelete: () -> Unit
) {
    val entry = vm.items.find { it.id == id }
    LaunchedEffect(entry) {
        if (entry == null) onBack()
    }
    if (entry == null) return
    val e = entry
    val ios = LocalIos.current
    val ctx = LocalContext.current
    val st = statusOf(e)
    val history = paymentHistory(e)
    val big = if (e.isMoney) inr(if (e.returned) e.amount else owing(e)) else e.desc.ifBlank { "Item" }
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        NavBar(e.name, onBack)
        Column(
            Modifier
                .stretchOverscroll()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Avatar(e.name, 72.dp)
                Spacer(Modifier.height(10.dp))
                Text(e.name, color = ios.label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(heroCaption(e), color = ios.secondary, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    big,
                    color = ios.label,
                    fontSize = if (e.isMoney) 34.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                if (e.isMoney && !e.returned && interest(e) > 0.5) {
                    Text("includes ${inr(interest(e))} interest", color = ios.secondary, fontSize = 13.sp)
                }
                Text(st.label, color = toneColor(st.tone), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            SectionLabel("Actions")
            Group {
                val actions = buildList {
                    if (!e.returned && e.isMoney) add(Triple("Record payment", null as String?, onPay))
                    if (!e.returned) {
                        add(Triple("Mark returned", null, { vm.markReturned(e.id) }))
                        add(Triple("Share reminder", null, {
                            try {
                                shareText(ctx, reminderText(e))
                            } catch (_: ActivityNotFoundException) {
                                vm.flash("Couldn’t share", "No app accepted the reminder.", "info", e.id)
                            }
                        }))
                        if (e.phone.isNotBlank()) {
                            add(Triple("Text ${e.name}", null, {
                                try {
                                    sms(ctx, e.phone, reminderText(e))
                                } catch (_: ActivityNotFoundException) {
                                    vm.flash("No messages app", "Share the reminder instead.", "info", e.id)
                                }
                            }))
                        }
                    } else {
                        add(Triple("Reopen", null, { vm.reopen(e.id) }))
                    }
                    if (e.phone.isNotBlank()) add(Triple("Call", e.phone, {
                        try {
                            dial(ctx, e.phone)
                        } catch (_: ActivityNotFoundException) {
                            vm.flash("Can’t call", "No phone app is available.", "info", e.id)
                        }
                    }))
                    add(Triple("Edit", null, onEdit))
                }
                actions.forEachIndexed { index, (title, value, click) ->
                    NavRow(title, value = value, chevron = true, onClick = click)
                    if (index != actions.lastIndex) Hairline()
                }
            }
            SectionLabel("Details")
            Group {
                val details = buildList {
                    add("Type" to kindLabel(e))
                    add("Given" to fmt(e.date))
                    add("Return" to (e.due?.let { fmt(it) } ?: "Not set"))
                    if (e.isMoney && e.rate > 0) add("Rate" to "${trimNum(e.rate)}% / month")
                    if (e.isMoney && interest(e) > 0.5) add("Interest" to inr(interest(e)))
                    if (e.isMoney && e.paid > 0) add("Received" to inr(e.paid))
                    val returnedOn = e.returnedAt
                    if (e.returned && returnedOn != null) add("Returned" to fmt(returnedOn))
                    if (e.phone.isNotBlank()) add("Phone" to e.phone)
                }
                details.forEachIndexed { index, (label, value) ->
                    NavRow(label, value = value)
                    if (index != details.lastIndex) Hairline()
                }
            }
            if (e.isMoney && e.rate > 0) {
                SectionFooter("Interest is simple: the monthly rate spread over 30-day months, until it’s returned.")
            }
            if (e.note.isNotBlank()) {
                SectionLabel("Note")
                Group {
                    Text(
                        e.note,
                        modifier = Modifier.padding(16.dp),
                        color = ios.label,
                        fontSize = 17.sp
                    )
                }
            }
            if (e.isMoney && history.isNotEmpty()) {
                SectionLabel("Payments")
                Group {
                    history.forEachIndexed { index, payment ->
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    payment.note.ifBlank { "Payment" },
                                    modifier = Modifier.weight(1f),
                                    color = ios.label,
                                    fontSize = 17.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(inr(payment.amount), color = ios.label, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            }
                            Text(fmt(payment.date), color = ios.secondary, fontSize = 13.sp)
                        }
                        if (index != history.lastIndex) Hairline()
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Group {
                Box(
                    Modifier.fillMaxWidth().clickable(onClick = onDelete).padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Delete entry", color = ios.red, fontSize = 17.sp)
                }
            }
        }
    }
}

@Composable
fun PersonScreen(
    vm: OwedVm,
    key: String,
    onBack: () -> Unit,
    onOpen: (Entry) -> Unit
) {
    val person = peopleOf(vm.items).find { it.key == key }
    LaunchedEffect(person) {
        if (person == null) onBack()
    }
    if (person == null) return
    val p = person
    val ios = LocalIos.current
    val ctx = LocalContext.current
    val netColor = when {
        p.net > 0.5 -> ios.green
        p.net < -0.5 -> ios.orange
        else -> ios.secondary
    }
    val netText = when {
        p.net > 0.5 -> "${inr(p.net)} owed to you"
        p.net < -0.5 -> "${inr(-p.net)} you owe"
        p.open.isEmpty() -> "All settled"
        else -> "No money open"
    }
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        NavBar(p.name, onBack)
        Column(
            Modifier
                .stretchOverscroll()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 88.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Avatar(p.name, 72.dp)
                Spacer(Modifier.height(10.dp))
                Text(p.name, color = ios.label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(netText, color = netColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    if (p.open.isEmpty()) "Nothing open" else "${p.open.size} open",
                    color = ios.secondary,
                    fontSize = 13.sp
                )
            }
            if (p.phone.isNotBlank()) {
                SectionLabel("Contact")
                Group {
                    NavRow("Call", value = p.phone, chevron = true, onClick = {
                        try {
                            dial(ctx, p.phone)
                        } catch (_: ActivityNotFoundException) {
                            vm.flash("Can’t call", "No phone app is available.", "info", null)
                        }
                    })
                    Hairline()
                    NavRow("Text", chevron = true, onClick = {
                        try {
                            sms(ctx, p.phone, personMessage(p))
                        } catch (_: ActivityNotFoundException) {
                            vm.flash("No messages app", "Nothing on this phone can send a text.", "info", null)
                        }
                    })
                }
            }
            if (p.open.isNotEmpty()) {
                SectionLabel("Open")
                EntryGroup(p.open, onOpen)
            }
            val settled = p.entries.filter { it.returned }
            if (settled.isNotEmpty()) {
                SectionLabel("Settled")
                EntryGroup(settled, onOpen)
            }
        }
    }
}

@Composable
fun EditorSheet(
    existing: Entry?,
    presetName: String,
    presetLent: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (Entry) -> Unit
) {
    val ios = LocalIos.current
    val ctx = LocalContext.current
    var lent by remember { mutableStateOf(if (existing != null) existing.direction == LENT else presetLent) }
    var money by remember { mutableStateOf(existing?.isMoney ?: true) }
    var name by remember { mutableStateOf(existing?.name ?: presetName) }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var amount by remember {
        mutableStateOf(
            if (existing != null && existing.isMoney && existing.amount > 0) trimNum(existing.amount) else ""
        )
    }
    var desc by remember { mutableStateOf(existing?.desc ?: "") }
    var rate by remember {
        mutableStateOf(existing?.rate?.takeIf { it > 0 }?.let { trimNum(it) }.orEmpty())
    }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var date by remember { mutableLongStateOf(existing?.date ?: todayMs()) }
    var due by remember { mutableStateOf(existing?.due) }
    var picker by remember { mutableStateOf(0) }
    var err by remember { mutableStateOf("") }
    var closing by remember { mutableStateOf(false) }
    val dismisser = remember { mutableStateOf<() -> Unit>({ onDismiss() }) }
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    fun readContact(uri: Uri) {
        val found = try {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val picked = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
            val id = if (idIdx >= 0) cursor.getString(idIdx) else null
            var number = ""
            if (!id.isNullOrBlank() && ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                ctx.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )?.use { phones ->
                    if (phones.moveToFirst()) number = phones.getString(0).orEmpty()
                }
            }
            picked to number
        }
        } catch (_: Exception) {
            null
        }
        if (found == null || found.first.isBlank()) {
            err = t("Allow contacts to fill in the name.")
        } else {
            name = found.first
            if (found.second.isNotBlank()) phone = found.second
            err = ""
        }
    }
    val pickContact = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) readContact(uri)
    }
    val askContacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pickContact.launch(null) else err = t("Allow contacts to fill in the name.")
    }
    fun chooseContact() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            pickContact.launch(null)
        } else {
            askContacts.launch(Manifest.permission.READ_CONTACTS)
        }
    }
    fun close(save: Entry?) {
        if (closing) return
        closing = true
        if (save != null) onSave(save)
        dismisser.value()
    }
    FastSheet(onDismiss = onDismiss) {
        val hide = LocalDismissSheet.current
        SideEffect { dismisser.value = hide }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            Text(
                if (existing == null) t("New") else t("Edit"),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = ios.label,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )
            Spacer(Modifier.height(8.dp))
            IosSegmented(
                listOf(t("I lent"), t("I borrowed")),
                selected = if (lent) 0 else 1,
                onSelect = { lent = it == 0 },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            IosSegmented(
                listOf(t("Money"), t("Item")),
                selected = if (money) 0 else 1,
                onSelect = { money = it == 0 },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(16.dp))
            Group {
                InputRow("Person", name, { name = it }, "Name")
                Hairline()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { chooseContact() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(t("Contacts"), color = ios.blue, fontSize = 17.sp)
                }
                Hairline()
                InputRow("Phone", phone, { phone = it }, "Optional", KeyboardOptions(keyboardType = KeyboardType.Phone))
            }
            AnimatedVisibility(money, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Group {
                        InputRow("Amount", amount, { amount = it }, "0", decimal)
                        Hairline()
                        InputRow("Rate", rate, { rate = it }, "% / month", decimal)
                    }
                }
            }
            AnimatedVisibility(!money, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Group {
                        InputRow("Item", desc, { desc = it }, "What was given")
                    }
                }
            }
            SectionLabel("When")
            Group {
                Row(
                    Modifier.fillMaxWidth().clickable { picker = 1 }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Given", modifier = Modifier.width(84.dp), color = ios.label, fontSize = 17.sp)
                    Text(fmt(date), color = ios.blue, fontSize = 17.sp)
                }
                Hairline()
                Row(
                    Modifier.fillMaxWidth().clickable { picker = 2 }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Return", modifier = Modifier.width(84.dp), color = ios.label, fontSize = 17.sp)
                    Text(
                        due?.let { fmt(it) } ?: "Add date",
                        modifier = Modifier.weight(1f),
                        color = if (due == null) ios.tertiary else ios.blue,
                        fontSize = 17.sp
                    )
                    if (due != null) {
                        Text(
                            "Clear",
                            color = ios.red,
                            fontSize = 17.sp,
                            modifier = Modifier.clickable { due = null }.padding(start = 8.dp)
                        )
                    }
                }
            }
            SectionLabel("Note")
            Group {
                InputRow("Note", note, { note = it }, "Optional")
            }
            if (err.isNotBlank()) {
                Text(
                    err,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = ios.red,
                    fontSize = 13.sp
                )
            } else {
                Spacer(Modifier.height(12.dp))
            }
            PrimaryButton(
                if (existing == null) t("Add an entry") else t("Save"),
                {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    val dueValue = due
                    when {
                        name.isBlank() -> err = "Enter a name."
                        money && amt <= 0 -> err = "Enter an amount."
                        !money && desc.isBlank() -> err = "Enter the item."
                        dueValue != null && dueValue < date -> err = "Return date is before the given date."
                        else -> {
                            val base = existing
                            close(
                                Entry(
                                    id = base?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    isMoney = money,
                                    amount = if (money) amt else 0.0,
                                    desc = if (money) "" else desc.trim(),
                                    date = date,
                                    due = dueValue,
                                    rate = if (money) (rate.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0) else 0.0,
                                    note = note.trim(),
                                    paid = base?.paid ?: 0.0,
                                    returned = base?.returned ?: false,
                                    returnedAt = base?.returnedAt,
                                    direction = if (lent) LENT else BORROWED,
                                    phone = phone.trim(),
                                    payments = base?.payments ?: emptyList()
                                )
                            )
                        }
                    }
                },
                Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clickable { close(null) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(t("Cancel"), color = ios.blue, fontSize = 17.sp)
            }
        }
    }
    if (picker != 0) {
        val initial = if (picker == 1) localToPicker(date) else due?.let { localToPicker(it) } ?: localToPicker(date)
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { picker = 0 },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { picked ->
                        val day = pickerToLocal(picked)
                        if (picker == 1) date = day else due = day
                    }
                    picker = 0
                }) { Text("Done") }
            },
            dismissButton = { TextButton({ picker = 0 }) { Text(t("Cancel")) } }
        ) { DatePicker(state) }
    }
}

@Composable
fun PaymentSheet(entry: Entry, onDismiss: () -> Unit, onSave: (Double, String) -> Unit) {
    val ios = LocalIos.current
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    var closing by remember { mutableStateOf(false) }
    val dismisser = remember { mutableStateOf<() -> Unit>({ onDismiss() }) }
    val typed = amount.toDoubleOrNull() ?: 0.0
    val left = (owing(entry) - typed).coerceAtLeast(0.0)
    fun close(save: Pair<Double, String>?) {
        if (closing) return
        closing = true
        if (save != null) onSave(save.first, save.second)
        dismisser.value()
    }
    FastSheet(onDismiss = onDismiss) {
        val hide = LocalDismissSheet.current
        SideEffect { dismisser.value = hide }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            Text(
                t("Payment"),
                modifier = Modifier.padding(horizontal = 16.dp),
                color = ios.label,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Group {
                InputRow("Amount", amount, { amount = it }, "0", KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Hairline()
                InputRow("Note", note, { note = it }, "Optional")
            }
            SectionFooter(
                when {
                    typed <= 0 -> "Still open ${inr(owing(entry))}"
                    left <= 0.5 -> "This settles the entry."
                    else -> "Left after this ${inr(left)}"
                }
            )
            if (err.isNotBlank()) {
                Text(err, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = ios.red, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                t("Save"),
                {
                    val value = amount.toDoubleOrNull()
                    if (value == null || value <= 0) err = "Enter an amount."
                    else close(value to note)
                },
                Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            )
            Box(
                Modifier.fillMaxWidth().clickable { close(null) }.padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(t("Cancel"), color = ios.blue, fontSize = 17.sp)
            }
        }
    }
}

@Composable
fun DeleteSheet(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val ios = LocalIos.current
    var closing by remember { mutableStateOf(false) }
    val dismisser = remember { mutableStateOf<() -> Unit>({ onDismiss() }) }
    fun close(confirm: Boolean) {
        if (closing) return
        closing = true
        if (confirm) onConfirm()
        dismisser.value()
    }
    FastSheet(onDismiss = onDismiss, container = Color.Transparent, handle = false) {
        val hide = LocalDismissSheet.current
        SideEffect { dismisser.value = hide }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp)
                .padding(bottom = 10.dp)
        ) {
            Group {
                Column(
                    Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Delete this entry?", color = ios.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "This removes $name’s entry.",
                        color = ios.secondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Hairline(0.dp)
                Box(
                    Modifier.fillMaxWidth().clickable { close(true) }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Delete entry", color = ios.red, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Group {
                Box(
                    Modifier.fillMaxWidth().clickable { close(false) }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(t("Cancel"), color = ios.blue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EntryGroup(entries: List<Entry>, onOpen: (Entry) -> Unit) {
    Group {
        entries.forEachIndexed { index, entry ->
            EntryRow(entry) { onOpen(entry) }
            if (index != entries.lastIndex) Hairline(66.dp)
        }
    }
}

@Composable
private fun EntryRow(entry: Entry, onClick: () -> Unit) {
    val ios = LocalIos.current
    val status = statusOf(entry)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(entry.name, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.name, color = ios.label, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = ios.secondary)) { append(rowKind(entry)) }
                    withStyle(SpanStyle(color = toneColor(status.tone))) { append(" · ${status.label}") }
                },
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            if (entry.isMoney) inr(if (entry.returned) entry.amount else owing(entry)) else entry.desc.ifBlank { "Item" },
            modifier = Modifier.padding(start = 8.dp).widthIn(max = 120.dp),
            color = ios.label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        androidx.compose.material3.Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ios.tertiary,
            modifier = Modifier.padding(start = 2.dp).size(20.dp)
        )
    }
}

@Composable
private fun PersonRow(person: Person, onClick: () -> Unit) {
    val ios = LocalIos.current
    val subtitle = when {
        person.open.isEmpty() -> "All settled"
        person.net > 0.5 -> "Owes you · ${person.open.size} open"
        person.net < -0.5 -> "You owe · ${person.open.size} open"
        else -> "${person.open.size} open"
    }
    val value = when {
        person.net > 0.5 -> inr(person.net)
        person.net < -0.5 -> inr(-person.net)
        else -> ""
    }
    val valueColor = when {
        person.net > 0.5 -> ios.green
        person.net < -0.5 -> ios.orange
        else -> ios.secondary
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(person.name, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(person.name, color = ios.label, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = ios.secondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (value.isNotEmpty()) {
            Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        androidx.compose.material3.Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ios.tertiary,
            modifier = Modifier.padding(start = 2.dp).size(20.dp)
        )
    }
}

@Composable
private fun AlertRow(alert: AppAlert, onClick: () -> Unit) {
    val ios = LocalIos.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(if (alert.read) Color.Transparent else ios.blue)
        )
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text(
                alert.title,
                color = ios.label,
                fontSize = 17.sp,
                fontWeight = if (alert.read) FontWeight.Normal else FontWeight.SemiBold
            )
            Text(alert.body, color = ios.secondary, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                fmtWhen(alert.time),
                modifier = Modifier.padding(top = 2.dp),
                color = ios.tertiary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun MoneyLine(label: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), color = LocalIos.current.label, fontSize = 17.sp)
        Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

private fun visibleEntries(items: List<Entry>, scope: String, chip: String, query: String): List<Entry> {
    val q = query.trim()
    return items.filter { entry ->
        val scopeOk = if (scope == "open") !entry.returned else entry.returned
        val chipOk = when (chip) {
            "money" -> entry.isMoney
            "item" -> !entry.isMoney
            "overdue" -> !entry.returned && (entry.due ?: Long.MAX_VALUE) < todayMs()
            else -> true
        }
        val queryOk = q.isBlank() ||
            entry.name.contains(q, true) ||
            entry.desc.contains(q, true) ||
            entry.note.contains(q, true) ||
            entry.phone.contains(q)
        scopeOk && chipOk && queryOk
    }.sortedWith(
        if (scope == "open") compareBy { it.due ?: Long.MAX_VALUE }
        else compareByDescending { it.returnedAt ?: 0L }
    )
}

private fun rowKind(entry: Entry): String = when {
    entry.isMoney && entry.direction == LENT -> "Lent"
    entry.isMoney && entry.direction == BORROWED -> "Borrowed"
    entry.direction == LENT -> "Your item"
    else -> "Their item"
}

private fun personMessage(person: Person): String {
    val open = person.open
    if (open.size == 1) return reminderText(open.first())
    if (open.isEmpty()) return "Hi ${person.name}, thanks — everything between us is settled."
    val lines = open.joinToString("\n") { entry ->
        val what = if (entry.isMoney) inr(owing(entry)) else entry.desc.ifBlank { "an item" }
        "• $what"
    }
    return "Hi ${person.name}, a gentle reminder about:\n$lines\nThank you!"
}

private fun trimNum(value: Double): String {
    val rounded = Math.round(value * 100.0) / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString()
    else String.format(Locale.ENGLISH, "%.2f", rounded).trimEnd('0').trimEnd('.')
}

private fun shareText(ctx: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    ctx.startActivity(Intent.createChooser(send, null))
}

private fun sms(ctx: Context, phone: String, body: String) {
    ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).putExtra("sms_body", body))
}

private fun dial(ctx: Context, phone: String) {
    ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
}

private fun openAppSettings(ctx: Context) {
    ctx.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
    )
}

@Composable
fun LanguagePick(onContinue: (String) -> Unit) {
    val ios = LocalIos.current
    var picked by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(ios.groupedBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Text(
            t("Choose a language"),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
            color = ios.label,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            Group {
                Languages.forEachIndexed { index, option ->
                    if (index > 0) Hairline()
                    CheckRow(option.native, picked == option.code) { picked = option.code }
                }
            }
        }
        AnimatedVisibility(visible = picked != null) {
            PrimaryButton(
                t("Continue"),
                { picked?.let(onContinue) },
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
fun CurrencyPick(onContinue: (String) -> Unit) {
    val ios = LocalIos.current
    var picked by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(ios.groupedBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Text(
            t("Choose a currency"),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
            color = ios.label,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            Group {
                Currencies.forEachIndexed { index, option ->
                    if (index > 0) Hairline()
                    val label = "${currencySymbol(option.code)}  ${t(option.name)}"
                    CheckRow(label, picked == option.code) { picked = option.code }
                }
            }
        }
        AnimatedVisibility(visible = picked != null) {
            PrimaryButton(
                t("Continue"),
                { picked?.let(onContinue) },
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
fun IntroScreen(onDone: () -> Unit) {
    val ios = LocalIos.current
    val scope = rememberCoroutineScope()
    val pages = listOf(
        t("Keep track of what’s owed") to t("Money and things you lent or borrowed, in one place."),
        t("Reminders that show up") to t("A nudge before the date, on the day, and again if it’s overdue."),
        t("Your currency, your language") to t("Amounts and words follow what you choose.")
    )
    val pager = rememberPagerState(pageCount = { pages.size })
    val page = pager.currentPage
    Column(
        Modifier
            .fillMaxSize()
            .background(ios.groupedBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { index ->
            val (title, body) = pages[index]
            val distance = (pager.currentPage - index) + pager.currentPageOffsetFraction
            Column(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val away = kotlin.math.abs(distance).coerceIn(0f, 1f)
                        alpha = 1f - away * 0.25f
                        translationY = away * 18.dp.toPx()
                    }
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier.size(88.dp).clip(CircleShape).background(ios.blue),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(46.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(22.dp).clip(CircleShape).background(ios.blue))
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text(title, color = ios.label, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                Spacer(Modifier.height(10.dp))
                Text(body, color = ios.secondary, fontSize = 17.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.Center) {
            pages.indices.forEach { index ->
                val on = index == page
                val width by animateDpAsState(if (on) 18.dp else 6.dp, tween(220), label = "dot")
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .height(6.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(if (on) ios.blue else ios.fill)
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                t("Skip"),
                color = ios.blue,
                fontSize = 17.sp,
                modifier = Modifier.clickable(onClick = onDone).padding(vertical = 12.dp)
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                if (page == pages.lastIndex) t("Start") else t("Next"),
                {
                    if (page == pages.lastIndex) onDone()
                    else scope.launch { pager.animateScrollToPage(page + 1) }
                },
                Modifier.widthIn(min = 140.dp)
            )
        }
    }
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
