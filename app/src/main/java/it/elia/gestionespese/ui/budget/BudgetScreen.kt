package it.elia.gestionespese.ui.budget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import it.elia.gestionespese.core.BudgetBadge
import it.elia.gestionespese.core.BudgetCard
import it.elia.gestionespese.core.Category
import it.elia.gestionespese.core.TextPart
import it.elia.gestionespese.ui.UiState
import it.elia.gestionespese.ui.components.AccountHeader
import it.elia.gestionespese.ui.components.Amount
import it.elia.gestionespese.ui.components.AppIcons
import it.elia.gestionespese.ui.components.Badge
import it.elia.gestionespese.ui.components.Jar
import it.elia.gestionespese.ui.components.Notice
import it.elia.gestionespese.ui.components.Panel
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
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        item { AccountHeader(st, actions.onReset, actions.onSalary, actions.onAssign) }
        if (cards.isEmpty()) {
            item { EmptyBudgets(actions) }
            return@LazyColumn
        }
        item {
            Notice(
                "Con ⇄ togli, svuoti o sposti i soldi di un budget; con ☰ vedi i suoi movimenti.",
                Modifier.padding(top = 8.dp),
            )
        }
        if (spese.isNotEmpty()) {
            item { GroupHeader("Spese", spese.sumOf { it.balance }, AppTheme.colors.spend) }
            items(spese, key = { it.category.id }) { BudgetRow(it, actions) }
        }
        if (obiettivi.isNotEmpty()) {
            item { GroupHeader("Obiettivi", obiettivi.sumOf { it.balance }, AppTheme.colors.goal, top = 18) }
            items(obiettivi, key = { it.category.id }) { BudgetRow(it, actions) }
        }
    }
}

@Composable
private fun GroupHeader(title: String, total: Long, color: Color, top: Int = 6) {
    Row(Modifier.fillMaxWidth().padding(top = top.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        SectionTitle(title, Modifier.padding(start = 10.dp).weight(1f))
        Amount(total, color = AppTheme.colors.muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyBudgets(actions: BudgetActions) {
    Panel(Modifier.padding(top = 12.dp)) {
        SectionTitle("Crea i tuoi budget")
        Text(
            "Ogni budget riceve una quota fissa a ogni stipendio e tiene tutto quello che non spendi, mese dopo mese. " +
                "I budget \"obiettivo\" accumulano fino a una soglia per una spesa futura.",
        )
        Button(onClick = actions.onCreateFirst, modifier = Modifier.fillMaxWidth()) { Text("Crea il primo budget") }
        OutlinedButton(onClick = actions.onImport, modifier = Modifier.fillMaxWidth()) { Text("Importa backup") }
        OutlinedButton(onClick = actions.onSeed, modifier = Modifier.fillMaxWidth()) { Text("Parti da un esempio modificabile") }
    }
}

@Composable
private fun BudgetRow(card: BudgetCard, actions: BudgetActions) {
    val colors = AppTheme.colors
    val c = card.category
    val border = when {
        card.negative -> colors.neg
        c.isGoal -> colors.goal.copy(alpha = 0.35f)
        else -> colors.line
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (c.isGoal) colors.goalTint else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Jar(card.level.layers, Modifier.align(Alignment.CenterVertically))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(c.name, fontWeight = FontWeight.SemiBold)
                Amount(card.balance, style = MaterialTheme.typography.titleLarge)
                Text(card.levelLine, style = MaterialTheme.typography.bodySmall.tabular(), color = colors.muted)
                for (line in card.lines) InfoLine(line)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (card.badge) {
                    BudgetBadge.SPESE -> Badge("Spese", colors.spendSoft, colors.spend)
                    BudgetBadge.OBIETTIVO -> Badge("Obiettivo", colors.goalSoft, colors.goal)
                    BudgetBadge.IN_ROSSO -> Badge("In rosso", colors.negSoft, colors.neg)
                    BudgetBadge.RAGGIUNTO -> Badge("Raggiunto", colors.accentSoft, colors.accent)
                }
                SquareButton(AppIcons.Swap, "Togli o sposta soldi da ${c.name}", colors.accentSoft, colors.accent) {
                    actions.onTake(c)
                }
                SquareButton(AppIcons.List, "Movimenti di ${c.name}", colors.spendSoft, colors.spend) {
                    actions.onShowMovements(c)
                }
            }
        }
    }
}

@Composable
private fun InfoLine(parts: List<TextPart>) {
    val colors = AppTheme.colors
    val text = buildAnnotatedString {
        for (p in parts) {
            val style = when {
                p.warn -> SpanStyle(color = colors.neg)
                p.highlight -> SpanStyle(color = colors.goal, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
                else -> SpanStyle()
            }
            withStyle(style) { append(p.text) }
        }
    }
    Text(text, style = MaterialTheme.typography.bodySmall.tabular(), color = colors.muted)
}

@Composable
private fun SquareButton(icon: ImageVector, description: String, background: Color, tint: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = background,
        modifier = Modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}
