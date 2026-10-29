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
                    if (!e.returned && e.isMoney) add(Triple(t("Record payment"), null as String?, onPay))
                    if (!e.returned) {
                        add(Triple(t("Mark returned"), null, { vm.markReturned(e.id) }))
                        add(Triple(t("Share reminder"), null, {
                            try {
                                shareText(ctx, reminderText(e))
                            } catch (_: ActivityNotFoundException) {
                                vm.flash(t("Couldn’t share"), t("No app accepted the reminder."), "info", e.id)
                            }
                        }))
                        if (e.phone.isNotBlank()) {
                            add(Triple(t("Text %s", e.name), null, {
                                try {
                                    sms(ctx, e.phone, reminderText(e))
                                } catch (_: ActivityNotFoundException) {
                                    vm.flash(t("No messages app"), t("Share the reminder instead."), "info", e.id)
                                }
                            }))
                        }
                    } else {
                        add(Triple(t("Reopen"), null, { vm.reopen(e.id) }))
                    }
                    if (e.phone.isNotBlank()) add(Triple(t("Call"), e.phone, {
                        try {
                            dial(ctx, e.phone)
                        } catch (_: ActivityNotFoundException) {
                            vm.flash(t("Can’t call"), t("No phone app is available."), "info", e.id)
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
                    if (e.phone.isNotBlank()) add(t("Phone") to e.phone)
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
