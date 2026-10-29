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
fun PickRow(
    title: String,
    value: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val ios = LocalIos.current
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = ios.label, fontSize = 17.sp)
            Spacer(Modifier.weight(1f))
            Text(
                value,
                color = ios.blue,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = 200.dp)
            )
        }
        AnimatedVisibility(visible = open) {
            Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                options.forEach { (id, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(id)
                                open = false
                            }
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            modifier = Modifier.weight(1f),
                            color = if (id == selected) ios.blue else ios.label,
                            fontSize = 16.sp
                        )
                        if (id == selected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = ios.blue, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
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
fun Modifier.swipeAway(key: Any, onDismiss: () -> Unit): Modifier {
    val shift = remember(key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    return this
        .graphicsLayer {
            translationX = shift.value
            val fade = kotlin.math.abs(shift.value) / 500f
            alpha = 1f - fade.coerceIn(0f, 0.55f)
        }
        .pointerInput(key) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, drag ->
                    scope.launch { shift.snapTo(shift.value + drag) }
                },
                onDragCancel = {
                    scope.launch { shift.animateTo(0f, tween(160)) }
                },
                onDragEnd = {
                    scope.launch {
                        if (kotlin.math.abs(shift.value) > 96f) {
                            val target = if (shift.value > 0f) 720f else -720f
                            shift.animateTo(target, tween(160))
                            onDismiss()
                        } else {
                            shift.animateTo(0f, tween(160))
                        }
                    }
                }
            )
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
        Box(Modifier.swipeAway(item.id) { onDismiss() }) {
            BannerCard(item) {
                onClick(item)
                onDismiss()
            }
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
fun EmptyBlock(title: String, body: String, action: String? = null, onAction: (() -> Unit)? = null, symbol: String? = "\u0000") {
    val ios = LocalIos.current
    val mark = when (symbol) {
        null -> null
        "\u0000" -> currencySymbol(LocaleHub.currency)
        else -> symbol
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (mark != null) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(ios.fill),
                contentAlignment = Alignment.Center
            ) {
                Text(mark, color = ios.blue, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
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
