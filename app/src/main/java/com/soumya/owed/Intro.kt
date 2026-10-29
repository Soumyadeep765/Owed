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
