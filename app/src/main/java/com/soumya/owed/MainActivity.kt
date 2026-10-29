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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import java.util.UUID

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
    data class Add(val name: String = "", val direction: Int = LENT) : Sheet
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
        readLaunch(intent)
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
        readLaunch(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.reload()
        vm.rescheduleAll()
        BalanceWidget.update(this)
    }

    private fun readLaunch(intent: Intent?) {
        intent?.getStringExtra("open")?.let { id ->
            intent.removeExtra("open")
            vm.requestOpen(id)
        }
        intent?.getStringExtra("add")?.let { which ->
            intent.removeExtra("add")
            vm.requestAdd(if (which == "borrowed") BORROWED else LENT)
        }
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
    LaunchedEffect(vm.pendingAdd, vm.introSeen) {
        if (!vm.introSeen) return@LaunchedEffect
        val direction = vm.pendingAdd ?: return@LaunchedEffect
        vm.consumeAdd()
        sheet = Sheet.Add(direction = direction)
    }
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
        when (startStep) {
            0 -> LanguagePick { code ->
                vm.changeLanguage(code)
                startStep = 1
            }
            1 -> IntroScreen(onDone = { startStep = 2 })
            else -> CurrencyPick { code ->
                vm.changeCurrency(code)
                vm.finishIntro()
            }
        }
        return
    }
    val navPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val showAdd = sheet == null && (page is Page.Home || page is Page.Entries || page is Page.People || page is Page.PersonDetail)
    Box(Modifier.fillMaxSize().background(ios.groupedBg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                AnimatedContent(
                    targetState = page,
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                    transitionSpec = {
                        val easing = FastOutSlowInEasing
                        val change = when (motion) {
                            1 -> (slideInHorizontally(tween(220, easing = easing)) { it } + fadeIn(tween(140))) togetherWith
                                (slideOutHorizontally(tween(180, easing = easing)) { -it / 6 } + fadeOut(tween(120)))
                            -1 -> (slideInHorizontally(tween(200, easing = easing)) { -it / 6 } + fadeIn(tween(120))) togetherWith
                                (slideOutHorizontally(tween(200, easing = easing)) { it } + fadeOut(tween(140)))
                            else -> fadeIn(tween(140)) togetherWith fadeOut(tween(90))
                        }
                        (change.apply {
                            targetContentZIndex = if (motion == -1) 0f else 1f
                        }) using null
                    },
                    label = "page"
                ) { current ->
                val speak = vm.language
                val payWith = vm.currency
                if (speak.isEmpty() && payWith.isEmpty()) return@AnimatedContent
                when (current) {
                    Page.Home -> HomeScreen(
                        vm = vm,
                        onAdd = { sheet = Sheet.Add() },
                        onOpen = { push(Page.Detail(it.id)) },
                        onEntries = { select(Tab.Entries) },
                        onPeople = { select(Tab.People) },
                        onPerson = { push(Page.PersonDetail(it.key)) },
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
            is Sheet.Add -> EditorSheet(
                existing = null,
                presetName = current.name,
                presetLent = current.direction == LENT,
                onDismiss = { sheet = null },
                onSave = { vm.add(it) }
            )
            is Sheet.Edit -> {
                val entry = vm.items.find { it.id == current.id }
                if (entry == null) {
                    LaunchedEffect(current.id) { sheet = null }
                } else {
                    EditorSheet(entry, "", onDismiss = { sheet = null }) { vm.update(it) }
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
