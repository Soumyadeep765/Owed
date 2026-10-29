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
                SectionLabel(t("Contact"))
                Group {
                    NavRow(t("Call"), value = p.phone, chevron = true, onClick = {
                        try {
                            dial(ctx, p.phone)
                        } catch (_: ActivityNotFoundException) {
                            vm.flash(t("Can’t call"), t("No phone app is available."), "info", null)
                        }
                    })
                    Hairline()
                    NavRow(t("Text"), chevron = true, onClick = {
                        try {
                            sms(ctx, p.phone, personMessage(p))
                        } catch (_: ActivityNotFoundException) {
                            vm.flash(t("No messages app"), t("Nothing on this phone can send a text."), "info", null)
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

private fun personMessage(person: Person): String {
    val open = person.open
    if (open.size == 1) return reminderText(open.first())
    if (open.isEmpty()) return t("Hi %s, thanks — everything between us is settled.", person.name)
    val lines = open.joinToString("\n") { entry ->
        val what = if (entry.isMoney) inr(owing(entry)) else entry.desc.ifBlank { t("an item") }
        "• $what"
    }
    return t("Hi %1\$s, a gentle reminder about:\n%2\$s\nThank you!", person.name, lines)
}
