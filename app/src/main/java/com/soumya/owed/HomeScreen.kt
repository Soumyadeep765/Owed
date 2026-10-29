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
fun HomeScreen(
    vm: OwedVm,
    onAdd: () -> Unit,
    onOpen: (Entry) -> Unit,
    onEntries: () -> Unit,
    onPeople: () -> Unit,
    onPerson: (Person) -> Unit,
    onOverdue: () -> Unit,
    onAlerts: () -> Unit
) {
    val ios = LocalIos.current
    val open = vm.items.filter { !it.returned }
    val overdue = open.filter { (it.due ?: Long.MAX_VALUE) < todayMs() }.sortedBy { it.due ?: Long.MAX_VALUE }
    val soon = open.filter { entry ->
        val due = entry.due ?: return@filter false
        daysBetween(todayMs(), due) in 0..7
    }.sortedBy { it.due ?: Long.MAX_VALUE }
    val rest = open.filter { entry ->
        overdue.none { it.id == entry.id } && soon.none { it.id == entry.id }
    }.sortedByDescending { it.date }
    val unread = vm.alerts.count { !it.read }
    val toYou = open.filter { it.isMoney && it.direction == LENT }.sumOf { owing(it) }
    val youOwe = open.filter { it.isMoney && it.direction == BORROWED }.sumOf { owing(it) }
    val net = toYou - youOwe
    val interestToYou = open.filter { it.isMoney && it.direction == LENT }.sumOf { interest(it) }
    val interestYouOwe = open.filter { it.isMoney && it.direction == BORROWED }.sumOf { interest(it) }
    val people = peopleOf(vm.items)
    val itemsOpen = open.count { !it.isMoney }
    val settled = vm.items.count { it.returned }
    val next = open.filter { it.due != null }.minByOrNull { it.due ?: Long.MAX_VALUE }
    val nextIsListed = next != null && (overdue.any { it.id == next.id } || soon.any { it.id == next.id })

    LargeTitlePage("Owed", bottom = 108.dp) {
        if (vm.items.isEmpty()) {
            item {
                EmptyBlock(
                    t("Nothing owed right now"),
                    t("Keep money and things you’ve lent or borrowed in one place."),
                    t("Add an entry"),
                    onAdd
                )
            }
        } else {
        item {
            Group {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t("Net"), color = ios.secondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        Text(vm.currency, color = ios.blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        inr(if (net < 0) -net else net),
                        color = ios.label,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.6).sp
                    )
                    Text(
                        when {
                            net > 0.5 -> t("in your favour")
                            net < -0.5 -> t("you owe more")
                            else -> t("you’re square")
                        },
                        color = when {
                            net > 0.5 -> ios.green
                            net < -0.5 -> ios.orange
                            else -> ios.secondary
                        },
                        fontSize = 15.sp
                    )
                }
                Hairline(0.dp)
                MoneyLine(t("Owed to you"), inr(toYou), ios.green)
                Hairline()
                MoneyLine(t("You owe"), inr(youOwe), ios.orange)
                if (interestToYou > 0.5) {
                    Hairline()
                    MoneyLine(t("Interest to you"), inr(interestToYou), ios.secondary)
                }
                if (interestYouOwe > 0.5) {
                    Hairline()
                    MoneyLine(t("Interest you owe"), inr(interestYouOwe), ios.secondary)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            Group {
                Row(Modifier.fillMaxWidth()) {
                    HomeStat(people.count { it.open.isNotEmpty() }.toString(), t("People"), onPeople, Modifier.weight(1f))
                    HomeStat(itemsOpen.toString(), t("Items"), onEntries, Modifier.weight(1f))
                    HomeStat(settled.toString(), t("Settled"), onEntries, Modifier.weight(1f))
                }
            }
        }
        if (next != null && !nextIsListed) {
            item { SectionLabel(t("Next"), t("See all"), onEntries) }
            item { EntryGroup(listOf(next), onOpen) }
        }
        if (unread > 0) {
            item { Spacer(Modifier.height(12.dp)) }
            item {
                Group {
                    NavRow(
                        t("Alerts"),
                        value = unread.toString(),
                        subtitle = vm.alerts.firstOrNull { !it.read }?.title,
                        leading = { TintIcon(Icons.Filled.Notifications, ios.orange) },
                        chevron = true,
                        onClick = onAlerts
                    )
                }
            }
        }
        if (overdue.isNotEmpty()) {
            item { SectionLabel(t("Overdue"), t("See all"), onOverdue) }
            item { EntryGroup(overdue.take(4), onOpen) }
        }
        if (soon.isNotEmpty()) {
            item { SectionLabel(t("Due soon"), t("See all"), onEntries) }
            item { EntryGroup(soon.take(4), onOpen) }
        }
        if (rest.isNotEmpty()) {
            item { SectionLabel(if (soon.isEmpty() && overdue.isEmpty()) t("Open") else t("Also open"), t("See all"), onEntries) }
            item { EntryGroup(rest.take(4), onOpen) }
        }
        val activePeople = people.filter { it.open.isNotEmpty() }.take(3)
        if (activePeople.isNotEmpty()) {
            item { SectionLabel(t("People"), t("See all"), onPeople) }
            item {
                Group {
                    activePeople.forEachIndexed { index, person ->
                        PersonRow(person) { onPerson(person) }
                        if (index != activePeople.lastIndex) Hairline(66.dp)
                    }
                }
            }
        }
        if (open.isEmpty()) {
            item { SectionFooter(t("Everything has been marked returned.")) }
        }
        }
    }
}

@Composable
private fun HomeStat(value: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ios = LocalIos.current
    Column(
        modifier.clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = ios.label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = ios.secondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MoneyLine(label: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), color = LocalIos.current.label, fontSize = 17.sp)
        Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}
