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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
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
        Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = bottom)) {
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
