@file:OptIn(ExperimentalMaterial3Api::class)

package it.elia.gestionespese.ui.movements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.elia.gestionespese.core.Formats
import it.elia.gestionespese.core.Movement
import it.elia.gestionespese.core.MovementText
import it.elia.gestionespese.ui.UiState
import it.elia.gestionespese.ui.components.AccountHeader
import it.elia.gestionespese.ui.components.Badge
import it.elia.gestionespese.ui.components.Notice
import it.elia.gestionespese.ui.components.Panel
import it.elia.gestionespese.ui.components.RowDivider
import it.elia.gestionespese.ui.components.SectionTitle
import it.elia.gestionespese.ui.components.SelectField
import it.elia.gestionespese.ui.components.SmallLabel
import it.elia.gestionespese.ui.theme.AppTheme
import it.elia.gestionespese.ui.theme.tabular
import kotlinx.coroutines.delay
import java.time.YearMonth

/** Filtri della lista: budget e mese (null = tutti). */
data class MovementFilter(val categoryId: String? = null, val month: YearMonth? = null)

@Composable
fun MovementsScreen(
    st: UiState,
    filter: MovementFilter,
    onFilter: (MovementFilter) -> Unit,
    onDelete: (Movement) -> Unit,
    header: @Composable () -> Unit,
    contentPadding: PaddingValues,
) {
    val months = st.math.monthsWithData().reversed()
    // Filtri non più validi (budget eliminato, mese senza dati) tornano a "tutti"
    val catId = filter.categoryId?.takeIf { st.category(it) != null }
    val month = filter.month?.takeIf { it in months }
    val shown = st.data.movements
        .filter { (month == null || it.month == month) && (catId == null || MovementText.touches(it, catId)) }
        .sortedWith(compareByDescending<Movement> { it.date }.thenByDescending { it.createdAt })
    val byMonth = shown.groupBy { it.month }

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        item { header() }
        if (months.isEmpty()) {
            item {
                Panel(Modifier.padding(top = 12.dp)) {
                    SectionTitle("Nessun movimento")
                    Text("Qui compariranno stipendi, entrate, spese e assegnazioni, raggruppati per mese.")
                }
            }
            return@LazyColumn
        }
        item {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Movimenti")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectField(
                        label = "Budget",
                        options = listOf<String?>(null) + st.categories.map { it.id },
                        selected = catId,
                        optionText = { id -> if (id == null) "Tutti i budget" else st.category(id)?.name.orEmpty() },
                        onSelect = { onFilter(filter.copy(categoryId = it)) },
                        modifier = Modifier.weight(1f),
                    )
                    SelectField(
                        label = "Mese",
                        options = listOf<YearMonth?>(null) + months,
                        selected = month,
                        optionText = { m -> if (m == null) "Tutti i mesi" else Formats.monthLong(m) },
                        onSelect = { onFilter(filter.copy(month = it)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (catId != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Badge("Solo ${st.category(catId)?.name}", AppTheme.colors.spendSoft, AppTheme.colors.spend)
                        TextButton(onClick = { onFilter(filter.copy(categoryId = null)) }) { Text("Mostra tutti") }
                    }
                }
                if (shown.isEmpty()) Notice("Nessun movimento per questo filtro.")
            }
        }
        for ((m, list) in byMonth) {
            item(key = "h-$m") { SmallLabel(Formats.monthLong(m), Modifier.padding(top = 10.dp)) }
            items(list, key = { it.id }) { mv ->
                MovementRow(st, mv, onDelete)
                RowDivider()
            }
        }
    }
}

@Composable
private fun MovementRow(st: UiState, m: Movement, onDelete: (Movement) -> Unit) {
    val line = MovementText.describe(m) { st.category(it)?.name }
    var armed by remember(m.id) { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(4000)
            armed = false
        }
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Formats.dayMonth(m.date),
                Modifier.width(56.dp),
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = AppTheme.colors.muted,
            )
            Column(Modifier.weight(1f)) {
                Text(line.title)
                if (line.subtitle.isNotEmpty()) {
                    Text(line.subtitle, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
                }
            }
            Text(
                line.sign + Formats.eur(line.amount),
                Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyLarge.tabular(),
                color = if (line.incoming) AppTheme.colors.chartIn else Color.Unspecified,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (armed) {
                Button(
                    onClick = { onDelete(m) },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.neg, contentColor = Color.White),
                ) { Text("Conferma eliminazione") }
            } else {
                TextButton(onClick = { armed = true }) { Text("Elimina", color = AppTheme.colors.muted) }
            }
        }
    }
}
