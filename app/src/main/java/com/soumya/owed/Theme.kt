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
