package it.elia.gestionespese.ui.budget

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import it.elia.gestionespese.core.BudgetBadge
import it.elia.gestionespese.core.BudgetCard
import it.elia.gestionespese.core.Category
import it.elia.gestionespese.core.Formats
import it.elia.gestionespese.core.TextPart
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
    val onSalary: () -> Unit,
    val onAssign: () -> Unit,
    val onTake: (Category) -> Unit,
    val onShowMovements: (Category) -> Unit,
    val onCreateFirst: () -> Unit,
    val onSeed: () -> Unit,
    val onImport: () -> Unit,
)

@Composable
fun BudgetScreen(st: UiState, actions: BudgetActions, contentPadding: PaddingValues) {
    val cards = st.categories.map { BudgetCard.of(it, st.balance(it.id), st.math) }
    val spese = cards.filter { !it.category.isGoal }
    val obiettivi = cards.filter { it.category.isGoal }
    val colors = AppTheme.colors
    val pastels = listOf(colors.peachSoft, colors.tealSoft, colors.amberSoft, colors.blueSoft)
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BudgetHeader(st, actions) }
        if (cards.isEmpty()) {
            item { EmptyBudgets(actions) }
            return@LazyColumn
        }
        if (spese.isNotEmpty()) {
            item { GroupHeader("Spese", spese.sumOf { it.balance }) }
            itemsIndexed(spese, key = { _, c -> c.category.id }) { i, c ->
                SpendRow(c, pastels[i % pastels.size], actions, Modifier.padding(horizontal = 20.dp))
            }
        }
        if (obiettivi.isNotEmpty()) {
            item { GroupHeader("Obiettivi", obiettivi.sumOf { it.balance }) }
            itemsIndexed(obiettivi, key = { _, c -> c.category.id }) { _, c ->
                GoalRow(c, actions, Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}

/** Testata teal: totali del conto, periodo in corso e azioni principali. */
@Composable
private fun BudgetHeader(st: UiState, actions: BudgetActions) {
    val colors = AppTheme.colors
    val b = st.balances
    val hasCats = st.categories.isNotEmpty()
    val period = st.math.currentPeriod
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.header, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "GESTIONE SPESE", Modifier.weight(1f),
                color = colors.onHeader, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            )
            Text(
                Formats.range(st.math.periodStart(period), st.math.periodEnd(period)),
                modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
                color = colors.onHeader, style = MaterialTheme.typography.labelMedium,
            )
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Totale conto", Modifier.weight(1f), color = colors.onHeader.copy(alpha = 0.9f))
                TextButton(onClick = actions.onReset) {
                    Text("Reimposta", color = colors.onHeader, style = MaterialTheme.typography.labelMedium)
                }
            }
            Amount(
                b.totalAccount,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = colors.onHeader,
                negativeRed = false,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(
                Modifier.weight(1f).background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text("Nei budget", color = colors.onHeader.copy(alpha = 0.9f), style = MaterialTheme.typography.labelMedium)
                Amount(b.totalInBudgets, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.onHeader, negativeRed = false)
            }
            Column(
                Modifier.weight(1f).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text("Da assegnare", color = colors.muted, style = MaterialTheme.typography.labelMedium)
                Amount(
                    b.toAssign,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (b.toAssign > 0) colors.income else colors.ink,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = actions.onSalary,
                enabled = hasCats,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.amber, contentColor = colors.onAmber),
            ) { Text("+ Stipendio", fontWeight = FontWeight.SemiBold) }
            OutlinedButton(
                onClick = actions.onAssign,
                enabled = hasCats && b.toAssign > 0,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, colors.onHeader.copy(alpha = if (hasCats && b.toAssign > 0) 0.8f else 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onHeader, disabledContentColor = colors.onHeader.copy(alpha = 0.5f)),
            ) { Text("Assegna") }
        }
    }
}

@Composable
private fun GroupHeader(title: String, total: Long) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Amount(total, color = AppTheme.colors.muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyBudgets(actions: BudgetActions) {
    Panel(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        SectionTitle("Crea i tuoi budget")
        Text(
            "Ogni budget riceve una quota fissa a ogni stipendio e tiene tutto quello che non spendi, periodo dopo periodo. " +
                "I budget \"obiettivo\" accumulano fino a una soglia per una spesa futura.",
        )
        Button(
            onClick = actions.onCreateFirst,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.peach, contentColor = AppTheme.colors.onPeach),
        ) { Text("Crea il primo budget", fontWeight = FontWeight.SemiBold) }
        OutlinedButton(onClick = actions.onImport, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
            Text("Importa backup")
        }
        OutlinedButton(onClick = actions.onSeed, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
            Text("Parti da un esempio modificabile")
        }
    }
}

/** Budget di spesa: card pastello con il vasetto. */
@Composable
private fun SpendRow(card: BudgetCard, background: Color, actions: BudgetActions, modifier: Modifier) {
    val colors = AppTheme.colors
    val c = card.category
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (card.negative) colors.negSoft else background,
        contentColor = colors.ink,
        border = if (card.negative) BorderStroke(1.5.dp, colors.neg) else null,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Jar(card.level.layers)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(c.name, fontWeight = FontWeight.SemiBold)
                    if (card.badge == BudgetBadge.IN_ROSSO) Badge("In rosso", colors.neg, Color.White)
                }
                Amount(card.balance, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text(card.levelLine, style = MaterialTheme.typography.bodySmall.tabular(), color = colors.muted)
                for (line in card.lines) InfoLine(line, colors.muted, colors.ink, colors.neg)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SquareButton(AppIcons.Swap, "Togli o sposta soldi da ${c.name}") { actions.onTake(c) }
                SquareButton(AppIcons.List, "Movimenti di ${c.name}") { actions.onShowMovements(c) }
            }
        }
    }
}

/** Obiettivo: card blu notte con barra di avanzamento verso la soglia. */
@Composable
private fun GoalRow(card: BudgetCard, actions: BudgetActions, modifier: Modifier) {
    val colors = AppTheme.colors
    val c = card.category
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.navy,
        contentColor = colors.onNavy,
        border = if (card.negative) BorderStroke(1.5.dp, colors.neg) else null,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                when (card.badge) {
                    BudgetBadge.IN_ROSSO -> Badge("In rosso", colors.neg, Color.White)
                    BudgetBadge.RAGGIUNTO -> Badge("Raggiunto", colors.tealSoft, colors.ink)
                    else -> Badge("Obiettivo", colors.amber, colors.onAmber)
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Amount(
                    card.balance, Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.onNavy, negativeRed = false,
                )
                Text(card.levelLine, style = MaterialTheme.typography.bodySmall.tabular(), color = colors.onNavyMuted)
            }
            if (c.target != null && c.target!! > 0) {
                ProgressBar((card.level.percent / 100).toFloat(), colors.amber, Color.White.copy(alpha = 0.2f))
            }
            for (line in card.lines) InfoLine(line, colors.onNavyMuted, colors.amber, colors.warnOnNavy)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                SquareButton(AppIcons.Swap, "Togli o sposta soldi da ${c.name}", light = false) { actions.onTake(c) }
                SquareButton(AppIcons.List, "Movimenti di ${c.name}", light = false) { actions.onShowMovements(c) }
            }
        }
    }
}

@Composable
private fun InfoLine(parts: List<TextPart>, base: Color, highlight: Color, warn: Color) {
    val text = buildAnnotatedString {
        for (p in parts) {
            val style = when {
                p.warn -> SpanStyle(color = warn)
                p.highlight -> SpanStyle(color = highlight, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
                else -> SpanStyle()
            }
            withStyle(style) { append(p.text) }
        }
    }
    Text(text, style = MaterialTheme.typography.bodySmall.tabular(), color = base)
}

@Composable
private fun SquareButton(icon: ImageVector, description: String, light: Boolean = true, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (light) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.16f),
        contentColor = if (light) Color(0xFF2E3E5C) else colors.onNavy,
        modifier = Modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
        }
    }
}
