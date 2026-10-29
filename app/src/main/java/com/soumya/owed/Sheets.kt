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
fun EditorSheet(
    existing: Entry?,
    presetName: String,
    presetLent: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (Entry) -> Unit
) {
    val ios = LocalIos.current
    val ctx = LocalContext.current
    var lent by remember { mutableStateOf(if (existing != null) existing.direction == LENT else presetLent) }
    var money by remember { mutableStateOf(existing?.isMoney ?: true) }
    var name by remember { mutableStateOf(existing?.name ?: presetName) }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var amount by remember {
        mutableStateOf(
            if (existing != null && existing.isMoney && existing.amount > 0) trimNum(existing.amount) else ""
        )
    }
    var desc by remember { mutableStateOf(existing?.desc ?: "") }
    var rate by remember {
        mutableStateOf(existing?.rate?.takeIf { it > 0 }?.let { trimNum(it) }.orEmpty())
    }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var date by remember { mutableLongStateOf(existing?.date ?: todayMs()) }
    var due by remember { mutableStateOf(existing?.due) }
    var picker by remember { mutableStateOf(0) }
    var err by remember { mutableStateOf("") }
    var closing by remember { mutableStateOf(false) }
    val dismisser = remember { mutableStateOf<() -> Unit>({ onDismiss() }) }
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    fun readContact(uri: Uri) {
        val found = try {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val picked = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
            val id = if (idIdx >= 0) cursor.getString(idIdx) else null
            var number = ""
            if (!id.isNullOrBlank() && ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                ctx.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )?.use { phones ->
                    if (phones.moveToFirst()) number = phones.getString(0).orEmpty()
                }
            }
            picked to number
        }
        } catch (_: Exception) {
            null
        }
        if (found == null || found.first.isBlank()) {
            err = t("Allow contacts to fill in the name.")
        } else {
            name = found.first
            if (found.second.isNotBlank()) phone = found.second
            err = ""
        }
    }
    val pickContact = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) readContact(uri)
    }
    val askContacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pickContact.launch(null) else err = t("Allow contacts to fill in the name.")
    }
    fun chooseContact() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            pickContact.launch(null)
        } else {
            askContacts.launch(Manifest.permission.READ_CONTACTS)
        }
    }
    fun close(save: Entry?) {
        if (closing) return
        closing = true
        if (save != null) onSave(save)
        dismisser.value()
    }
    FastSheet(onDismiss = onDismiss) {
        val hide = LocalDismissSheet.current
        SideEffect { dismisser.value = hide }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            Text(
                if (existing == null) t("New") else t("Edit"),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = ios.label,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )
            Spacer(Modifier.height(8.dp))
            IosSegmented(
                listOf(t("I lent"), t("I borrowed")),
                selected = if (lent) 0 else 1,
                onSelect = { lent = it == 0 },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            IosSegmented(
                listOf(t("Money"), t("Item")),
                selected = if (money) 0 else 1,
                onSelect = { money = it == 0 },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(16.dp))
            Group {
                InputRow("Person", name, { name = it }, "Name")
                Hairline()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { chooseContact() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(t("Contacts"), color = ios.blue, fontSize = 17.sp)
                }
                Hairline()
                InputRow(t("Phone"), phone, { phone = it }, t("Optional"), KeyboardOptions(keyboardType = KeyboardType.Phone))
            }
            AnimatedVisibility(money, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Group {
                        InputRow("Amount", amount, { amount = it }, "0", decimal)
                        Hairline()
                        InputRow("Rate", rate, { rate = it }, "% / month", decimal)
                    }
                }
            }
            AnimatedVisibility(!money, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Group {
                        InputRow("Item", desc, { desc = it }, "What was given")
                    }
                }
            }
            SectionLabel("When")
            Group {
                Row(
                    Modifier.fillMaxWidth().clickable { picker = 1 }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Given", modifier = Modifier.width(84.dp), color = ios.label, fontSize = 17.sp)
                    Text(fmt(date), color = ios.blue, fontSize = 17.sp)
                }
                Hairline()
                Row(
                    Modifier.fillMaxWidth().clickable { picker = 2 }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Return", modifier = Modifier.width(84.dp), color = ios.label, fontSize = 17.sp)
                    Text(
                        due?.let { fmt(it) } ?: "Add date",
                        modifier = Modifier.weight(1f),
                        color = if (due == null) ios.tertiary else ios.blue,
                        fontSize = 17.sp
                    )
                    if (due != null) {
                        Text(
                            "Clear",
                            color = ios.red,
                            fontSize = 17.sp,
                            modifier = Modifier.clickable { due = null }.padding(start = 8.dp)
                        )
                    }
                }
            }
            SectionLabel("Note")
            Group {
                InputRow("Note", note, { note = it }, t("Optional"))
            }
            if (err.isNotBlank()) {
                Text(
                    err,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = ios.red,
                    fontSize = 13.sp
                )
            } else {
                Spacer(Modifier.height(12.dp))
            }
            PrimaryButton(
                if (existing == null) t("Add an entry") else t("Save"),
                {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    val dueValue = due
                    when {
                        name.isBlank() -> err = "Enter a name."
                        money && amt <= 0 -> err = "Enter an amount."
                        !money && desc.isBlank() -> err = "Enter the item."
                        dueValue != null && dueValue < date -> err = "Return date is before the given date."
                        else -> {
                            val base = existing
                            close(
                                Entry(
                                    id = base?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    isMoney = money,
                                    amount = if (money) amt else 0.0,
                                    desc = if (money) "" else desc.trim(),
                                    date = date,
                                    due = dueValue,
                                    rate = if (money) (rate.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0) else 0.0,
                                    note = note.trim(),
                                    paid = base?.paid ?: 0.0,
                                    returned = base?.returned ?: false,
                                    returnedAt = base?.returnedAt,
                                    direction = if (lent) LENT else BORROWED,
                                    phone = phone.trim(),
                                    payments = base?.payments ?: emptyList()
                                )
                            )
                        }
                    }
                },
                Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clickable { close(null) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(t("Cancel"), color = ios.blue, fontSize = 17.sp)
            }
        }
    }
    if (picker != 0) {
        val initial = if (picker == 1) localToPicker(date) else due?.let { localToPicker(it) } ?: localToPicker(date)
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { picker = 0 },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { picked ->
                        val day = pickerToLocal(picked)
                        if (picker == 1) date = day else due = day
                    }
                    picker = 0
                }) { Text("Done") }
            },
            dismissButton = { TextButton({ picker = 0 }) { Text(t("Cancel")) } }
        ) { DatePicker(state) }
    }
}

@Composable
fun PaymentSheet(entry: Entry, onDismiss: () -> Unit, onSave: (Double, String) -> Unit) {
    val ios = LocalIos.current
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    var closing by remember { mutableStateOf(false) }
    val dismisser = remember { mutableStateOf<() -> Unit>({ onDismiss() }) }
    val typed = amount.toDoubleOrNull() ?: 0.0
    val left = (owing(entry) - typed).coerceAtLeast(0.0)
    fun close(save: Pair<Double, String>?) {
        if (closing) return
        closing = true
        if (save != null) onSave(save.first, save.second)
        dismisser.value()
    }
    FastSheet(onDismiss = onDismiss) {
        val hide = LocalDismissSheet.current
        SideEffect { dismisser.value = hide }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            Text(
                t("Payment"),
                modifier = Modifier.padding(horizontal = 16.dp),
                color = ios.label,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Group {
                InputRow("Amount", amount, { amount = it }, "0", KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Hairline()
                InputRow("Note", note, { note = it }, t("Optional"))
            }
            SectionFooter(
                when {
                    typed <= 0 -> "Still open ${inr(owing(entry))}"
                    left <= 0.5 -> "This settles the entry."
                    else -> "Left after this ${inr(left)}"
                }
            )
            if (err.isNotBlank()) {
                Text(err, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = ios.red, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                t("Save"),
                {
                    val value = amount.toDoubleOrNull()
                    if (value == null || value <= 0) err = "Enter an amount."
                    else close(value to note)
                },
                Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            )
            Box(
                Modifier.fillMaxWidth().clickable { close(null) }.padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(t("Cancel"), color = ios.blue, fontSize = 17.sp)
            }
        }
    }
}

@Composable
fun DeleteSheet(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val ios = LocalIos.current
    var closing by remember { mutableStateOf(false) }
    val dismisser = remember { mutableStateOf<() -> Unit>({ onDismiss() }) }
    fun close(confirm: Boolean) {
        if (closing) return
        closing = true
        if (confirm) onConfirm()
        dismisser.value()
    }
    FastSheet(onDismiss = onDismiss, container = Color.Transparent, handle = false) {
        val hide = LocalDismissSheet.current
        SideEffect { dismisser.value = hide }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp)
                .padding(bottom = 10.dp)
        ) {
            Group {
                Column(
                    Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Delete this entry?", color = ios.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "This removes $name’s entry.",
                        color = ios.secondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Hairline(0.dp)
                Box(
                    Modifier.fillMaxWidth().clickable { close(true) }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Delete entry", color = ios.red, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Group {
                Box(
                    Modifier.fillMaxWidth().clickable { close(false) }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(t("Cancel"), color = ios.blue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

fun trimNum(value: Double): String {
    val rounded = Math.round(value * 100.0) / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString()
    else String.format(Locale.ENGLISH, "%.2f", rounded).trimEnd('0').trimEnd('.')
}
