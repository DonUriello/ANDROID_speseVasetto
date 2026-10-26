@file:OptIn(ExperimentalMaterial3Api::class)

package it.elia.gestionespese.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import it.elia.gestionespese.core.Category
import it.elia.gestionespese.core.CategoryDraft
import it.elia.gestionespese.core.CategoryKind
import it.elia.gestionespese.core.Checked
import it.elia.gestionespese.core.FillMode
import it.elia.gestionespese.core.Formats
import it.elia.gestionespese.core.Ids
import it.elia.gestionespese.core.Money
import it.elia.gestionespese.core.Shortcut
import it.elia.gestionespese.core.ShortcutDraft
import it.elia.gestionespese.ui.MainViewModel
import it.elia.gestionespese.ui.UiState
import it.elia.gestionespese.ui.components.AmountField
import it.elia.gestionespese.ui.components.Badge
import it.elia.gestionespese.ui.components.DateField
import it.elia.gestionespese.ui.components.Notice
import it.elia.gestionespese.ui.components.Panel
import it.elia.gestionespese.ui.components.SectionTitle
import it.elia.gestionespese.ui.components.SelectField
import it.elia.gestionespese.ui.components.SmallLabel
import it.elia.gestionespese.ui.components.TextInput
import it.elia.gestionespese.ui.theme.AppTheme
import it.elia.gestionespese.ui.theme.tabular

/** Azioni delle Impostazioni che non passano dal ViewModel (aprire moduli e file). */
class SettingsActions(
    val onDeleteCategory: (Category) -> Unit,
    val onExport: () -> Unit,
    val onImport: () -> Unit,
)

@Composable
fun SettingsScreen(
    st: UiState,
    vm: MainViewModel,
    actions: SettingsActions,
    newCategoryOpen: Boolean,
    onNewCategoryOpen: (Boolean) -> Unit,
    header: @Composable () -> Unit,
    contentPadding: PaddingValues,
) {
    var newShortcutOpen by remember { mutableStateOf(false) }
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        item { header() }
        item { PaydayPanel(st, vm) }
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Budget", Modifier.weight(1f))
                }
                val quotaTot = st.categories.sumOf { st.math.effectiveQuota(it, st.balance(it.id)) }
                Row {
                    Notice("Totale quote al prossimo stipendio: ")
                    Text(Formats.eur(quotaTot), style = MaterialTheme.typography.bodySmall.tabular(), fontWeight = FontWeight.SemiBold)
                }
                if (newCategoryOpen) {
                    CategoryEditor(st, vm, null, 0, 0, actions, onClose = { onNewCategoryOpen(false) })
                } else {
                    OutlinedButton(onClick = { onNewCategoryOpen(true) }) { Text("+ Nuovo budget") }
                }
                st.categories.forEachIndexed { i, c ->
                    CategoryEditor(st, vm, c, i, st.categories.size, actions, onClose = {})
                }
                Notice(
                    "Le modifiche si applicano solo quando premi Salva. L'ordine conta: se lo stipendio non basta per tutte " +
                        "le quote, si riempiono dall'alto verso il basso e il resto va in \"Da assegnare\". Un obiettivo " +
                        "automatico con soglia e scadenza calcola da solo la quota; uno manuale riceve la quota che scrivi tu " +
                        "(zero = lo riempi a mano).",
                )
            }
        }
        item {
            Panel {
                SectionTitle("Spese rapide")
                if (newShortcutOpen) {
                    ShortcutEditor(st, vm, null, onClose = { newShortcutOpen = false })
                } else {
                    OutlinedButton(onClick = { newShortcutOpen = true }, enabled = st.categories.isNotEmpty()) {
                        Text("+ Nuova spesa rapida")
                    }
                }
                for (s in st.allShortcuts) ShortcutEditor(st, vm, s, onClose = {})
                Notice(
                    "Le trovi nel pulsante \"Spese rapide\": un tocco registra la spesa con la data di oggi. " +
                        "Non vengono mai scalate da sole.",
                )
            }
        }
        item {
            Panel {
                SectionTitle("Backup")
                Notice(
                    "Il backup è un file JSON con budget, spese rapide, movimenti e impostazioni. Importarlo sostituisce " +
                        "tutti i dati presenti sul telefono.",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = actions.onExport, modifier = Modifier.weight(1f)) { Text("Esporta backup") }
                    OutlinedButton(onClick = actions.onImport, modifier = Modifier.weight(1f)) { Text("Importa backup") }
                }
            }
        }
    }
}

@Composable
private fun PaydayPanel(st: UiState, vm: MainViewModel) {
    val saved = st.data.settings.payday
    var text by remember(saved) { mutableStateOf(saved.toString()) }
    val focus = LocalFocusManager.current
    val commit: () -> Unit = {
        val d = text.trim().toIntOrNull()
        if (d != null && d in 1..31) {
            vm.setPayday(d)
        } else {
            text = saved.toString()
        }
    }
    Panel {
        SectionTitle("Stipendio")
        OutlinedTextField(
            value = text,
            onValueChange = { v -> text = v.filter { it.isDigit() }.take(2) },
            label = { Text("Giorno di arrivo") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                commit()
                focus.clearFocus()
            }),
            modifier = Modifier.width(180.dp).onFocusChanged { if (!it.isFocused) commit() },
        )
        Notice(
            "È la data proposta quando registri lo stipendio e serve agli obiettivi: contano gli stipendi che arrivano " +
                "entro il giorno della scadenza. Se lo stipendio di questo mese è già registrato, si parte dal mese prossimo.",
        )
    }
}

/** Riga modificabile: bordo tratteggiato verde se nuova, sfondo colorato se ha modifiche non salvate. */
@Composable
private fun EditorRow(isNew: Boolean, dirty: Boolean, content: @Composable () -> Unit) {
    val colors = AppTheme.colors
    val dashed = Modifier.drawBehind {
        drawRoundRect(
            color = colors.ok,
            cornerRadius = CornerRadius(10.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
        )
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = when {
            isNew -> colors.ok.copy(alpha = 0.06f)
            dirty -> colors.ok.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surface
        },
        border = if (isNew) null else BorderStroke(1.dp, colors.line),
        modifier = Modifier.fillMaxWidth().then(if (isNew) dashed else Modifier),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun SaveDeleteButtons(isNew: Boolean, dirty: Boolean, onSave: () -> Unit, onDelete: () -> Unit, onCancel: () -> Unit) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onSave,
            enabled = dirty,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = colors.ok, contentColor = colors.onOk),
        ) { Text(if (isNew) "Crea" else "Salva") }
        if (isNew) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Annulla") }
        } else {
            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = colors.neg, contentColor = Color.White),
            ) { Text("Elimina") }
        }
    }
}

@Composable
private fun CategoryEditor(
    st: UiState,
    vm: MainViewModel,
    category: Category?,
    index: Int,
    count: Int,
    actions: SettingsActions,
    onClose: () -> Unit,
) {
    val isNew = category == null
    var draft by remember(category) { mutableStateOf(category?.let { CategoryDraft.of(it) } ?: CategoryDraft()) }
    var error by remember(category) { mutableStateOf<String?>(null) }
    val dirty = category == null || draft.differsFrom(category)
    val edit = { d: CategoryDraft ->
        draft = d
        error = null
    }

    EditorRow(isNew, dirty) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (category == null) {
                Text("Nuovo budget", fontWeight = FontWeight.Bold)
            } else {
                OrderButton("↑", "Sposta su ${category.name}", index > 0) { vm.moveCategory(category.id, -1) }
                OrderButton("↓", "Sposta giù ${category.name}", index < count - 1) { vm.moveCategory(category.id, 1) }
                Text(category.name, Modifier.weight(1f).padding(start = 4.dp), fontWeight = FontWeight.Bold)
                if (category.isGoal) {
                    Badge("Obiettivo", AppTheme.colors.goalSoft, AppTheme.colors.goal)
                } else {
                    Badge("Spese", AppTheme.colors.spendSoft, AppTheme.colors.spend)
                }
            }
        }
        TextInput(draft.name, { edit(draft.copy(name = it)) }, "Nome", placeholder = "es. Spesa alimentare")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectField(
                "Tipo", CategoryKind.entries, draft.kind,
                { if (it == CategoryKind.SPESA) "Spese" else "Obiettivo" },
                { edit(draft.copy(kind = it)) },
                Modifier.weight(1f),
            )
            if (draft.isGoal) {
                SelectField(
                    "Riempimento", FillMode.entries, draft.fill,
                    { if (it == FillMode.AUTO) "Automatico" else "Manuale" },
                    { edit(draft.copy(fill = it)) },
                    Modifier.weight(1f),
                )
            }
        }
        if (draft.hasAutoQuota) {
            val plan = category?.let {
                st.math.goalPlan(
                    it.copy(kind = CategoryKind.OBIETTIVO, target = Money.parse(draft.target), deadline = draft.deadline),
                    st.balance(it.id),
                )
            }
            Column {
                SmallLabel("Quota a stipendio")
                Text(
                    plan?.let { "${Formats.eur(it.quota)} · auto" } ?: "Calcolata dalla scadenza",
                    color = AppTheme.colors.goal,
                    style = MaterialTheme.typography.bodyLarge.tabular(),
                )
            }
        } else {
            AmountField(draft.quota, { edit(draft.copy(quota = it)) }, "Quota a stipendio")
        }
        if (draft.isGoal) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountField(draft.target, { edit(draft.copy(target = it)) }, "Soglia", Modifier.weight(1f), placeholder = "es. 1500")
                DateField("Entro il", draft.deadline, { edit(draft.copy(deadline = it)) }, Modifier.weight(1f), clearable = true)
            }
        }
        error?.let { Text(it, color = AppTheme.colors.neg, style = MaterialTheme.typography.bodySmall) }
        SaveDeleteButtons(
            isNew = isNew,
            dirty = dirty,
            onSave = {
                val base = category ?: Category(Ids.newId(), "", CategoryKind.SPESA, order = vm.nextCategoryOrder())
                when (val r = draft.toCategory(base)) {
                    is Checked.Error -> error = r.message
                    is Checked.Ok -> {
                        vm.saveCategory(r.value, created = isNew)
                        if (isNew) onClose()
                    }
                }
            },
            onDelete = { category?.let(actions.onDeleteCategory) },
            onCancel = onClose,
        )
    }
}

@Composable
private fun OrderButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.size(34.dp),
    ) {
        Text(symbol, color = AppTheme.colors.muted)
    }
}

@Composable
private fun ShortcutEditor(st: UiState, vm: MainViewModel, shortcut: Shortcut?, onClose: () -> Unit) {
    val isNew = shortcut == null
    var draft by remember(shortcut) {
        mutableStateOf(shortcut?.let { ShortcutDraft.of(it) } ?: ShortcutDraft(categoryId = st.categories.firstOrNull()?.id))
    }
    var error by remember(shortcut) { mutableStateOf<String?>(null) }
    val dirty = shortcut == null || draft.differsFrom(shortcut)
    val edit = { d: ShortcutDraft ->
        draft = d
        error = null
    }
    EditorRow(isNew, dirty) {
        if (isNew) Text("Nuova spesa rapida", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextInput(draft.name, { edit(draft.copy(name = it)) }, "Nome", Modifier.weight(1.4f), placeholder = "es. Caffè")
            AmountField(draft.amount, { edit(draft.copy(amount = it)) }, "Importo", Modifier.weight(1f))
        }
        SelectField(
            label = "Budget",
            options = st.categories.map { it.id },
            selected = draft.categoryId.orEmpty(),
            optionText = { id -> st.category(id)?.name ?: "Budget eliminato" },
            onSelect = { edit(draft.copy(categoryId = it)) },
        )
        error?.let { Text(it, color = AppTheme.colors.neg, style = MaterialTheme.typography.bodySmall) }
        SaveDeleteButtons(
            isNew = isNew,
            dirty = dirty,
            onSave = {
                val base = shortcut ?: Shortcut(Ids.newId(), "", 1, "", order = vm.nextShortcutOrder())
                when (val r = draft.toShortcut(base) { st.category(it) != null }) {
                    is Checked.Error -> error = r.message
                    is Checked.Ok -> {
                        vm.saveShortcut(r.value, created = isNew)
                        if (isNew) onClose()
                    }
                }
            },
            onDelete = { shortcut?.let(vm::deleteShortcut) },
            onCancel = onClose,
        )
    }
}
