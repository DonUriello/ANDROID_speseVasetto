@file:OptIn(ExperimentalMaterial3Api::class)

package it.elia.gestionespese.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.elia.gestionespese.core.BudgetMath
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
import it.elia.gestionespese.ui.components.SegmentedPills
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
    data class Add(val categoryId: String) : Sheet
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
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = AppTheme.colors.ink,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 22.dp, end = 22.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (sheet) {
                Sheet.Salary -> IncomeForm(st, vm, MovementKind.STIPENDIO, close)
                Sheet.Income -> IncomeForm(st, vm, MovementKind.ENTRATA, close)
                is Sheet.Expense -> ExpenseForm(st, vm, sheet.prefill, close)
                Sheet.Assign -> AssignForm(st, vm, close)
                is Sheet.Take -> st.category(sheet.categoryId)?.let { TakeForm(st, vm, it, close) }
                is Sheet.Add -> st.category(sheet.categoryId)?.let { AddForm(st, vm, it, close) }
                Sheet.Reset -> ResetForm(st, vm, close)
                Sheet.Quick -> QuickList(st, vm, onOpen, onGoToSettings)
                is Sheet.DeleteCategory -> st.category(sheet.categoryId)?.let { DeleteCategoryForm(st, vm, it, close) }
            }
        }
    }
}

// ---------- entrate: stipendio, stipendio extra, altro ----------

private fun incomeLabel(kind: MovementKind) = when (kind) {
    MovementKind.STIPENDIO -> "Stipendio"
    MovementKind.STIPENDIO_EXTRA -> "Stipendio extra"
    else -> "Altro"
}

/**
 * Registra un'entrata. Solo lo STIPENDIO riempie i vasetti con le quote proposte;
 * stipendio extra (tredicesima…) e altro vanno tutti in "Da assegnare".
 */
@Composable
private fun IncomeForm(st: UiState, vm: MainViewModel, initialKind: MovementKind, close: () -> Unit) {
    val cats = st.categories
    val quotas = remember { cats.associate { it.id to st.math.effectiveQuota(it, st.balance(it.id)) } }
    var kind by remember { mutableStateOf(initialKind) }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(if (initialKind == MovementKind.STIPENDIO) st.math.proposedSalaryDate() else st.today) }
    var note by remember { mutableStateOf("") }
    val alloc: SnapshotStateMap<String, String> = remember { mutableStateMapOf() }
    var error by remember { mutableStateOf<String?>(null) }
    val salary = kind == MovementKind.STIPENDIO
    val fill = { text: String ->
        // Quote proposte in ordine (SPEC.md §4.5), ricalcolate a ogni modifica dell'importo
        st.math.salarySplit(Money.parse(text) ?: 0, cats, st.balances).proposals.forEach { (id, v) -> alloc[id] = Money.input(v) }
    }

    SheetTitle("Registra un'entrata")
    SegmentedPills(
        options = listOf(MovementKind.STIPENDIO, MovementKind.STIPENDIO_EXTRA, MovementKind.ENTRATA),
        selected = kind,
        optionText = { if (it == MovementKind.STIPENDIO_EXTRA) "Extra" else incomeLabel(it) },
        onSelect = {
            kind = it
            error = null
            if (it == MovementKind.STIPENDIO) fill(amount)
        },
    )
    Notice(
        when (kind) {
            MovementKind.STIPENDIO -> "Riempie i vasetti fino al prossimo stipendio: le quote fisse vengono distribuite in ordine e puoi ritoccarle prima di confermare."
            MovementKind.STIPENDIO_EXTRA -> "Tredicesima, quattordicesima e simili: non riempiono i vasetti, vanno tutte in \"Da assegnare\"."
            else -> "Rimborsi, regali e altre entrate: non riempiono i vasetti, vanno tutte in \"Da assegnare\"."
        },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AmountField(amount, {
            amount = it
            error = null
            if (salary) fill(it)
        }, "Importo", Modifier.weight(1f), background = AppTheme.colors.peachSoft)
        DateField("Data", date, { d -> if (d != null) date = d }, Modifier.weight(1f))
    }
    if (salary) InfoChip("Periodo: ${st.math.periodLabel(st.math.periodOf(date.plusDays(BudgetMath.SALARY_EARLY_DAYS)))}")
    if (!salary) {
        TextInput(
            note, { note = it }, "Descrizione",
            placeholder = if (kind == MovementKind.STIPENDIO_EXTRA) "es. tredicesima" else "es. rimborso, regalo",
        )
    }
    if (salary) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cats.forEachIndexed { i, c ->
                QuoteRow(c.name, quotaSubtitle(st, c, quotas[c.id] ?: 0), stripColor(i), alloc[c.id].orEmpty()) {
                    alloc[c.id] = it
                    error = null
                }
            }
            SummaryRow("Resta in \"Da assegnare\"", (Money.parse(amount) ?: 0) - Forms.sumOf(alloc.values), emphasized = true)
        }
    }
    ErrorText(error)
    SheetButtons("Conferma", onConfirm = {
        val texts = if (salary) cats.associate { it.id to alloc[it.id].orEmpty() } else emptyMap()
        when (val r = Forms.income(amount, texts)) {
            is Checked.Error -> error = r.message
            is Checked.Ok -> {
                vm.record(
                    vm.newMovement(kind, date, r.value.amount, note = if (salary) null else note, allocations = r.value.allocations),
                    if (kind == MovementKind.ENTRATA) "Entrata di ${Formats.eur(r.value.amount)} registrata"
                    else "${incomeLabel(kind)} di ${Formats.eur(r.value.amount)} registrato",
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

    SheetTitle(prefill?.title ?: "Registra una spesa")
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

    SheetTitle("Assegna dal \"Da assegnare\"")
    InfoChip("Disponibili: ${Formats.eur(available)}")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cats.forEachIndexed { i, c ->
            QuoteRow(c.name, "saldo ${Formats.eur(st.balance(c.id))}", stripColor(i), alloc[c.id].orEmpty()) {
                alloc[c.id] = it
                error = null
            }
        }
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

// ---------- aggiungi a un budget ----------

/**
 * Aggiunge soldi a un budget (es. svuotato e non ricaricato): da "Da assegnare" (un'assegnazione)
 * oppure da un altro budget (uno spostamento, non una spesa).
 */
@Composable
private fun AddForm(st: UiState, vm: MainViewModel, c: Category, close: () -> Unit) {
    val toAssign = st.balances.toAssign
    val others = st.categories.filter { it.id != c.id }
    var amount by remember { mutableStateOf("") }
    var source by remember {
        mutableStateOf(if (toAssign > 0) null else others.firstOrNull { st.balance(it.id) > 0 }?.id)
    }
    var error by remember { mutableStateOf<String?>(null) }
    val available = source?.let { st.balance(it) } ?: toAssign

    SheetTitle("Aggiungi a ${c.name}")
    Row {
        Notice("Saldo attuale: ")
        Amount(st.balance(c.id), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
    }
    AmountField(amount, {
        amount = it
        error = null
    }, "Quanto aggiungere", background = AppTheme.colors.peachSoft)
    SelectField(
        label = "Da dove",
        options = listOf<String?>(null) + others.map { it.id },
        selected = source,
        optionText = { id ->
            if (id == null) "${MovementText.TO_ASSIGN} · ${Formats.eur(toAssign)}"
            else "${st.category(id)?.name} · ${Formats.eur(st.balance(id))}"
        },
        onSelect = {
            source = it
            error = null
        },
    )
    Notice(
        when {
            source != null -> "Sposta i soldi da ${st.category(source)?.name}: non è una spesa."
            toAssign <= 0 -> "\"Da assegnare\" è vuoto: registra prima un'entrata con + oppure prendi da un altro budget."
            else -> "Prende dai soldi non ancora assegnati."
        },
    )
    ErrorText(error)
    SheetButtons("Aggiungi", onConfirm = {
        when (val r = Forms.addTo(amount, available)) {
            is Checked.Error -> error = r.message
            is Checked.Ok -> {
                val v = r.value
                val from = source
                if (from == null) {
                    vm.record(
                        vm.newMovement(MovementKind.ASSEGNA, st.today, 0, allocations = mapOf(c.id to v)),
                        "Aggiunti ${Formats.eur(v)} a ${c.name}",
                    )
                } else {
                    vm.record(
                        vm.newMovement(MovementKind.PRELEVA, st.today, 0, allocations = linkedMapOf(from to -v, c.id to v)),
                        "${Formats.eur(v)} spostati da ${st.category(from)?.name} a ${c.name}",
                    )
                }
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

    SheetTitle("Togli da ${c.name}")
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
    OutlinedButton(onClick = { amount = Money.input(bal) }, shape = RoundedCornerShape(14.dp)) {
        Text("Svuota: tutto il saldo (${Formats.eur(bal)})", color = AppTheme.colors.ink)
    }
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

    SheetTitle("Reimposta totale iniziale")
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
    val colors = AppTheme.colors
    val list = st.quickShortcuts
    val tiles = listOf(
        colors.amberSoft to colors.ink,
        colors.peachSoft to colors.ink,
        colors.tealSoft to colors.ink,
        colors.navy to colors.onNavy,
        colors.blueSoft to colors.ink,
    )
    SheetTitle("Spese rapide")
    Notice("Un tocco registra la spesa con la data di oggi. Con la matita cambi importo, data o budget.")
    // Tessere a due colonne, più quella per crearne una nuova
    val cells: List<Shortcut?> = list + listOf(null)
    for (row in cells.chunked(2)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (s in row) {
                if (s == null) {
                    NewTile(onGoToSettings, Modifier.weight(1f).fillMaxHeight())
                } else {
                    val (bg, fg) = tiles[list.indexOf(s) % tiles.size]
                    QuickTile(st, s, bg, fg, Modifier.weight(1f).fillMaxHeight(), onTap = {
                        vm.quickExpense(s)
                        onOpen(null)
                    }, onEdit = {
                        onOpen(Sheet.Expense(ExpensePrefill("${s.name}, con modifiche", s.amount, s.categoryId, s.name)))
                    })
                }
            }
            if (row.size == 1) Box(Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickTile(
    st: UiState,
    s: Shortcut,
    background: Color,
    foreground: Color,
    modifier: Modifier,
    onTap: () -> Unit,
    onEdit: () -> Unit,
) {
    Box(modifier) {
        Surface(
            onClick = onTap,
            shape = RoundedCornerShape(22.dp),
            color = background,
            contentColor = foreground,
            modifier = Modifier.fillMaxWidth().fillMaxHeight().heightIn(min = 118.dp),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(s.name, Modifier.padding(end = 36.dp), fontWeight = FontWeight.SemiBold)
                Column(Modifier.padding(top = 12.dp)) {
                    Text(Formats.eur(s.amount), style = MaterialTheme.typography.titleLarge.tabular(), fontWeight = FontWeight.Bold)
                    Text(st.category(s.categoryId)?.name.orEmpty(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Surface(
            onClick = onEdit,
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.75f),
            contentColor = Color(0xFF2E3E5C),
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(AppIcons.Edit, "Registra ${s.name} con modifiche", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun NewTile(onClick: () -> Unit, modifier: Modifier) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = colors.muted,
        border = BorderStroke(2.dp, colors.line),
        modifier = modifier.heightIn(min = 118.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
            Text("Nuova spesa rapida", style = MaterialTheme.typography.bodyMedium)
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

    SheetTitle("Eliminare \"${c.name}\"?")
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

// ---------- elementi comuni dei moduli ----------

@Composable
private fun SheetTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
}

/** Etichetta teal tenue, es. il periodo dello stipendio. */
@Composable
private fun InfoChip(text: String) {
    Text(
        text,
        modifier = Modifier.background(AppTheme.colors.accentSoft, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = AppTheme.colors.ink,
    )
}

@Composable
private fun stripColor(i: Int): Color {
    val c = AppTheme.colors
    return listOf(c.peach, c.chartIn, c.amber, c.navy)[i % 4]
}

/** Riga di una quota: segno colorato, nome, sottotitolo e campo importo compatto. */
@Composable
private fun QuoteRow(
    name: String,
    subtitle: String,
    strip: Color,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        Modifier.fillMaxWidth().background(colors.sunk, RoundedCornerShape(16.dp)).padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(10.dp).height(36.dp).background(strip, RoundedCornerShape(5.dp)))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.muted)
        }
        AmountField(value, onValueChange, null, Modifier.width(112.dp))
    }
}
