package com.soumya.owed

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import java.util.UUID

class OwedVm(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    var items by mutableStateOf(store.load())
        private set
    var alerts by mutableStateOf(store.loadAlerts())
        private set
    var notificationsOn by mutableStateOf(store.notificationsEnabled())
        private set
    var reminderHour by mutableIntStateOf(store.reminderHour())
        private set
    var remindDays by mutableIntStateOf(store.remindDaysBefore())
        private set
    var appearance by mutableStateOf(store.appearance())
        private set
    var language by mutableStateOf(store.language())
        private set
    var currency by mutableStateOf(store.currency())
        private set
    var introSeen by mutableStateOf(store.introSeen())
        private set
    var banner by mutableStateOf<Banner?>(null)
        private set
    var pendingOpen by mutableStateOf<String?>(null)
        private set

    init {
        LocaleHub.lang = language
        LocaleHub.currency = currency
        val added = syncAlerts()
        rescheduleAll()
        when {
            added.size > 1 -> {
                val overdueCount = added.count { it.kind == "overdue" }
                if (overdueCount > 0) {
                    val body = if (overdueCount == 1) {
                        "1 entry is past the return date."
                    } else {
                        "$overdueCount entries are past the return date."
                    }
                    flash("Overdue", body, "overdue", null)
                } else {
                    flash("Coming up", "${added.size} return dates are close.", "soon", null)
                }
            }
            added.size == 1 -> {
                val alert = added.first()
                flash(alert.title, alert.body, alert.kind, alert.entryId)
            }
        }
    }

    fun requestOpen(id: String?) {
        if (!id.isNullOrBlank()) pendingOpen = id
    }

    fun consumeOpen() {
        pendingOpen = null
    }

    fun dismissBanner() {
        banner = null
    }

    fun flash(title: String, body: String, kind: String = "info", entryId: String? = null) {
        banner = Banner(UUID.randomUUID().toString(), title, body, kind, entryId)
    }

    fun reloadAlerts() {
        alerts = store.loadAlerts()
    }

    fun rescheduleAll() {
        val ctx = getApplication<Application>()
        items.forEach { Reminders.schedule(ctx, it) }
    }

    fun add(entry: Entry) {
        commit(items + entry)
        val urgent = dueSnapshot(entry)
        if (urgent != null) {
            pushAlert(urgent)
            flash(urgent.title, urgent.body, urgent.kind, entry.id)
        } else {
            pushAlert(
                AppAlert(
                    id = UUID.randomUUID().toString(),
                    entryId = entry.id,
                    kind = "added",
                    title = "Saved",
                    body = "${entry.name} is on your list.",
                    time = System.currentTimeMillis(),
                    read = false
                )
            )
            flash("Saved", "${entry.name} is on your list.", "added", entry.id)
        }
    }

    fun update(entry: Entry) {
        val settled = entry.isMoney && !entry.returned && entry.paid > 0 && owing(entry) <= 0.5
        val next = if (settled) entry.copy(returned = true, returnedAt = entry.returnedAt ?: todayMs()) else entry
        commit(items.map { if (it.id == next.id) next else it })
        if (settled) flash("Settled", "${entry.name} is clear.", "returned", entry.id)
        else flash("Updated", entry.name, "info", entry.id)
    }

    fun delete(entry: Entry) {
        Reminders.cancel(getApplication(), entry)
        commit(items.filter { it.id != entry.id })
        flash("Deleted", "${entry.name} was removed.", "info", null)
    }

    fun recordPayment(id: String, amount: Double, note: String) {
        if (amount <= 0) return
        val current = items.find { it.id == id } ?: return
        val next = applyPayment(current, UUID.randomUUID().toString(), amount, note, System.currentTimeMillis())
        commit(items.map { if (it.id == id) next else it })
        val settled = next.returned && !current.returned
        if (settled) {
            pushAlert(fresh(id, "returned", "Settled", "${current.name} is clear."))
            flash("Settled", "${current.name} is clear.", "returned", id)
        } else {
            pushAlert(fresh(id, "payment", "Payment recorded", "${inr(amount)} · ${current.name}"))
            flash("Payment recorded", "${inr(amount)} · ${current.name}", "payment", id)
        }
    }

    fun markReturned(id: String) {
        val current = items.find { it.id == id } ?: return
        if (current.returned) return
        val next = current.copy(returned = true, returnedAt = todayMs())
        commit(items.map { if (it.id == id) next else it })
        pushAlert(fresh(id, "returned", "Marked returned", "${current.name} is settled."))
        flash("Returned", "${current.name} is settled.", "returned", id)
    }

    fun reopen(id: String) {
        val current = items.find { it.id == id } ?: return
        commit(items.map { if (it.id == id) current.copy(returned = false, returnedAt = null) else it })
        flash("Reopened", "${current.name} is active again.", "info", id)
    }

    fun markRead(id: String) {
        alerts = alerts.map { if (it.id == id) it.copy(read = true) else it }
        store.saveAlerts(alerts)
    }

    fun markAllRead() {
        alerts = alerts.map { it.copy(read = true) }
        store.saveAlerts(alerts)
    }

    fun clearAlerts() {
        alerts = emptyList()
        store.saveAlerts(emptyList())
    }

    fun setNotifications(on: Boolean) {
        notificationsOn = on
        store.setNotificationsEnabled(on)
        rescheduleAll()
    }

    fun setHour(hour: Int) {
        reminderHour = hour
        store.setReminderHour(hour)
        rescheduleAll()
    }

    fun setLead(days: Int) {
        remindDays = days
        store.setRemindDaysBefore(days)
        rescheduleAll()
    }

    fun changeAppearance(value: String) {
        if (value != "system" && value != "light" && value != "dark") return
        appearance = value
        store.setAppearance(value)
    }

    fun changeLanguage(code: String) {
        if (Languages.none { it.code == code } || code == language) return
        language = code
        store.setLanguage(code)
        LocaleHub.lang = code
        java.util.Locale.setDefault(localeFor(code))
    }

    fun changeCurrency(code: String) {
        if (Currencies.none { it.code == code }) return
        currency = code
        store.setCurrency(code)
        LocaleHub.currency = code
    }

    fun finishIntro() {
        introSeen = true
        store.setIntroSeen()
    }

    fun sendTest(ctx: Context) {
        if (!notificationsOn) {
            flash("Alerts are off", "Turn on alerts, then try again.", "soon", null)
            return
        }
        val title = "Alerts are on"
        val body = "You’ll get a reminder when a return date arrives."
        pushAlert(fresh("", "test", title, body))
        if (canNotify(ctx)) {
            Reminders.notify(ctx, title, body, 42, null)
            flash(title, body, "test", null)
        } else {
            flash(
                "Allow phone alerts",
                "Owed saved this here. Allow notifications to also see it on the lock screen.",
                "soon",
                null
            )
        }
    }

    private fun commit(list: kotlin.collections.List<Entry>) {
        items = list
        store.save(list)
        rescheduleAll()
    }

    private fun pushAlert(alert: AppAlert) {
        if (alerts.any { it.id == alert.id }) return
        alerts = (listOf(alert) + alerts).take(80)
        store.saveAlerts(alerts)
    }

    private fun fresh(entryId: String, kind: String, title: String, body: String) = AppAlert(
        id = UUID.randomUUID().toString(),
        entryId = entryId,
        kind = kind,
        title = title,
        body = body,
        time = System.currentTimeMillis(),
        read = false
    )

    private fun dueSnapshot(entry: Entry): AppAlert? {
        val due = entry.due ?: return null
        if (entry.returned) return null
        val days = daysBetween(todayMs(), due)
        val kind = when {
            days < 0 -> "overdue"
            days == 0L -> "due"
            days in 1..3 -> "soon"
            else -> return null
        }
        val id = "${entry.id}:$kind:${todayMs()}"
        if (alerts.any { it.id == id }) return null
        return AppAlert(id, entry.id, kind, alertTitle(kind, entry), alertBody(entry), System.currentTimeMillis(), false)
    }

    private fun syncAlerts(): kotlin.collections.List<AppAlert> {
        val today = todayMs()
        val created = mutableListOf<AppAlert>()
        items.filter { !it.returned && it.due != null }.forEach { entry ->
            val due = entry.due ?: return@forEach
            val days = daysBetween(today, due)
            val kind = when {
                days < 0 -> "overdue"
                days == 0L -> "due"
                days in 1..3 -> "soon"
                else -> return@forEach
            }
            val id = "${entry.id}:$kind:$today"
            if (alerts.any { it.id == id } || created.any { it.id == id }) return@forEach
            created += AppAlert(id, entry.id, kind, alertTitle(kind, entry), alertBody(entry), System.currentTimeMillis(), false)
        }
        if (created.isNotEmpty()) {
            alerts = (created + alerts).take(80)
            store.saveAlerts(alerts)
        }
        return created
    }
}

sealed interface Page {
    data object Home : Page
    data object Entries : Page
    data object People : Page
    data object Alerts : Page
    data object Settings : Page
    data class Detail(val id: String) : Page
    data class PersonDetail(val key: String) : Page
}

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Entries("Entries", Icons.Filled.List),
    People("People", Icons.Filled.Person),
    Alerts("Alerts", Icons.Filled.Notifications),
    Settings("Settings", Icons.Filled.Settings);

    fun root(): Page = when (this) {
        Home -> Page.Home
        Entries -> Page.Entries
        People -> Page.People
        Alerts -> Page.Alerts
        Settings -> Page.Settings
    }
}

sealed interface Sheet {
    data class Add(val name: String = "") : Sheet
    data class Edit(val id: String) : Sheet
    data class Pay(val id: String) : Sheet
    data class Delete(val id: String) : Sheet
}

class MainActivity : ComponentActivity() {
    private val vm: OwedVm by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val code = newBase.getSharedPreferences("owed", Context.MODE_PRIVATE).getString("lang", "en") ?: "en"
        val locale = localeFor(code)
        java.util.Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent.getStringExtra("open")?.let { id ->
            intent.removeExtra("open")
            vm.requestOpen(id)
        }
        setContent {
            val dark = when (vm.appearance) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            val direction = if (vm.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                OwedTheme(dark) {
                    Box(Modifier.fillMaxSize()) {
                        App(vm)
                        SplashFade()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("open")?.let { id ->
            intent.removeExtra("open")
            vm.requestOpen(id)
        }
    }

    override fun onResume() {
        super.onResume()
        vm.reloadAlerts()
        vm.rescheduleAll()
    }
}

@Composable
private fun SplashFade() {
    val ios = LocalIos.current
    var hide by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        if (hide) 0f else 1f,
        tween(420, easing = FastOutSlowInEasing),
        label = "splash"
    )
    LaunchedEffect(Unit) { hide = true }
    if (alpha <= 0.01f) return
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(ios.groupedBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {},
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(ios.blue),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(50.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(ios.blue))
            }
        }
    }
}

@Composable
fun App(vm: OwedVm) {
    val context = LocalContext.current
    val ios = LocalIos.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !canNotify(context)) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    var encoded by rememberSaveable { mutableStateOf("home") }
    var tabName by rememberSaveable { mutableStateOf(Tab.Home.name) }
    var scope by rememberSaveable { mutableStateOf("open") }
    var chip by rememberSaveable { mutableStateOf("all") }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var motion by remember { mutableIntStateOf(0) }
    var slide by remember { mutableIntStateOf(1) }
    val stack = decode(encoded)
    val tab = Tab.entries.find { it.name == tabName } ?: Tab.Home
    fun setStack(next: kotlin.collections.List<Page>) {
        encoded = encode(next)
    }
    fun select(next: Tab) {
        slide = next.ordinal - tab.ordinal
        motion = 0
        tabName = next.name
        setStack(listOf(next.root()))
    }
    fun push(page: Page) {
        slide = 1
        motion = 1
        setStack(stack + page)
    }
    fun pop() {
        if (stack.size > 1) {
            slide = -1
            motion = -1
            setStack(stack.dropLast(1))
        }
    }
    BackHandler(enabled = stack.size > 1) { pop() }
    LaunchedEffect(vm.pendingOpen) {
        val id = vm.pendingOpen ?: return@LaunchedEffect
        vm.consumeOpen()
        if (vm.items.any { it.id == id }) {
            motion = 1
            slide = 1
            tabName = Tab.Home.name
            setStack(listOf(Page.Home, Page.Detail(id)))
        }
    }
    val page = stack.last()
    var startStep by rememberSaveable { mutableIntStateOf(0) }
    if (!vm.introSeen) {
        if (startStep == 0) {
            LanguagePick { code ->
                vm.changeLanguage(code)
                startStep = 1
            }
        } else {
            IntroScreen(onDone = { vm.finishIntro() })
        }
        return
    }
    val navPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val showAdd = sheet == null && (page is Page.Home || page is Page.Entries || page is Page.People || page is Page.PersonDetail)
    Box(Modifier.fillMaxSize().background(ios.groupedBg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = page,
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                    transitionSpec = {
                        val easing = FastOutSlowInEasing
                        when (motion) {
                            1 -> slideInHorizontally(tween(280, easing = easing)) { it } togetherWith
                                slideOutHorizontally(tween(280, easing = easing)) { -it / 4 }
                            -1 -> slideInHorizontally(tween(280, easing = easing)) { -it / 4 } togetherWith
                                slideOutHorizontally(tween(280, easing = easing)) { it }
                            else -> {
                                val dir = if (slide >= 0) 1 else -1
                                slideInHorizontally(tween(240, easing = easing)) { dir * it / 3 } togetherWith
                                    slideOutHorizontally(tween(240, easing = easing)) { -dir * it / 3 }
                            }
                        }
                    },
                    label = "page"
                ) { current ->
                when (current) {
                    Page.Home -> HomeScreen(
                        vm = vm,
                        onAdd = { sheet = Sheet.Add() },
                        onOpen = { push(Page.Detail(it.id)) },
                        onEntries = { select(Tab.Entries) },
                        onOverdue = {
                            scope = "open"
                            chip = "overdue"
                            select(Tab.Entries)
                        },
                        onAlerts = { select(Tab.Alerts) }
                    )
                    Page.Entries -> EntriesScreen(
                        vm = vm,
                        scope = scope,
                        chip = chip,
                        onScope = { scope = it },
                        onChip = { chip = it },
                        onAdd = { sheet = Sheet.Add() },
                        onOpen = { push(Page.Detail(it.id)) }
                    )
                    Page.People -> PeopleScreen(vm, onOpen = { push(Page.PersonDetail(it.key)) })
                    Page.Alerts -> AlertsScreen(vm) { alert ->
                        vm.markRead(alert.id)
                        if (alert.entryId.isNotBlank() && vm.items.any { it.id == alert.entryId }) {
                            push(Page.Detail(alert.entryId))
                        } else if (alert.entryId.isNotBlank()) {
                            vm.flash("Entry removed", "That reminder is no longer on your list.", "info", null)
                        }
                    }
                    Page.Settings -> SettingsScreen(vm)
                    is Page.Detail -> EntryScreen(
                        vm = vm,
                        id = current.id,
                        onBack = { pop() },
                        onEdit = { sheet = Sheet.Edit(current.id) },
                        onPay = { sheet = Sheet.Pay(current.id) },
                        onDelete = { sheet = Sheet.Delete(current.id) }
                    )
                    is Page.PersonDetail -> PersonScreen(
                        vm = vm,
                        key = current.key,
                        onBack = { pop() },
                        onOpen = { push(Page.Detail(it.id)) }
                    )
                }
                }
            }
            if (stack.size == 1) {
                TabBar(tab, vm.alerts.count { !it.read }) { select(it) }
            }
        }
        if (showAdd) {
            AddFab(
                onClick = {
                    val here = stack.last()
                    sheet = if (here is Page.PersonDetail) {
                        Sheet.Add(peopleOf(vm.items).find { it.key == here.key }?.name.orEmpty())
                    } else {
                        Sheet.Add()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = navPad + if (stack.size == 1) 64.dp else 16.dp)
            )
        }
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
            BannerHost(vm.banner, vm::dismissBanner) { banner ->
                val id = banner.entryId
                if (!id.isNullOrBlank() && vm.items.any { it.id == id }) push(Page.Detail(id))
                else select(Tab.Alerts)
            }
        }
        when (val current = sheet) {
            null -> Unit
            is Sheet.Add -> EditorSheet(null, current.name, { sheet = null }) { vm.add(it) }
            is Sheet.Edit -> {
                val entry = vm.items.find { it.id == current.id }
                if (entry == null) {
                    LaunchedEffect(current.id) { sheet = null }
                } else {
                    EditorSheet(entry, "", { sheet = null }) { vm.update(it) }
                }
            }
            is Sheet.Pay -> {
                val entry = vm.items.find { it.id == current.id }
                if (entry == null) {
                    LaunchedEffect(current.id) { sheet = null }
                } else {
                    PaymentSheet(entry, { sheet = null }) { amount, note -> vm.recordPayment(entry.id, amount, note) }
                }
            }
            is Sheet.Delete -> {
                val entry = vm.items.find { it.id == current.id }
                if (entry == null) {
                    LaunchedEffect(current.id) { sheet = null }
                } else {
                    DeleteSheet(entry.name, { sheet = null }) { vm.delete(entry) }
                }
            }
        }
    }
}

private fun encode(stack: kotlin.collections.List<Page>): String = stack.joinToString("\u001f") { page ->
    when (page) {
        Page.Home -> "home"
        Page.Entries -> "entries"
        Page.People -> "people"
        Page.Alerts -> "alerts"
        Page.Settings -> "settings"
        is Page.Detail -> "e:${page.id}"
        is Page.PersonDetail -> "p:${page.key}"
    }
}

private fun decode(raw: String): kotlin.collections.List<Page> {
    val pages = raw.split('\u001f').mapNotNull { token ->
                    when {
            token == "home" -> Page.Home
            token == "entries" -> Page.Entries
            token == "people" -> Page.People
            token == "alerts" -> Page.Alerts
            token == "settings" -> Page.Settings
            token.startsWith("e:") && token.length > 2 -> Page.Detail(token.substring(2))
            token.startsWith("p:") && token.length > 2 -> Page.PersonDetail(token.substring(2))
            else -> null
        }
    }
    return pages.ifEmpty { listOf(Page.Home) }
}
