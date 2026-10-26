@file:OptIn(ExperimentalMaterial3Api::class)

package it.elia.gestionespese.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.elia.gestionespese.core.Category
import it.elia.gestionespese.core.Checked
import it.elia.gestionespese.core.Formats
import it.elia.gestionespese.core.Forms
import it.elia.gestionespese.core.Money
import it.elia.gestionespese.core.MovementKind
import it.elia.gestionespese.core.MovementText
import it.elia.gestionespese.core.Shortcut
import it.elia.gestionespese.ui.MainViewModel
import it.elia.gestionespese.ui.UiState
import it.elia.gestionespese.ui.components.Amount
import it.elia.gestionespese.ui.components.AmountField
import it.elia.gestionespese.ui.components.AppIcons
import it.elia.gestionespese.ui.components.BoxedRows
import it.elia.gestionespese.ui.components.DateField
import it.elia.gestionespese.ui.components.ErrorText
import it.elia.gestionespese.ui.components.Notice
import it.elia.gestionespese.ui.components.RowDivider
import it.elia.gestionespese.ui.components.SectionTitle
import it.elia.gestionespese.ui.components.SelectField
import it.elia.gestionespese.ui.components.SheetButtons
import it.elia.gestionespese.ui.components.SummaryRow
import it.elia.gestionespese.ui.components.TextInput
import it.elia.gestionespese.ui.theme.AppTheme
import it.elia.gestionespese.ui.theme.tabular

/** Precompilazione del modulo Spesa (es. da una spesa rapida con ✎). */
data class ExpensePrefill(val title: String, val amount: Long, val categoryId: String, val note: String)

/** Moduli aperti come bottom sheet (SPEC.md §5, Moduli). */
sealed interface Sheet {
    data object Salary : Sheet
    data object Income : Sheet
    data class Expense(val prefill: ExpensePrefill? = null) : Sheet
    data object Assign : Sheet
    data class Take(val categoryId: String) : Sheet
    data object Reset : Sheet
    data object Quick : Sheet
    data class DeleteCategory(val categoryId: String) : Sheet
}

@Composable
fun SheetHost(
    sheet: Sheet,
    st: UiState,
    vm: MainViewModel,
    onOpen: (Sheet?) -> Unit,
    onGoToSettings: () -> Unit,
) {
    val close = { onOpen(null) }
    ModalBottomSheet(
        onDismissRequest = close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (sheet) {
                Sheet.Salary -> IncomeForm(st, vm, salary = true, close)
                Sheet.Income -> IncomeForm(st, vm, salary = false, close)
                is Sheet.Expense -> ExpenseForm(st, vm, sheet.prefill, close)
                Sheet.Assign -> AssignForm(st, vm, close)
                is Sheet.Take -> st.category(sheet.categoryId)?.let { TakeForm(st, vm, it, close) }
                Sheet.Reset -> ResetForm(st, vm, close)
                Sheet.Quick -> QuickList(st, vm, onOpen, onGoToSettings)
                is Sheet.DeleteCategory -> st.category(sheet.categoryId)?.let { DeleteCategoryForm(st, vm, it, close) }
            }
        }
    }
}

// ---------- stipendio ed entrata extra ----------

@Composable
private fun IncomeForm(st: UiState, vm: MainViewModel, salary: Boolean, close: () -> Unit) {
    val cats = st.categories
    val quotas = remember { cats.associate { it.id to st.math.effectiveQuota(it, st.balance(it.id)) } }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(if (salary) st.math.proposedSalaryDate() else st.today) }
    var note by remember { mutableStateOf("") }
    val alloc: SnapshotStateMap<String, String> = remember {
        mutableStateMapOf<String, String>().apply { cats.forEach { put(it.id, if (salary) "" else "0,00") } }
    }
    var error by remember { mutableStateOf<String?>(null) }

    SectionTitle(if (salary) "Registra lo stipendio" else "Registra un'altra entrata")
    Notice(
        if (salary) "Le quote fisse vengono distribuite in ordine. Puoi ritoccarle prima di confermare."
        else "Di default resta tutto in \"Da assegnare\". Puoi già metterne una parte nei budget.",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AmountField(amount, {
            amount = it
            error = null
            if (salary) {
                // Ridistribuisce le quote in ordine a ogni modifica dell'importo (SPEC.md §4.5)
                val split = st.math.salarySplit(Money.parse(it) ?: 0, cats, st.balances)
                split.proposals.forEach { (id, v) -> alloc[id] = Money.input(v) }
            }
        }, "Importo", Modifier.weight(1f))
        DateField("Data", date, { d -> if (d != null) date = d }, Modifier.weight(1f))
    }
    if (!salary) TextInput(note, { note = it }, "Descrizione", placeholder = "es. rimborso, regalo, bonus")
    BoxedRows {
        cats.forEachIndexed { i, c ->
            if (i > 0) RowDivider()
            Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(c.name)
                    Text(quotaSubtitle(st, c, quotas[c.id] ?: 0), style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
                }
                AmountField(alloc[c.id].orEmpty(), {
                    alloc[c.id] = it
                    error = null
                }, null, Modifier.width(130.dp))
            }
        }
        RowDivider()
        SummaryRow("Resta in \"Da assegnare\"", (Money.parse(amount) ?: 0) - Forms.sumOf(alloc.values), emphasized = true)
    }
    ErrorText(error)
    SheetButtons("Conferma", onConfirm = {
        when (val r = Forms.income(amount, cats.associate { it.id to alloc[it.id].orEmpty() })) {
            is Checked.Error -> error = r.message
            is Checked.Ok -> {
                val kind = if (salary) MovementKind.STIPENDIO else MovementKind.ENTRATA
                vm.record(
                    vm.newMovement(kind, date, r.value.amount, note = if (salary) null else note, allocations = r.value.allocations),
                    if (salary) "Stipendio di ${Formats.eur(r.value.amount)} registrato"
                    else "Entrata di ${Formats.eur(r.value.amount)} registrata",
                )
                close()
            }
        }
    }, onCancel = close)
}

private fun quotaSubtitle(st: UiState, c: Category, q: Long): String = when {
    c.isManualGoal -> "obiettivo manuale · quota ${Formats.eur(q)}"
    st.math.goalPlan(c, st.balance(c.id)) != null -> "obiettivo · ${Formats.eur(q)} calcolati"
    q > 0 -> "quota ${Formats.eur(q)}"
    else -> "nessuna quota"
}

// ---------- spesa ----------

@Composable
private fun ExpenseForm(st: UiState, vm: MainViewModel, prefill: ExpensePrefill?, close: () -> Unit) {
    val cats = st.categories
    var amount by remember { mutableStateOf(prefill?.amount?.let(Money::input).orEmpty()) }
    var date by remember { mutableStateOf(st.today) }
    var catId by remember { mutableStateOf(prefill?.categoryId?.takeIf { st.category(it) != null } ?: cats.first().id) }
    var note by remember { mutableStateOf(prefill?.note.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    SectionTitle(prefill?.title ?: "Registra una spesa")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AmountField(amount, {
            amount = it
            error = null
        }, "Importo", Modifier.weight(1f))
        DateField("Data", date, { d -> if (d != null) date = d }, Modifier.weight(1f))
    }
    SelectField(
        label = "Budget",
        options = cats.map { it.id },
        selected = catId,
        optionText = { id -> "${st.category(id)?.name} · ${Formats.eur(st.balance(id))}" },
        onSelect = { catId = it },
    )
    TextInput(note, { note = it }, "Descrizione", placeholder = "es. spesa Esselunga")
    val bal = st.balance(catId)
    val a = Money.parse(amount) ?: 0
    if (a > bal) Notice("Il budget andrà in rosso di ${Formats.eur(a - bal)}.")
    ErrorText(error)
    SheetButtons("Registra spesa", onConfirm = {
        when (val r = Forms.positiveAmount(amount)) {
            is Checked.Error -> error = r.message
            is Checked.Ok -> {
                vm.record(
                    vm.newMovement(MovementKind.SPESA, date, r.value, categoryId = catId, note = note),
                    "Spesa di ${Formats.eur(r.value)} da ${st.category(catId)?.name}",
                )
                close()
            }
        }
    }, onCancel = close)
}

// ---------- assegna ----------

@Composable
private fun AssignForm(st: UiState, vm: MainViewModel, close: () -> Unit) {
    val cats = st.categories
    val available = st.balances.toAssign
    val alloc = remember { mutableStateMapOf<String, String>().apply { cats.forEach { put(it.id, "0,00") } } }
    var error by remember { mutableStateOf<String?>(null) }

    SectionTitle("Assegna dal \"Da assegnare\"")
    Row {
        Notice("Disponibili: ")
        Amount(available, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
    }
    BoxedRows {
        cats.forEachIndexed { i, c ->
            if (i > 0) RowDivider()
            Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(c.name, Modifier.weight(1f))
                AmountField(alloc[c.id].orEmpty(), {
                    alloc[c.id] = it
                    error = null
                }, null, Modifier.width(130.dp))
            }
        }
        RowDivider()
        SummaryRow("Resta da assegnare", available - Forms.sumOf(alloc.values), emphasized = true)
    }
    ErrorText(error)
    SheetButtons("Assegna", onConfirm = {
        when (val r = Forms.assign(cats.associate { it.id to alloc[it.id].orEmpty() }, available)) {
            is Checked.Error -> error = r.message
            is Checked.Ok -> {
                vm.record(
                    vm.newMovement(MovementKind.ASSEGNA, st.today, 0, allocations = r.value),
                    "Assegnati ${Formats.eur(r.value.values.sum())}",
                )
                close()
            }
        }
    }, onCancel = close)
}

// ---------- togli da un budget ----------

@Composable
private fun TakeForm(st: UiState, vm: MainViewModel, c: Category, close: () -> Unit) {
    val bal = st.balance(c.id)
    val others = st.categories.filter { it.id != c.id }
    var amount by remember { mutableStateOf("") }
    var dest by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    SectionTitle("Togli da ${c.name}")
    Row {
        Notice("Saldo attuale: ")
        Amount(bal, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
    }
    Notice("I soldi tolti non sono una spesa: restano sul conto e finiscono dove scegli qui sotto.")
    if (bal <= 0) {
        Text("Questo budget non ha soldi da togliere.")
        OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Chiudi") }
        return
    }
    AmountField(amount, {
        amount = it
        error = null
    }, "Quanto togliere")
    OutlinedButton(onClick = { amount = Money.input(bal) }) { Text("Svuota: tutto il saldo (${Formats.eur(bal)})") }
    SelectField(
        label = "Dove vanno",
        options = listOf<String?>(null) + others.map { it.id },
        selected = dest,
        optionText = { id -> if (id == null) MovementText.TO_ASSIGN else "${st.category(id)?.name} · ${Formats.eur(st.balance(id))}" },
        onSelect = { dest = it },
    )
    TextInput(note, { note = it }, "Motivo (facoltativo)", placeholder = "es. troppo accantonato")
    ErrorText(error)
    SheetButtons("Togli", onConfirm = {
        when (val r = Forms.take(amount, bal)) {
            is Checked.Error -> error = r.message
            is Checked.Ok -> {
                val v = r.value
                val alloc = linkedMapOf(c.id to -v)
                dest?.let { alloc[it] = v }
                vm.record(
                    vm.newMovement(MovementKind.PRELEVA, st.today, 0, note = note, allocations = alloc),
                    "${Formats.eur(v)} tolti da ${c.name} → ${dest?.let { st.category(it)?.name } ?: MovementText.TO_ASSIGN}",
                )
                close()
            }
        }
    }, onCancel = close)
}

// ---------- reimposta totale ----------

@Composable
private fun ResetForm(st: UiState, vm: MainViewModel, close: () -> Unit) {
    val current = st.balances.totalAccount
    var total by remember { mutableStateOf(Money.input(maxOf(0L, current))) }
    var date by remember { mutableStateOf(st.today) }
    var error by remember { mutableStateOf<String?>(null) }

    SectionTitle("Reimposta totale iniziale")
    Notice(
        "Scrivi quanti soldi hai davvero sul conto. La differenza con il totale attuale entra o esce da " +
            "\"Da assegnare\"; i saldi dei budget non cambiano.",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AmountField(total, {
            total = it
            error = null
        }, "Totale reale del conto", Modifier.weight(1f))
        DateField("Data", date, { d -> if (d != null) date = d }, Modifier.weight(1f))
    }
    BoxedRows {
        SummaryRow("Totale attuale", current)
        RowDivider()
        SummaryRow("Differenza su \"Da assegnare\"", Money.parse(total)?.let { it - current } ?: 0, emphasized = true, sign = true)
    }
    ErrorText(error)
    SheetButtons("Reimposta", onConfirm = {
        when (val r = Forms.accountTotal(total)) {
            is Checked.Error -> error = r.message
            is Checked.Ok -> {
                val diff = st.math.reconcileDifference(r.value)
                if (diff != 0L) {
                    vm.record(
                        vm.newMovement(MovementKind.RETTIFICA, date, diff, total = r.value),
                        "Totale conto impostato a ${Formats.eur(r.value)}",
                    )
                }
                close()
            }
        }
    }, onCancel = close)
}

// ---------- spese rapide ----------

@Composable
private fun QuickList(st: UiState, vm: MainViewModel, onOpen: (Sheet?) -> Unit, onGoToSettings: () -> Unit) {
    val list = st.quickShortcuts
    SectionTitle("Spese rapide")
    Notice("Tocca per registrare la spesa con la data di oggi. Con ✎ cambi importo, data o budget prima di registrarla.")
    if (list.isEmpty()) {
        Text("Non hai ancora spese rapide. Le crei in Impostazioni, sotto \"Spese rapide\".")
        OutlinedButton(onClick = onGoToSettings, modifier = Modifier.fillMaxWidth()) { Text("Vai alle Impostazioni") }
    }
    for (s in list) QuickRow(st, s, onTap = {
        vm.quickExpense(s)
        onOpen(null)
    }, onEdit = {
        onOpen(Sheet.Expense(ExpensePrefill("${s.name}, con modifiche", s.amount, s.categoryId, s.name)))
    })
    OutlinedButton(onClick = { onOpen(null) }, modifier = Modifier.fillMaxWidth()) { Text("Chiudi") }
}

@Composable
private fun QuickRow(st: UiState, s: Shortcut, onTap: () -> Unit, onEdit: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppTheme.colors.line),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onTap, color = MaterialTheme.colorScheme.background, modifier = Modifier.weight(1f)) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.name, fontWeight = FontWeight.SemiBold)
                        Text(st.category(s.categoryId)?.name.orEmpty(), style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
                    }
                    Text(Formats.eur(s.amount), style = MaterialTheme.typography.titleMedium.tabular())
                }
            }
            VerticalDivider(Modifier.fillMaxHeight(), color = AppTheme.colors.line)
            Surface(onClick = onEdit, color = MaterialTheme.colorScheme.background) {
                Box(Modifier.padding(horizontal = 18.dp, vertical = 22.dp)) {
                    Icon(AppIcons.Edit, "Registra ${s.name} con modifiche", tint = AppTheme.colors.muted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ---------- elimina budget ----------

@Composable
private fun DeleteCategoryForm(st: UiState, vm: MainViewModel, c: Category, close: () -> Unit) {
    val bal = st.balance(c.id)
    val nMov = st.data.movements.count { MovementText.touches(it, c.id) }
    val nShort = st.allShortcuts.count { it.categoryId == c.id }
    val others = st.categories.filter { it.id != c.id }
    var dest by remember { mutableStateOf<String?>(null) }

    SectionTitle("Eliminare \"${c.name}\"?")
    BoxedRows {
        SummaryRow("Saldo attuale", bal)
        RowDivider()
        CountRow("Movimenti collegati", nMov)
        RowDivider()
        CountRow("Spese rapide collegate", nShort)
    }
    if (bal > 0) {
        SelectField(
            label = "Dove vanno i ${Formats.eur(bal)} rimasti",
            options = listOf<String?>(null) + others.map { it.id },
            selected = dest,
            optionText = { id -> if (id == null) MovementText.TO_ASSIGN else st.category(id)?.name.orEmpty() },
            onSelect = { dest = it },
        )
    }
    if (bal < 0) Notice("Il budget è in rosso: i ${Formats.eur(-bal)} mancanti verranno coperti da \"Da assegnare\".")
    Notice(
        buildString {
            if (nMov > 0) append("I movimenti restano nello storico e nel riepilogo, indicati come \"Budget eliminato\". ")
            if (nShort > 0) append("Le spese rapide collegate vengono eliminate. ")
            append("Non si può annullare.")
        },
    )
    SheetButtons("Elimina definitivamente", onConfirm = {
        vm.deleteCategory(c, dest)
        close()
    }, onCancel = close, danger = true)
}

@Composable
private fun CountRow(label: String, n: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, Modifier.weight(1f))
        Text(n.toString(), style = MaterialTheme.typography.bodyLarge.tabular())
    }
}
