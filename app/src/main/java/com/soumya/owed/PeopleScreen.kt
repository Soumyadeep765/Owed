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
fun PeopleScreen(vm: OwedVm, onOpen: (Person) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val people = peopleOf(vm.items).filter { person ->
        query.isBlank() || person.name.contains(query, true) || person.phone.contains(query)
    }
    LargeTitlePage(t("People"), bottom = 108.dp) {
        item { IosSearch(query, { query = it }, t("Search people")) }
        item { Spacer(Modifier.height(12.dp)) }
        if (people.isEmpty()) {
            item {
                if (vm.items.isEmpty()) {
                    EmptyBlock(
                        t("No one yet"),
                        t("People are grouped from the names on your entries."),
                        symbol = null
                    )
                } else {
                    Text(
                        t("No matches."),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        color = LocalIos.current.secondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item {
                Group {
                    people.forEachIndexed { index, person ->
                        PersonRow(person) { onOpen(person) }
                        if (index != people.lastIndex) Hairline(66.dp)
                    }
                }
            }
            item { SectionFooter("Balances count open money only. Items are listed on the person.") }
        }
    }
}

@Composable
fun PersonRow(person: Person, onClick: () -> Unit) {
    val ios = LocalIos.current
    val subtitle = when {
        person.open.isEmpty() -> t("All settled")
        person.net > 0.5 -> t("Owes you · %d open", person.open.size)
        person.net < -0.5 -> t("You owe · %d open", person.open.size)
        else -> t("%d open", person.open.size)
    }
    val value = when {
        person.net > 0.5 -> inr(person.net)
        person.net < -0.5 -> inr(-person.net)
        else -> ""
    }
    val valueColor = when {
        person.net > 0.5 -> ios.green
        person.net < -0.5 -> ios.orange
        else -> ios.secondary
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(person.name, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(person.name, color = ios.label, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = ios.secondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (value.isNotEmpty()) {
            Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        androidx.compose.material3.Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ios.tertiary,
            modifier = Modifier.padding(start = 2.dp).size(20.dp)
        )
    }
}
