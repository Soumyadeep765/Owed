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
fun EntriesScreen(
    vm: OwedVm,
    scope: String,
    chip: String,
    onScope: (String) -> Unit,
    onChip: (String) -> Unit,
    onAdd: () -> Unit,
    onOpen: (Entry) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val rows = visibleEntries(vm.items, scope, chip, query)
    val chips = buildList {
        add("all" to t("All"))
        add("money" to t("Money"))
        add("item" to t("Items"))
        if (scope == "open") add("overdue" to t("Overdue"))
    }
    LargeTitlePage(t("Entries"), bottom = 108.dp) {
        item { IosSearch(query, { query = it }, t("Search name or note")) }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            IosSegmented(
                listOf(t("Open"), t("Settled")),
                selected = if (scope == "open") 0 else 1,
                onSelect = { index ->
                    val next = if (index == 0) "open" else "done"
                    onScope(next)
                    if (next == "done" && chip == "overdue") onChip("all")
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        item { Spacer(Modifier.height(10.dp)) }
        item { IosChips(chips, chip, onChip) }
        item { Spacer(Modifier.height(12.dp)) }
        if (rows.isEmpty()) {
            item {
                if (vm.items.isEmpty()) {
                    EmptyBlock(
                        t("No entries yet"),
                        t("Add money or an item, with a return date if you want a reminder."),
                        t("Add an entry"),
                        onAdd
                    )
                } else {
                    Text(
                        t("Nothing matches."),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        color = LocalIos.current.secondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item { EntryGroup(rows, onOpen) }
        }
    }
}

private fun visibleEntries(items: List<Entry>, scope: String, chip: String, query: String): List<Entry> {
    val q = query.trim()
    return items.filter { entry ->
        val scopeOk = if (scope == "open") !entry.returned else entry.returned
        val chipOk = when (chip) {
            "money" -> entry.isMoney
            "item" -> !entry.isMoney
            "overdue" -> !entry.returned && (entry.due ?: Long.MAX_VALUE) < todayMs()
            else -> true
        }
        val queryOk = q.isBlank() ||
            entry.name.contains(q, true) ||
            entry.desc.contains(q, true) ||
            entry.note.contains(q, true) ||
            entry.phone.contains(q)
        scopeOk && chipOk && queryOk
    }.sortedWith(
        if (scope == "open") compareBy { it.due ?: Long.MAX_VALUE }
        else compareByDescending { it.returnedAt ?: 0L }
    )
}

@Composable
fun EntryGroup(entries: List<Entry>, onOpen: (Entry) -> Unit) {
    Group {
        entries.forEachIndexed { index, entry ->
            EntryRow(entry) { onOpen(entry) }
            if (index != entries.lastIndex) Hairline(66.dp)
        }
    }
}

@Composable
fun EntryRow(entry: Entry, onClick: () -> Unit) {
    val ios = LocalIos.current
    val status = statusOf(entry)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(entry.name, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.name, color = ios.label, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = ios.secondary)) { append(rowKind(entry)) }
                    withStyle(SpanStyle(color = toneColor(status.tone))) { append(" · ${status.label}") }
                },
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            if (entry.isMoney) inr(if (entry.returned) entry.amount else owing(entry)) else entry.desc.ifBlank { "Item" },
            modifier = Modifier.padding(start = 8.dp).widthIn(max = 120.dp),
            color = ios.label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        androidx.compose.material3.Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ios.tertiary,
            modifier = Modifier.padding(start = 2.dp).size(20.dp)
        )
    }
}

fun rowKind(entry: Entry): String = when {
    entry.isMoney && entry.direction == LENT -> t("Lent")
    entry.isMoney && entry.direction == BORROWED -> t("Borrowed")
    entry.direction == LENT -> t("Your item")
    else -> t("Their item")
}
