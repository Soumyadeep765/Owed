package com.soumya.owed

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.launch
import java.util.UUID

class OwedVm(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    var items by mutableStateOf(store.load())
        private set

    init {
        items.forEach { Reminders.schedule(app, it) }
    }

    private fun commit(l: List<Entry>) {
        items = l
        store.save(l)
        l.forEach { Reminders.schedule(getApplication(), it) }
    }

    fun add(e: Entry) = commit(items + e)

    fun update(e: Entry) = commit(items.map { if (it.id == e.id) e else it })

    fun delete(e: Entry) {
        Reminders.cancel(getApplication(), e)
        commit(items.filter { it.id != e.id })
    }
}

class MainActivity : ComponentActivity() {
    private val vm: OwedVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { OwedTheme { App(vm) } }
    }
}

@Composable
fun OwedTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFF5B8CFF),
            onPrimary = Color(0xFF0F1114),
            background = Color(0xFF0F1114),
            surface = Color(0xFF171A1F),
            onSurface = Color(0xFFECEEF1),
            onSurfaceVariant = Color(0xFF8B93A1),
            outlineVariant = Color(0xFF262B33),
            error = Color(0xFFFF6B6B),
            surfaceContainerHigh = Color(0xFF171A1F)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1F5EFF),
            onPrimary = Color.White,
            background = Color(0xFFF6F7F9),
            surface = Color.White,
            onSurface = Color(0xFF14171C),
            onSurfaceVariant = Color(0xFF6B7280),
            outlineVariant = Color(0xFFE5E7EB),
            error = Color(0xFFC42B2B),
            surfaceContainerHigh = Color.White
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun statusOf(e: Entry): Pair<String, Color> {
    val dark = isSystemInDarkTheme()
    val ok = if (dark) Color(0xFF3ECF9A) else Color(0xFF12805C)
    val warn = if (dark) Color(0xFFF0A64A) else Color(0xFFB45309)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    if (e.returned) return "Returned" to ok
    val due = e.due ?: return "No return date" to muted
    val d = daysBetween(todayMs(), due)
    return when {
        d < 0 -> "Overdue by ${-d} ${if (d == -1L) "day" else "days"}" to MaterialTheme.colorScheme.error
        d == 0L -> "Due today" to warn
        d <= 3 -> "Due in $d ${if (d == 1L) "day" else "days"}" to warn
        else -> "Due in $d days" to muted
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(vm: OwedVm) {
    val cs = MaterialTheme.colorScheme
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) perm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    var tab by remember { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    val active = vm.items.filter { !it.returned }.sortedBy { it.due ?: Long.MAX_VALUE }
    val done = vm.items.filter { it.returned }.sortedByDescending { it.returnedAt ?: 0L }
    val list = if (tab == 0) active else done
    val receive = active.filter { it.isMoney }.sumOf { owing(it) }
    val accrued = active.sumOf { interest(it) }
    val over = active.count { e -> e.due?.let { it < todayMs() } == true }

    Scaffold(
        containerColor = cs.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                containerColor = cs.primary,
                contentColor = cs.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add") }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = pad.calculateTopPadding() + 16.dp,
                bottom = pad.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column {
                    Text("Owed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Track money and things you’ve lent",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat("To receive", receive, true, Modifier.weight(1f))
                    Stat("Interest", accrued, true, Modifier.weight(1f))
                    Stat("Overdue", over.toDouble(), false, Modifier.weight(1f))
                }
            }
            item {
                AnimatedVisibility(
                    visible = over > 0,
                    enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = cs.surface,
                        border = BorderStroke(1.dp, cs.error)
                    ) {
                        Text(
                            "$over ${if (over == 1) "entry is" else "entries are"} past the return date.",
                            Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.error
                        )
                    }
                }
            }
            item {
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = Color.Transparent,
                    contentColor = cs.onSurface,
                    divider = {}
                ) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Active") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Returned") })
                }
            }
            if (list.isEmpty()) {
                item {
                    Text(
                        if (tab == 0) "Nothing lent right now.\nTap Add to start." else "No returned entries yet.",
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        textAlign = TextAlign.Center,
                        color = cs.onSurfaceVariant
                    )
                }
            }
            items(list, key = { it.id }) { e ->
                EntryCard(
                    e,
                    Modifier.animateItem(),
                    onUpdate = { vm.update(it) },
                    onDelete = { vm.delete(e) }
                )
            }
        }
    }

    if (adding) {
        AddSheet(onDismiss = { adding = false }, onSave = { vm.add(it); tab = 0 })
    }
}

@Composable
fun Stat(label: String, value: Double, money: Boolean, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val v by animateFloatAsState(value.toFloat(), tween(500), label = "stat")
    Surface(
        modifier,
        shape = RoundedCornerShape(10.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                if (money) inr(v.toDouble()) else v.toInt().toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryCard(e: Entry, modifier: Modifier, onUpdate: (Entry) -> Unit, onDelete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var dlg by remember { mutableIntStateOf(0) }
    val (label, color) = statusOf(e)
    val lines = buildList {
        add(if (e.isMoney) "Lent ${inr(e.amount)} on ${fmt(e.date)}" else "Given on ${fmt(e.date)}")
        if (e.isMoney && interest(e) > 0) add("Interest ${inr(interest(e))} at ${e.rate}% / month")
        if (e.isMoney && e.paid > 0) add("Received ${inr(e.paid)}")
        e.due?.let { add("Return by ${fmt(it)}") }
        if (e.note.isNotBlank()) add(e.note)
    }

    Surface(
        modifier.animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Medium)
                }
                Text(
                    if (e.isMoney) inr(owing(e)) else e.desc,
                    Modifier.widthIn(max = 160.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(lines.joinToString("\n"), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!e.returned) {
                    if (e.isMoney) OutlinedButton({ dlg = 1 }) { Text("Payment") }
                    Button({ onUpdate(e.copy(returned = true, returnedAt = todayMs())) }) { Text("Returned") }
                    OutlinedButton({
                        val msg = "Hi ${e.name}, a gentle reminder about " +
                            (if (e.isMoney) inr(owing(e)) else e.desc) +
                            (e.due?.let { " (due ${fmt(it)})" } ?: "") + ". Thank you!"
                        ctx.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, msg),
                                null
                            )
                        )
                    }) { Text("Remind") }
                } else {
                    OutlinedButton({ onUpdate(e.copy(returned = false, returnedAt = null)) }) { Text("Reopen") }
                }
                TextButton({ dlg = 2 }, colors = ButtonDefaults.textButtonColors(contentColor = cs.error)) {
                    Text("Delete")
                }
            }
        }
    }

    if (dlg == 1) {
        var v by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dlg = 0 },
            title = { Text("Payment received") },
            text = {
                OutlinedTextField(
                    v,
                    { v = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            },
            confirmButton = {
                TextButton({
                    val a = v.toDoubleOrNull()
                    if (a != null && a > 0) {
                        val u = e.copy(paid = e.paid + a)
                        onUpdate(if (owing(u) <= 0.5) u.copy(returned = true, returnedAt = todayMs()) else u)
                    }
                    dlg = 0
                }) { Text("Save") }
            },
            dismissButton = { TextButton({ dlg = 0 }) { Text("Cancel") } }
        )
    }

    if (dlg == 2) {
        AlertDialog(
            onDismissRequest = { dlg = 0 },
            title = { Text("Delete entry?") },
            text = { Text("This removes ${e.name}’s entry permanently.") },
            confirmButton = {
                TextButton({ dlg = 0; onDelete() }, colors = ButtonDefaults.textButtonColors(contentColor = cs.error)) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton({ dlg = 0 }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSheet(onDismiss: () -> Unit, onSave: (Entry) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var money by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(todayMs()) }
    var due by remember { mutableStateOf<Long?>(null) }
    var picker by remember { mutableIntStateOf(0) }
    var err by remember { mutableStateOf("") }
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = cs.surface) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("New entry", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = money,
                    onClick = { money = true },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Money") }
                SegmentedButton(
                    selected = !money,
                    onClick = { money = false },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Item") }
            }
            OutlinedTextField(
                name,
                { name = it },
                Modifier.fillMaxWidth(),
                label = { Text("Person") },
                singleLine = true
            )
            AnimatedVisibility(money, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        amount,
                        { amount = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Amount (₹)") },
                        singleLine = true,
                        keyboardOptions = decimal
                    )
                    OutlinedTextField(
                        rate,
                        { rate = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Interest % per month (optional)") },
                        singleLine = true,
                        keyboardOptions = decimal
                    )
                }
            }
            AnimatedVisibility(!money, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                OutlinedTextField(
                    desc,
                    { desc = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Item") },
                    singleLine = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ picker = 1 }, Modifier.weight(1f)) { Text("Given: ${fmt(date)}", maxLines = 1) }
                OutlinedButton({ picker = 2 }, Modifier.weight(1f)) {
                    Text("Return: ${due?.let { fmt(it) } ?: "Set date"}", maxLines = 1)
                }
            }
            OutlinedTextField(
                note,
                { note = it },
                Modifier.fillMaxWidth(),
                label = { Text("Note (optional)") },
                singleLine = true
            )
            Text(err, color = cs.error, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    { scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() } },
                    Modifier.weight(1f)
                ) { Text("Cancel") }
                Button({
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    val dueV = due
                    when {
                        name.isBlank() -> err = "Enter a name."
                        money && amt <= 0 -> err = "Enter a valid amount."
                        !money && desc.isBlank() -> err = "Enter the item."
                        dueV != null && dueV < date -> err = "Return date is before the given date."
                        else -> {
                            onSave(
                                Entry(
                                    id = UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    isMoney = money,
                                    amount = if (money) amt else 0.0,
                                    desc = desc.trim(),
                                    date = date,
                                    due = dueV,
                                    rate = if (money) (rate.toDoubleOrNull() ?: 0.0) else 0.0,
                                    note = note.trim()
                                )
                            )
                            scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
                        }
                    }
                }, Modifier.weight(1f)) { Text("Save") }
            }
        }
    }

    if (picker != 0) {
        val st = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { picker = 0 },
            confirmButton = {
                TextButton({
                    st.selectedDateMillis?.let {
                        val ms = pickerToLocal(it)
                        if (picker == 1) date = ms else due = ms
                    }
                    picker = 0
                }) { Text("OK") }
            },
            dismissButton = { TextButton({ picker = 0 }) { Text("Cancel") } }
        ) { DatePicker(st) }
    }
}
