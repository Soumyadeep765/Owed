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
                    t("Return reminders, payments, and settled entries show up here."),
                    symbol = "!"
                )
            }
        } else {
            item {
                Group {
                    vm.alerts.forEachIndexed { index, alert ->
                        AlertRow(alert, onDismiss = { vm.dismissAlert(alert.id) }) { onOpen(alert) }
                        if (index != vm.alerts.lastIndex) Hairline(34.dp)
                    }
                }
            }
            item {
                Text(
                    t("Clear alerts"),
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
private fun AlertRow(alert: AppAlert, onDismiss: () -> Unit, onClick: () -> Unit) {
    val ios = LocalIos.current
    Row(
        Modifier
            .swipeAway(alert.id, onDismiss)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
