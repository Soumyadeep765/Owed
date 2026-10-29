package com.soumya.owed

import android.app.Activity
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class IosPalette(
    val groupedBg: Color,
    val card: Color,
    val elevated: Color,
    val label: Color,
    val secondary: Color,
    val tertiary: Color,
    val separator: Color,
    val blue: Color,
    val green: Color,
    val red: Color,
    val orange: Color,
    val fill: Color,
    val thumb: Color,
    val switchOff: Color,
    val tabBar: Color,
    val dark: Boolean
)

private val LightIos = IosPalette(
    groupedBg = Color(0xFFF2F2F7),
    card = Color.White,
    elevated = Color.White,
    label = Color.Black,
    secondary = Color(0x993C3C43),
    tertiary = Color(0x4D3C3C43),
    separator = Color(0x4A3C3C43),
    blue = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    orange = Color(0xFFFF9500),
    fill = Color(0xFFE9E9EB),
    thumb = Color.White,
    switchOff = Color(0xFFE9E9EB),
    tabBar = Color(0xFFF8F8F8),
    dark = false
)

private val DarkIos = IosPalette(
    groupedBg = Color.Black,
    card = Color(0xFF1C1C1E),
    elevated = Color(0xFF2C2C2E),
    label = Color.White,
    secondary = Color(0x99EBEBF5),
    tertiary = Color(0x4DEBEBF5),
    separator = Color(0xA6545458),
    blue = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    orange = Color(0xFFFF9F0A),
    fill = Color(0xFF2C2C2E),
    thumb = Color(0xFF636366),
    switchOff = Color(0xFF39393D),
    tabBar = Color(0xFF1C1C1E),
    dark = true
)

val LocalIos = staticCompositionLocalOf { LightIos }
val LocalDismissSheet = staticCompositionLocalOf<() -> Unit> { {} }

private val AvatarColors = listOf(
    Color(0xFF007AFF),
    Color(0xFF34C759),
    Color(0xFFFF9500),
    Color(0xFFAF52DE),
    Color(0xFFFF2D55),
    Color(0xFF5AC8FA),
    Color(0xFF5856D6)
)

fun avatarColor(name: String): Color {
    val index = (name.trim().lowercase().hashCode() and Int.MAX_VALUE) % AvatarColors.size
    return AvatarColors[index]
}

@Composable
fun toneColor(tone: Tone): Color {
    val ios = LocalIos.current
    return when (tone) {
        Tone.Good -> ios.green
        Tone.Warn -> ios.orange
        Tone.Bad -> ios.red
        Tone.Mute -> ios.secondary
    }
}

@Composable
fun OwedTheme(dark: Boolean, content: @Composable () -> Unit) {
    val ios = if (dark) DarkIos else LightIos
    val scheme = if (dark) {
        darkColorScheme(
            primary = ios.blue,
            onPrimary = Color.White,
            background = ios.groupedBg,
            surface = ios.card,
            onSurface = ios.label,
            onSurfaceVariant = ios.secondary,
            outline = ios.separator,
            error = ios.red,
            surfaceContainerHigh = ios.elevated,
            surfaceContainer = ios.card
        )
    } else {
        lightColorScheme(
            primary = ios.blue,
            onPrimary = Color.White,
            background = ios.groupedBg,
            surface = ios.card,
            onSurface = ios.label,
            onSurfaceVariant = ios.secondary,
            outline = ios.separator,
            error = ios.red,
            surfaceContainerHigh = ios.elevated,
            surfaceContainer = ios.card
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalIos provides ios) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

@Composable
fun rememberResumeTick(): Int {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val activity = context as? ComponentActivity
        if (activity == null) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }
    return tick
}

@Composable
fun rememberTap(): () -> Unit {
    val view = LocalView.current
    return { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) }
}

@Composable
fun LargeTitlePage(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    bottom: Dp = 28.dp,
    content: LazyListScope.() -> Unit
) {
    val ios = LocalIos.current
    val list = rememberLazyListState()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val fraction by remember {
        derivedStateOf {
            if (list.firstVisibleItemIndex > 0) 1f
            else (list.firstVisibleItemScrollOffset / 48f).coerceIn(0f, 1f)
        }
    }
    Column(Modifier.fillMaxSize().background(ios.groupedBg)) {
        Box(Modifier.fillMaxWidth().height(top + 44.dp).background(ios.groupedBg)) {
            Text(
                title,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp, start = 88.dp, end = 88.dp)
                    .fillMaxWidth()
                    .graphicsLayer { alpha = fraction },
                color = ios.label,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailing != null) {
                Box(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 6.dp)) {
                    trailing()
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .graphicsLayer { alpha = fraction }
                    .background(ios.separator)
            )
        }
        LazyColumn(
            state = list,
            modifier = Modifier.stretchOverscroll().fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottom)
        ) {
            item {
                Text(
                    title,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    color = ios.label,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp
                )
            }
            content()
        }
    }
}

@Composable
fun NavBar(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    val ios = LocalIos.current
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val tap = rememberTap()
    Column(Modifier.fillMaxWidth().background(ios.groupedBg)) {
        Box(Modifier.fillMaxWidth().padding(top = top).height(44.dp)) {
            Row(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .clickable {
                        tap()
                        onBack()
                    }
                    .padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ios.blue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(t("Back"), color = ios.blue, fontSize = 17.sp, lineHeight = 22.sp)
            }
            Text(
                title,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 96.dp),
                color = ios.label,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )
            if (trailing != null) {
                Box(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) { trailing() }
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(ios.separator))
    }
}

@Composable
fun TabBar(selected: Tab, unread: Int, onSelect: (Tab) -> Unit) {
    val ios = LocalIos.current
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val tap = rememberTap()
    Column(Modifier.fillMaxWidth().background(ios.tabBar)) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(ios.separator))
        Box(Modifier.fillMaxWidth().padding(bottom = bottom)) {
            Row(Modifier.fillMaxWidth().height(50.dp)) {
                    Tab.entries.forEach { tab ->
                        val on = tab == selected
                        val press = remember(tab) { MutableInteractionSource() }
                        val pressed by press.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            if (pressed) 0.92f else 1f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "tap"
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .height(50.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clickable(interactionSource = press, indication = null) {
                                    tap()
                                    onSelect(tab)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (on) ios.blue else ios.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                if (tab == Tab.Alerts && unread > 0) {
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 7.dp, y = (-5).dp)
                                            .height(16.dp)
                                            .widthIn(min = 16.dp)
                                            .background(ios.red, CircleShape)
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (unread > 9) "9+" else unread.toString(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                t(tab.label),
                                color = if (on) ios.blue else ios.secondary,
                                fontSize = 10.sp,
                                fontWeight = if (on) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }

fun Modifier.stretchOverscroll(): Modifier = composed {
    var pull by remember { mutableFloatStateOf(0f) }
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
                pull += available.y * 0.12f
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pull == 0f) return Velocity.Zero
                val start = pull
                Animatable(start).animateTo(
                    0f,
                    spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                ) { pull = value }
                pull = 0f
                return available
            }
        }
    }
    this
        .clipToBounds()
        .nestedScroll(connection)
        .graphicsLayer {
            val damp = pull.coerceIn(-24f, 24f)
            translationY = damp * 0.2f
            scaleY = 1f + (kotlin.math.abs(damp) / size.height.coerceAtLeast(1f)) * 0.06f
            transformOrigin = TransformOrigin(0.5f, if (pull >= 0f) 0f else 1f)
        }
}

@Composable
fun AddFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ios = LocalIos.current
    val tap = rememberTap()
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.92f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "add"
    )
    Box(
        modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(ios.blue)
            .clickable(interactionSource = press, indication = null) {
                tap()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Add, contentDescription = t("Add an entry"), tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun PlusButton(onClick: () -> Unit) {
    val ios = LocalIos.current
    val tap = rememberTap()
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(ios.blue)
            .clickable {
                tap()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun FastSheet(
    onDismiss: () -> Unit,
    container: Color = LocalIos.current.groupedBg,
    handle: Boolean = true,
    sheetContent: @Composable ColumnScope.() -> Unit
) {
    val ios = LocalIos.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val shift = remember { Animatable(2200f) }
    val pull = remember { mutableFloatStateOf(0f) }
    var closing by remember { mutableStateOf(false) }
    val threshold = with(density) { 120.dp.toPx() }
    fun closeSheet() {
        if (closing) return
        closing = true
        scope.launch {
            try {
                val from = shift.value + pull.floatValue
                pull.floatValue = 0f
                shift.snapTo(from)
                shift.animateTo(2200f, tween(150, easing = FastOutSlowInEasing))
            } finally {
                onDismiss()
            }
        }
    }
    LaunchedEffect(Unit) {
        shift.animateTo(0f, tween(160, easing = FastOutSlowInEasing))
    }
    BackHandler(enabled = !closing) { closeSheet() }
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y >= 0f || pull.floatValue <= 0f) {
                    return Offset.Zero
                }
                val used = available.y.coerceAtLeast(-pull.floatValue)
                pull.floatValue += used
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                pull.floatValue += available.y
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val dragged = pull.floatValue
                if (dragged <= 0f) return Velocity.Zero
                pull.floatValue = 0f
                shift.snapTo(shift.value + dragged)
                if (shift.value > threshold) closeSheet() else shift.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
                return available
            }
        }
    }
    val travel = (shift.value + pull.floatValue).coerceAtLeast(0f)
    val scrim = (1f - travel / 700f).coerceIn(0f, 1f)
    CompositionLocalProvider(LocalDismissSheet provides { closeSheet() }) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = scrim }
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { closeSheet() }
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset { IntOffset(0, travel.roundToInt()) }
                    .nestedScroll(connection)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(container)
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .pointerInput(threshold) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { _, dy ->
                                    pull.floatValue = (pull.floatValue + dy).coerceAtLeast(-shift.value)
                                },
                                onDragEnd = {
                                    val dragged = pull.floatValue
                                    pull.floatValue = 0f
                                    scope.launch {
                                        shift.snapTo((shift.value + dragged).coerceAtLeast(0f))
                                        if (shift.value > threshold) closeSheet()
                                        else shift.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
                                    }
                                },
                                onDragCancel = {
                                    val dragged = pull.floatValue
                                    pull.floatValue = 0f
                                    scope.launch {
                                        shift.snapTo((shift.value + dragged).coerceAtLeast(0f))
                                        shift.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
                                    }
                                }
                            )
                        }
                        .padding(top = 8.dp, bottom = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (handle) {
                        Box(
                            Modifier
                                .width(36.dp)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(ios.tertiary)
                        )
                    } else {
                        Spacer(Modifier.height(6.dp))
                    }
                }
                sheetContent()
            }
        }
    }
}

@Composable
fun Group(color: Color = LocalIos.current.card, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color
    ) {
        Column(content = content)
    }
}

@Composable
fun SectionLabel(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val ios = LocalIos.current
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 22.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text.uppercase(),
            modifier = Modifier.weight(1f),
            color = ios.secondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp
        )
        if (action != null && onAction != null) {
            Text(
                action,
                color = ios.blue,
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun SectionFooter(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
        color = LocalIos.current.secondary,
        fontSize = 13.sp
    )
}

@Composable
fun Hairline(start: Dp = 16.dp) {
    Box(
        Modifier
            .padding(start = start)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(LocalIos.current.separator)
    )
}

@Composable
fun Avatar(name: String, size: Dp) {
    val letter = name.trim().take(1).uppercase().ifEmpty { "?" }
    Box(
        Modifier.size(size).clip(CircleShape).background(avatarColor(name)),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.42f).sp)
    }
}

@Composable
fun IosSearch(value: String, onValue: (String) -> Unit, placeholder: String = "Search") {
    val ios = LocalIos.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val open = focused || value.isNotEmpty()
    val height by animateDpAsState(
        if (open) 44.dp else 36.dp,
        tween(220, easing = FastOutSlowInEasing),
        label = "searchH"
    )
    val radius by animateDpAsState(if (focused) 14.dp else 10.dp, tween(220), label = "searchR")
    val lift by animateDpAsState(if (focused) 6.dp else 0.dp, tween(220), label = "searchS")
    val icon by animateDpAsState(if (focused) 20.dp else 18.dp, tween(220), label = "searchI")
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(height)
            .shadow(lift, RoundedCornerShape(radius))
            .clip(RoundedCornerShape(radius))
            .background(if (focused) ios.card else ios.fill)
            .border(
                if (focused) 1.5.dp else 0.dp,
                ios.blue.copy(alpha = if (focused) 0.85f else 0f),
                RoundedCornerShape(radius)
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = if (focused) ios.blue else ios.secondary,
            modifier = Modifier.size(icon)
        )
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, color = ios.secondary, fontSize = 17.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                textStyle = TextStyle(color = ios.label, fontSize = 17.sp),
                cursorBrush = SolidColor(ios.blue),
                interactionSource = interaction,
                modifier = Modifier.fillMaxWidth()
            )
        }
        AnimatedVisibility(visible = value.isNotEmpty(), enter = fadeIn(tween(140)), exit = fadeOut(tween(100))) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Clear",
                tint = ios.secondary,
                modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onValue("") }
            )
        }
    }
}

@Composable
fun IosSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val ios = LocalIos.current
    val tap = rememberTap()
    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(9.dp))
            .background(ios.fill)
            .padding(2.dp)
            .height(32.dp)
    ) {
        val count = options.size.coerceAtLeast(1)
        val segment = maxWidth / count
        val x by animateDpAsState(
            segment * selected.coerceIn(0, count - 1),
            spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium),
            label = "segment"
        )
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .fillMaxHeight()
                .background(ios.thumb, RoundedCornerShape(7.dp))
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { index, label ->
                val on = index == selected
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            tap()
                            onSelect(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = ios.label,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun IosChips(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val ios = LocalIos.current
    val tap = rememberTap()
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (id, label) ->
            val on = id == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) ios.blue else ios.fill)
                    .clickable {
                        tap()
                        onSelect(id)
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    label,
                    color = if (on) Color.White else ios.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun NavRow(
    title: String,
    value: String? = null,
    subtitle: String? = null,
    titleColor: Color = LocalIos.current.label,
    leading: (@Composable () -> Unit)? = null,
    chevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val ios = LocalIos.current
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Box(Modifier.padding(end = 12.dp)) { leading() }
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = titleColor, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = ios.secondary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (value != null) {
            Text(
                value,
                modifier = Modifier.padding(start = 8.dp),
                color = ios.secondary,
                fontSize = 17.sp,
                maxLines = 1
            )
        }
        if (chevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = ios.tertiary,
                modifier = Modifier.padding(start = 2.dp).size(20.dp)
            )
        }
    }
}

@Composable
fun TintIcon(icon: ImageVector, background: Color) {
    Box(
        Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun CheckRow(title: String, selected: Boolean, onClick: () -> Unit) {
    val ios = LocalIos.current
    val tap = rememberTap()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                tap()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = if (selected) ios.blue else ios.label,
            fontSize = 17.sp
        )
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = ios.blue, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, on: Boolean, onChange: (Boolean) -> Unit) {
    val ios = LocalIos.current
    val view = LocalView.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onChange(!on)
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = ios.label, fontSize = 17.sp)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = ios.secondary, fontSize = 13.sp)
            }
        }
        IosSwitch(on)
    }
}

@Composable
fun IosSwitch(on: Boolean) {
    val ios = LocalIos.current
    val knob by animateDpAsState(if (on) 22.dp else 2.dp, tween(180), label = "knob")
    Box(
        Modifier
            .width(51.dp)
            .height(31.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (on) ios.green else ios.switchOff)
    ) {
        Box(
            Modifier
                .padding(start = knob, top = 2.dp)
                .size(27.dp)
                .shadow(1.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun InputRow(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    keyboard: KeyboardOptions = KeyboardOptions.Default
) {
    val ios = LocalIos.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.width(84.dp), color = ios.label, fontSize = 17.sp)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, color = ios.tertiary, fontSize = 17.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                textStyle = TextStyle(color = ios.label, fontSize = 17.sp),
                keyboardOptions = keyboard,
                cursorBrush = SolidColor(ios.blue),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = LocalIos.current.blue) {
    val tap = rememberTap()
    Box(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable {
                tap()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun BannerHost(banner: Banner?, onDismiss: () -> Unit, onClick: (Banner) -> Unit) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var shown by remember { mutableStateOf<Banner?>(null) }
    SideEffect {
        if (banner != null && shown != banner) shown = banner
    }
    val draw = banner ?: shown
    LaunchedEffect(banner?.id) {
        if (banner != null) {
            delay(3600)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = banner != null,
        enter = slideInVertically(tween(280)) { -it } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(220)) { -it } + fadeOut(tween(180)),
        modifier = Modifier.padding(top = top + 6.dp, start = 10.dp, end = 10.dp)
    ) {
        val item = draw ?: return@AnimatedVisibility
        BannerCard(item) {
            onClick(item)
            onDismiss()
        }
    }
}

@Composable
private fun BannerCard(banner: Banner, onClick: () -> Unit) {
    val ios = LocalIos.current
    val tint = when (banner.kind) {
        "overdue" -> ios.red
        "due", "soon" -> ios.orange
        "payment", "returned", "added" -> ios.green
        else -> ios.blue
    }
    val icon = when (banner.kind) {
        "overdue" -> Icons.Filled.Warning
        "payment", "returned", "added" -> Icons.Filled.Check
        else -> Icons.Filled.Notifications
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, ios.separator, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = ios.elevated,
        shadowElevation = 8.dp
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(tint),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(banner.title, color = ios.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(banner.body, color = ios.secondary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("now", color = ios.tertiary, fontSize = 12.sp)
        }
    }
}

@Composable
fun EmptyBlock(title: String, body: String, action: String? = null, onAction: (() -> Unit)? = null, symbol: String? = "₹") {
    val ios = LocalIos.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (symbol != null) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(ios.fill),
                contentAlignment = Alignment.Center
            ) {
                Text(symbol, color = ios.blue, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))
        }
        Text(title, color = ios.label, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, color = ios.secondary, fontSize = 15.sp, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            PrimaryButton(action, onAction, Modifier.fillMaxWidth())
        }
    }
}
