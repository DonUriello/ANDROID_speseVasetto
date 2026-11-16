package it.elia.gestionespese.ui.budget

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.elia.gestionespese.core.BudgetCard
import it.elia.gestionespese.core.Category
import it.elia.gestionespese.core.Formats
import it.elia.gestionespese.core.Movement
import it.elia.gestionespese.core.MovementText
import it.elia.gestionespese.ui.UiState
import it.elia.gestionespese.ui.components.Amount
import it.elia.gestionespese.ui.components.AppIcons
import it.elia.gestionespese.ui.components.Jar
import it.elia.gestionespese.ui.components.Panel
import it.elia.gestionespese.ui.components.ProgressBar
import it.elia.gestionespese.ui.components.SectionTitle
import it.elia.gestionespese.ui.theme.AppFont
import it.elia.gestionespese.ui.theme.AppTheme
import it.elia.gestionespese.ui.theme.tabular
import kotlin.math.max
import kotlin.math.min

/** Azioni del dettaglio di un budget. */
class BudgetDetailActions(
    val onBack: () -> Unit,
    val onTake: (Category) -> Unit,
    val onExpense: (Category) -> Unit,
    val onAllMovements: (Category) -> Unit,
)

/** Dettaglio di un budget: avanzamento, spese per periodo, ultimi movimenti e azioni. */
@Composable
fun BudgetDetailScreen(st: UiState, category: Category, actions: BudgetDetailActions, contentPadding: PaddingValues) {
    BackHandler(onBack = actions.onBack)
    val colors = AppTheme.colors
    val index = st.categories.indexOfFirst { it.id == category.id }.coerceAtLeast(0)
    val card = BudgetCard.of(category, st.balance(category.id), st.math)
    val p = progressOf(card)
    val movements = st.data.movements
        .filter { MovementText.touches(it, category.id) }
        .sortedWith(compareByDescending<Movement> { it.date }.thenByDescending { it.createdAt })

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 80.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(onClick = actions.onBack, shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(AppIcons.Back, "Indietro", Modifier.size(20.dp)) }
                    }
                    Text(
                        "Dettaglio budget", Modifier.weight(1f).padding(end = 44.dp),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).background(tintColor(index), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Jar(card.level.layers, Modifier.size(26.dp, 40.dp))
                    }
                    Box(Modifier.weight(1f))
                    Text(
                        "${p.percent}%",
                        modifier = Modifier.background(colors.tealSoft, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
                        color = colors.income, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(category.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(card.levelLine, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                }
            }
            item {
                Panel {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Amount(card.balance, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        if (p.full > 0) Text(Formats.eur(p.full), style = MaterialTheme.typography.bodyMedium.tabular(), color = colors.muted)
                    }
                    ProgressBar(p.fraction, if (card.negative) colors.neg else barColor(index), colors.sunk, height = 6)
                    for (line in card.lines) {
                        val text = buildAnnotatedString {
                            for (part in line) {
                                val style = when {
                                    part.warn -> SpanStyle(color = colors.neg)
                                    part.highlight -> SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.ink)
                                    else -> SpanStyle()
                                }
                                withStyle(style) { append(part.text) }
                            }
                        }
                        Text(text, style = MaterialTheme.typography.bodySmall.tabular(), color = colors.muted)
                    }
                }
            }
            item { SpendingPanel(st, category) }
            item {
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Movimenti", Modifier.weight(1f))
                    if (movements.size > 6) {
                        androidx.compose.material3.TextButton(onClick = { actions.onAllMovements(category) }) {
                            Text("Vedi tutti", color = colors.ink)
                        }
                    }
                }
            }
            if (movements.isEmpty()) {
                item { Text("Nessun movimento per questo budget.", style = MaterialTheme.typography.bodyMedium, color = colors.muted) }
            }
            items(movements.take(6), key = { it.id }) { m -> DetailMovementRow(st, m, category) }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = contentPadding.calculateBottomPadding() + 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = { actions.onTake(category) },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
            ) { Text("Togli o sposta", color = colors.ink) }
            Button(
                onClick = { actions.onExpense(category) },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = colors.peach, contentColor = colors.onPeach),
            ) {
                Icon(AppIcons.Minus, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Spesa", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Grafico a barre delle spese del budget negli ultimi periodi (l'ultimo in evidenza). */
@Composable
private fun SpendingPanel(st: UiState, category: Category) {
    val colors = AppTheme.colors
    val periods = st.math.chartMonths().takeLast(6)
    if (periods.isEmpty()) return
    val spent = periods.map { st.math.monthSummary(it).perCategory[category.id]?.spent ?: 0L }
    val avg = spent.sum() / spent.size
    val maxV = max(1L, spent.maxOrNull() ?: 1L)
    val measurer = rememberTextMeasurer()
    val label = TextStyle(fontFamily = AppFont, fontSize = 11.sp, color = colors.muted)
    Panel {
        Column {
            SectionTitle("Spese per periodo")
            Text("Media ${Formats.eur(avg)} a periodo", style = MaterialTheme.typography.bodySmall, color = colors.muted)
        }
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val padB = 20.dp.toPx()
            val ih = size.height - padB
            val gw = size.width / periods.size
            val bw = min(34.dp.toPx(), gw * 0.55f)
            periods.forEachIndexed { i, p ->
                val cx = gw * i + gw / 2
                val h = max(6.dp.toPx(), (spent[i].toFloat() / maxV) * ih)
                val last = i == periods.size - 1
                drawRoundRect(
                    if (last) colors.chartOut else colors.chartOut.copy(alpha = 0.25f),
                    Offset(cx - bw / 2, ih - h), Size(bw, h), CornerRadius(bw / 2, bw / 2),
                )
                val t = measurer.measure(Formats.monthShort(p).substringBefore(' '), label)
                drawText(t, topLeft = Offset(cx - t.size.width / 2f, size.height - t.size.height))
            }
        }
    }
}

@Composable
private fun DetailMovementRow(st: UiState, m: Movement, category: Category) {
    val colors = AppTheme.colors
    val line = MovementText.describe(m) { st.category(it)?.name }
    // Dal punto di vista del budget: spesa o soldi tolti = uscita, quota ricevuta = entrata
    val delta = if (m.categoryId == category.id) -m.amount else m.allocations[category.id] ?: 0L
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).background(if (delta >= 0) colors.tealSoft else colors.sunk, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (delta >= 0) AppIcons.Plus else AppIcons.Minus, null, Modifier.size(18.dp), tint = if (delta >= 0) colors.income else colors.ink)
            }
            Column(Modifier.weight(1f)) {
                Text(line.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(Formats.date(m.date), style = MaterialTheme.typography.bodySmall, color = colors.muted)
            }
            Text(
                (if (delta > 0) "+" else if (delta < 0) "−" else "") + Formats.eur(kotlin.math.abs(delta)),
                style = MaterialTheme.typography.titleSmall.tabular(), fontWeight = FontWeight.SemiBold,
                color = if (delta > 0) colors.income else colors.ink,
            )
        }
    }
}
