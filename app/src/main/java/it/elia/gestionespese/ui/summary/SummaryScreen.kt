@file:OptIn(ExperimentalMaterial3Api::class)

package it.elia.gestionespese.ui.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.elia.gestionespese.core.Formats
import it.elia.gestionespese.core.MonthSummary
import it.elia.gestionespese.ui.UiState
import it.elia.gestionespese.ui.components.Amount
import it.elia.gestionespese.ui.components.Panel
import it.elia.gestionespese.ui.components.ProgressBar
import it.elia.gestionespese.ui.components.RowDivider
import it.elia.gestionespese.ui.components.ScreenTitle
import it.elia.gestionespese.ui.components.SectionTitle
import it.elia.gestionespese.ui.theme.AppTheme
import it.elia.gestionespese.ui.theme.Rubik
import it.elia.gestionespese.ui.theme.tabular
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min

@Composable
fun SummaryScreen(st: UiState, contentPadding: PaddingValues) {
    val periods = st.math.monthsWithData()
    var selectedText by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedText?.let { YearMonth.parse(it) }?.takeIf { it in periods }
        ?: st.math.currentPeriod.takeIf { it in periods }
        ?: periods.lastOrNull()
    val select = { p: YearMonth -> selectedText = p.toString() }

    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (selected == null) {
            item { Header(st, periods, null, select, summary = null) }
            item {
                Panel(Modifier.padding(horizontal = 20.dp)) {
                    SectionTitle("Ancora nessun dato")
                    Text("Dopo il primo stipendio e le prime spese qui trovi il riepilogo periodo per periodo e il grafico entrate/uscite.")
                }
            }
            return@LazyColumn
        }
        val s = st.math.monthSummary(selected)
        val series = st.math.chartMonths().map { st.math.monthSummary(it) }
        item { Header(st, periods, selected, select, s) }
        item { SpentPanel(st, s) }
        if (series.isNotEmpty()) item { ChartPanel(st, series, selected, select) }
        item { DetailPanel(st, s) }
    }
}

/** Testata pesca con titolo, periodi e il riquadro dei totali che la sovrasta. */
@Composable
private fun Header(st: UiState, periods: List<YearMonth>, selected: YearMonth?, onSelect: (YearMonth) -> Unit, summary: MonthSummary?) {
    val colors = AppTheme.colors
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .padding(bottom = if (summary != null) 44.dp else 0.dp)
                .background(colors.peachSoft, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
        )
        Column(
            Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = if (summary != null) 0.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenTitle("Riepilogo")
            if (selected != null) {
                Text(st.math.periodLabel(selected), color = colors.muted)
                val scroll = rememberScrollState(Int.MAX_VALUE)
                Row(Modifier.horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (p in periods) {
                        val on = p == selected
                        Surface(
                            onClick = { onSelect(p) },
                            shape = RoundedCornerShape(50),
                            color = if (on) colors.peach else Color.White.copy(alpha = 0.6f),
                            contentColor = Color(0xFF2E3E5C),
                        ) {
                            Text(
                                Formats.monthShort(p),
                                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
            if (summary != null) {
                Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stat("Entrate", summary.income, colors.income, Modifier.weight(1f))
                        Stat("Spese", summary.expenses, colors.ink, Modifier.weight(1f))
                        Stat("Differenza", summary.difference, colors.ink, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, cents: Long, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.muted)
        Amount(cents, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

/** Quanto è stato speso di quello che c'era in ogni budget nel periodo. */
@Composable
private fun SpentPanel(st: UiState, s: MonthSummary) {
    val colors = AppTheme.colors
    val barColors = listOf(colors.peach, colors.navy, colors.chartIn, colors.amber)
    Panel(Modifier.padding(horizontal = 20.dp)) {
        SectionTitle("Speso per budget")
        st.categories.forEachIndexed { i, c ->
            val m = s.perCategory[c.id]
            val spent = m?.spent ?: 0
            // Disponibile nel periodo = quello che resta a fine periodo + quello che è stato speso
            val available = max(spent, (m?.endBalance ?: 0) + spent)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Text(c.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${Formats.eur(spent)} di ${Formats.eur(available)}",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.muted,
                    )
                }
                ProgressBar(if (available > 0) spent.toFloat() / available else 0f, barColors[i % barColors.size], colors.sunk)
            }
        }
    }
}

/** Valore "tondo" per il massimo dell'asse, in euro. */
private fun niceMax(v: Double): Double {
    if (v <= 0) return 100.0
    val e = Math.pow(10.0, Math.floor(Math.log10(v)))
    val f = v / e
    return (if (f <= 1) 1.0 else if (f <= 2) 2.0 else if (f <= 2.5) 2.5 else if (f <= 5) 5.0 else 10.0) * e
}

@Composable
private fun ChartPanel(st: UiState, series: List<MonthSummary>, selected: YearMonth, onSelect: (YearMonth) -> Unit) {
    val colors = AppTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = Rubik, fontSize = 11.sp, color = colors.muted)
    val labelStrong = labelStyle.copy(color = colors.ink, fontWeight = FontWeight.SemiBold)
    val maxEuro = niceMax(series.maxOf { max(it.income, it.expenses) } / 100.0)
    val sel = series.firstOrNull { it.month == selected }
    Panel(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Entrate e spese", Modifier.weight(1f))
            Legend("Entrate", colors.chartIn)
            Legend("Spese", colors.chartOut, Modifier.padding(start = 12.dp))
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(series) {
                    detectTapGestures { pos ->
                        val i = (pos.x / (size.width.toFloat() / series.size)).toInt()
                        series.getOrNull(i)?.let { onSelect(it.month) }
                    }
                },
        ) {
            val padB = 24.dp.toPx()
            val ih = size.height - padB
            val gw = size.width / series.size
            val bw = min(14.dp.toPx(), (gw - 8.dp.toPx()) / 2)
            drawLine(colors.line, Offset(0f, ih), Offset(size.width, ih), strokeWidth = 1.dp.toPx())
            series.forEachIndexed { i, s ->
                val cx = gw * i + gw / 2
                val on = s.month == selected
                fun bar(x: Float, cents: Long, color: Color) {
                    val h = (cents / 100.0 / maxEuro * ih).toFloat()
                    if (h <= 0f) return
                    drawRoundRect(color, Offset(x, ih - h), Size(bw, h), CornerRadius(bw / 2, bw / 2))
                }
                bar(cx - bw - 2f, s.income, if (on) colors.chartInStrong else colors.chartIn)
                bar(cx + 2f, s.expenses, colors.chartOut)
                val t = measurer.measure(Formats.monthShort(s.month).substringBefore(' '), if (on) labelStrong else labelStyle)
                drawText(t, topLeft = Offset(cx - t.size.width / 2f, size.height - t.size.height))
            }
        }
        if (sel != null) {
            Text(
                "${st.math.periodLabel(sel.month)}: entrate ${Formats.eur(sel.income)} · spese ${Formats.eur(sel.expenses)}",
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun Legend(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Text(text, Modifier.padding(start = 5.dp), style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.muted)
    }
}

/** Tabella del periodo per budget, espandibile. */
@Composable
private fun DetailPanel(st: UiState, s: MonthSummary) {
    val colors = AppTheme.colors
    var open by rememberSaveable { mutableStateOf(false) }
    Panel(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Dettaglio per budget", Modifier.weight(1f))
            TextButton(onClick = { open = !open }) { Text(if (open) "Nascondi" else "Mostra", color = colors.accent) }
        }
        if (open) {
            val weights = listOf(1.4f, 1f, 1f, 1.1f)
            val header = listOf("Budget", "Entrato", "Speso", "Saldo fine")
            Row {
                header.forEachIndexed { i, h ->
                    Text(
                        h.uppercase(), Modifier.weight(weights[i]),
                        style = MaterialTheme.typography.labelSmall, color = colors.muted, fontWeight = FontWeight.SemiBold,
                        textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                    )
                }
            }
            RowDivider()
            val rows = st.categories.map { c ->
                val m = s.perCategory[c.id]
                listOf(c.name, m?.entered ?: 0L, m?.spent ?: 0L, m?.endBalance ?: 0L)
            }
            for (r in rows) TableRow(r, weights, bold = false)
            RowDivider()
            val tot = st.categories.mapNotNull { s.perCategory[it.id] }
            TableRow(listOf("Totale", tot.sumOf { it.entered }, tot.sumOf { it.spent }, tot.sumOf { it.endBalance }), weights, bold = true)
        }
    }
}

@Composable
private fun TableRow(cells: List<Any>, weights: List<Float>, bold: Boolean) {
    Row(Modifier.padding(vertical = 4.dp)) {
        cells.forEachIndexed { i, c ->
            val style = MaterialTheme.typography.bodySmall.tabular().copy(fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
            if (c is Long) {
                Text(
                    Formats.eur(c), Modifier.weight(weights[i]), style = style, textAlign = TextAlign.End,
                    color = if (c < 0) AppTheme.colors.neg else Color.Unspecified,
                )
            } else {
                Text(c.toString(), Modifier.weight(weights[i]), style = style)
            }
        }
    }
}
