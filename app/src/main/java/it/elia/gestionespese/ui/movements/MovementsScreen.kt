@file:OptIn(ExperimentalMaterial3Api::class)

package it.elia.gestionespese.ui.movements

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.elia.gestionespese.core.Formats
import it.elia.gestionespese.core.Movement
import it.elia.gestionespese.core.MovementKind
import it.elia.gestionespese.core.MovementText
import it.elia.gestionespese.ui.UiState
import it.elia.gestionespese.ui.components.AppIcons
import it.elia.gestionespese.ui.components.Notice
import it.elia.gestionespese.ui.components.Panel
import it.elia.gestionespese.ui.components.PillChip
import it.elia.gestionespese.ui.components.ScreenTitle
import it.elia.gestionespese.ui.components.SectionTitle
import it.elia.gestionespese.ui.components.SmallLabel
import it.elia.gestionespese.ui.theme.AppColors
import it.elia.gestionespese.ui.theme.AppTheme
import it.elia.gestionespese.ui.theme.tabular
import kotlinx.coroutines.delay
import java.time.YearMonth

/** Filtri della lista: budget e periodo (null = tutti). */
data class MovementFilter(val categoryId: String? = null, val month: YearMonth? = null)

@Composable
fun MovementsScreen(
    st: UiState,
    filter: MovementFilter,
    onFilter: (MovementFilter) -> Unit,
    onDelete: (Movement) -> Unit,
    contentPadding: PaddingValues,
) {
    val periods = st.math.monthsWithData()
    // Filtri non più validi (budget eliminato, periodo senza dati) tornano a "tutti"
    val catId = filter.categoryId?.takeIf { st.category(it) != null }
    val month = filter.month?.takeIf { it in periods }
    val shown = st.data.movements
        .filter { (month == null || st.math.periodOf(it) == month) && (catId == null || MovementText.touches(it, catId)) }
        .sortedWith(compareByDescending<Movement> { it.date }.thenByDescending { it.createdAt })
    val byPeriod = shown.groupBy { st.math.periodOf(it) }

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        item { ScreenTitle("Movimenti") }
        if (periods.isEmpty()) {
            item {
                Panel(Modifier.padding(top = 8.dp)) {
                    SectionTitle("Nessun movimento")
                    Text("Qui compariranno stipendi, entrate, spese e assegnazioni, raggruppati per periodo.")
                }
            }
            return@LazyColumn
        }
        item { PeriodPicker(st, periods, month) { onFilter(filter.copy(month = it)) } }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip("Tutti i budget", catId == null, { onFilter(filter.copy(categoryId = null)) })
                for (c in st.categories) {
                    PillChip(c.name, catId == c.id, { onFilter(filter.copy(categoryId = c.id)) })
                }
            }
        }
        if (shown.isEmpty()) item { Notice("Nessun movimento per questo filtro.") }
        for ((p, list) in byPeriod) {
            item(key = "h-$p") { SmallLabel(st.math.periodLabel(p), Modifier.padding(top = 6.dp)) }
            items(list, key = { it.id }) { mv -> MovementCard(st, mv, onDelete) }
        }
    }
}

/** Caselle dei periodi, come un calendario: giorno e mese in cui inizia ciascun periodo, più "Tutti". */
@Composable
private fun PeriodPicker(st: UiState, periods: List<YearMonth>, selected: YearMonth?, onSelect: (YearMonth?) -> Unit) {
    val colors = AppTheme.colors
    val scroll = rememberScrollState()
    LaunchedEffect(periods.size) { scroll.scrollTo(scroll.maxValue) }
    Row(Modifier.horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (p in periods) {
            val start = st.math.periodStart(p)
            val on = p == selected
            Surface(
                onClick = { onSelect(if (on) null else p) },
                shape = RoundedCornerShape(16.dp),
                color = if (on) colors.peach else MaterialTheme.colorScheme.surface,
                contentColor = if (on) colors.onPeach else colors.muted,
                shadowElevation = if (on) 0.dp else 1.dp,
                modifier = Modifier.size(64.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(start.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold)
                    Text(Formats.monthShort(p), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Surface(
            onClick = { onSelect(null) },
            shape = RoundedCornerShape(16.dp),
            color = if (selected == null) colors.peach else MaterialTheme.colorScheme.surface,
            contentColor = if (selected == null) colors.onPeach else colors.muted,
            shadowElevation = if (selected == null) 0.dp else 1.dp,
            modifier = Modifier.height(64.dp).width(80.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Tutti", fontWeight = if (selected == null) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

/** Fondo e striscia laterale per tipo di movimento. */
private fun styleOf(kind: MovementKind, colors: AppColors, surface: Color): Pair<Color, Color> = when (kind) {
    MovementKind.STIPENDIO, MovementKind.STIPENDIO_EXTRA, MovementKind.ENTRATA -> colors.tealSoft to colors.header
    MovementKind.PRELEVA -> colors.amberSoft to colors.amber
    MovementKind.ASSEGNA -> colors.blueSoft to colors.navy
    MovementKind.RETTIFICA -> colors.sunk to colors.muted
    MovementKind.SPESA -> surface to colors.peach
}

@Composable
private fun MovementCard(st: UiState, m: Movement, onDelete: (Movement) -> Unit) {
    val colors = AppTheme.colors
    val line = MovementText.describe(m) { st.category(it)?.name }
    val (bg, strip) = styleOf(m.kind, colors, MaterialTheme.colorScheme.surface)
    var armed by remember(m.id) { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(4000)
            armed = false
        }
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bg,
        contentColor = colors.ink,
        shadowElevation = if (m.kind == MovementKind.SPESA) 1.dp else 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(10.dp).fillMaxHeight().background(strip))
            Column(Modifier.weight(1f).padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(line.title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(
                        line.sign + Formats.eur(line.amount),
                        Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleMedium.tabular(),
                        fontWeight = FontWeight.Bold,
                        color = if (line.incoming) colors.income else colors.ink,
                        maxLines = 1,
                    )
                }
                if (line.subtitle.isNotEmpty()) {
                    Text(line.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.muted)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Formats.dayMonth(m.date), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = colors.muted)
                    if (armed) {
                        Button(
                            onClick = { onDelete(m) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.neg, contentColor = Color.White),
                        ) { Text("Conferma eliminazione", style = MaterialTheme.typography.labelMedium) }
                    } else {
                        Surface(
                            onClick = { armed = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.6f),
                            contentColor = Color(0xFF4A5A70),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(AppIcons.Trash, contentDescription = "Elimina ${line.title}", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
