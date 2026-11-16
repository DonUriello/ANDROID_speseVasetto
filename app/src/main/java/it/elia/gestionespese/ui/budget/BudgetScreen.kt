package it.elia.gestionespese.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.elia.gestionespese.core.BudgetBadge
import it.elia.gestionespese.core.BudgetCard
import it.elia.gestionespese.core.Category
import it.elia.gestionespese.core.Formats
import it.elia.gestionespese.ui.UiState
import it.elia.gestionespese.ui.components.Amount
import it.elia.gestionespese.ui.components.AppIcons
import it.elia.gestionespese.ui.components.Badge
import it.elia.gestionespese.ui.components.Jar
import it.elia.gestionespese.ui.components.Panel
import it.elia.gestionespese.ui.components.ProgressBar
import it.elia.gestionespese.ui.components.SectionTitle
import it.elia.gestionespese.ui.theme.AppTheme
import it.elia.gestionespese.ui.theme.tabular

/** Azioni della schermata Budget. */
class BudgetActions(
    val onReset: () -> Unit,
    val onIncome: () -> Unit,
    val onExpense: () -> Unit,
    val onAssign: () -> Unit,
    val onOpen: (Category) -> Unit,
    val onCreateFirst: () -> Unit,
    val onSeed: () -> Unit,
    val onImport: () -> Unit,
)

@Composable
fun BudgetScreen(st: UiState, actions: BudgetActions, contentPadding: PaddingValues) {
    val cards = st.categories.map { BudgetCard.of(it, st.balance(it.id), st.math) }
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Gestione spese", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                val p = st.math.currentPeriod
                Text(
                    Formats.range(st.math.periodStart(p), st.math.periodEnd(p)),
                    style = MaterialTheme.typography.labelLarge,
                    color = AppTheme.colors.muted,
                )
            }
        }
        item { BalanceCard(st, actions) }
        if (cards.isEmpty()) {
            item { EmptyBudgets(actions) }
            return@LazyColumn
        }
        item {
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("I tuoi budget", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Amount(cards.sumOf { it.balance }, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.muted)
            }
        }
        itemsIndexed(cards, key = { _, c -> c.category.id }) { i, c -> BudgetItem(c, i, actions) }
    }
}

/** Card nera del saldo con le azioni principali, come la card "My balance". */
@Composable
private fun BalanceCard(st: UiState, actions: BudgetActions) {
    val colors = AppTheme.colors
    val b = st.balances
    val hasCats = st.categories.isNotEmpty()
    Surface(shape = RoundedCornerShape(24.dp), color = colors.header, contentColor = colors.onHeader, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Da assegnare ${Formats.eur(b.toAssign)}",
                modifier = Modifier.background(
                    if (b.toAssign > 0) colors.amber else colors.onHeader.copy(alpha = 0.15f),
                    RoundedCornerShape(50),
                ).padding(horizontal = 12.dp, vertical = 5.dp),
                color = if (b.toAssign > 0) colors.onAmber else colors.onHeader,
                style = MaterialTheme.typography.labelLarge.tabular(),
                fontWeight = FontWeight.SemiBold,
            )
            Text("Totale conto", Modifier.padding(top = 6.dp), color = colors.onHeaderMuted, style = MaterialTheme.typography.bodyMedium)
            Amount(
                b.totalAccount,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = colors.onHeader,
                negativeRed = false,
            )
            Text(
                "${Formats.eur(b.totalInBudgets)} nei budget",
                color = colors.onHeaderMuted,
                style = MaterialTheme.typography.bodySmall.tabular(),
            )
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                RoundAction(AppIcons.Plus, "Entrata", hasCats, actions.onIncome)
                RoundAction(AppIcons.Minus, "Spesa", hasCats, actions.onExpense)
                RoundAction(AppIcons.Assign, "Assegna", hasCats && b.toAssign > 0, actions.onAssign)
                RoundAction(AppIcons.Reset, "Rettifica", true, actions.onReset)
            }
        }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = colors.onHeader.copy(alpha = if (enabled) 0.14f else 0.06f),
            contentColor = colors.onHeader.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp)) }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onHeader.copy(alpha = if (enabled) 0.9f else 0.4f))
    }
}

@Composable
private fun EmptyBudgets(actions: BudgetActions) {
    Panel {
        SectionTitle("Crea i tuoi budget")
        Text(
            "Ogni budget riceve una quota fissa a ogni stipendio e tiene tutto quello che non spendi, periodo dopo periodo. " +
                "I budget \"obiettivo\" accumulano fino a una soglia per una spesa futura.",
        )
        Button(
            onClick = actions.onCreateFirst,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.peach, contentColor = AppTheme.colors.onPeach),
        ) { Text("Crea il primo budget", fontWeight = FontWeight.SemiBold) }
        OutlinedButton(onClick = actions.onImport, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(50)) {
            Text("Importa backup", color = AppTheme.colors.ink)
        }
        OutlinedButton(onClick = actions.onSeed, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(50)) {
            Text("Parti da un esempio modificabile", color = AppTheme.colors.ink)
        }
    }
}

/** Dati comuni all'elenco e al dettaglio: riferimento ("di Y"), colore della barra e testo sotto la barra. */
internal data class BudgetProgress(val full: Long, val fraction: Float, val percent: Int, val caption: String)

internal fun progressOf(card: BudgetCard): BudgetProgress {
    val pct = card.level.percent.toInt()
    val caption = card.lines.firstOrNull()?.joinToString("") { it.text } ?: card.levelLine
    return BudgetProgress(card.level.full, (card.level.percent / 100).toFloat(), pct, caption)
}

@Composable
internal fun barColor(index: Int): Color = AppTheme.colors.bars[index % AppTheme.colors.bars.size]

@Composable
internal fun tintColor(index: Int): Color {
    val c = AppTheme.colors
    return listOf(c.tealSoft, c.peachSoft, c.blueSoft, c.amberSoft)[index % 4]
}

/** Riga di un budget: icona col vasetto, nome, "saldo / riferimento", barra e percentuale. */
@Composable
private fun BudgetItem(card: BudgetCard, index: Int, actions: BudgetActions) {
    val colors = AppTheme.colors
    val c = card.category
    val p = progressOf(card)
    Surface(
        onClick = { actions.onOpen(c) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = colors.ink,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).background(tintColor(index), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Jar(card.level.layers, Modifier.size(24.dp, 36.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(c.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (c.isGoal) "Obiettivo" else "Spese", style = MaterialTheme.typography.bodySmall, color = colors.muted)
                }
                when (card.badge) {
                    BudgetBadge.IN_ROSSO -> Badge("In rosso", colors.negSoft, colors.neg)
                    BudgetBadge.RAGGIUNTO -> Badge("Raggiunto", colors.tealSoft, colors.income)
                    else -> Unit
                }
                Icon(AppIcons.ChevronRight, contentDescription = "Apri ${c.name}", tint = colors.muted, modifier = Modifier.size(20.dp))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Amount(card.balance, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                if (p.full > 0) {
                    Text(Formats.eur(p.full), style = MaterialTheme.typography.bodyMedium.tabular(), color = colors.muted)
                }
            }
            ProgressBar(p.fraction, if (card.negative) colors.neg else barColor(index), colors.sunk, height = 6)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (p.full > 0) p.caption else card.levelLine,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("${p.percent}%", style = MaterialTheme.typography.bodySmall.tabular(), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
