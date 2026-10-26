@file:OptIn(ExperimentalMaterial3Api::class)

package it.elia.gestionespese.ui.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
import it.elia.gestionespese.ui.components.RowDivider
import it.elia.gestionespese.ui.components.SectionTitle
import it.elia.gestionespese.ui.components.SelectField
import it.elia.gestionespese.ui.components.SmallLabel
import it.elia.gestionespese.ui.theme.AppTheme
import it.elia.gestionespese.ui.theme.tabular
import java.time.YearMonth
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Composable
fun SummaryScreen(st: UiState, header: @Composable () -> Unit, contentPadding: PaddingValues) {
    val months = st.math.monthsWithData()
    var selectedText by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        item { header() }
        if (months.isEmpty()) {
            item {
                Panel {
                    SectionTitle("Ancora nessun dato")
                    Text("Dopo il primo stipendio e le prime spese qui trovi il riepilogo mese per mese e il grafico entrate/uscite.")
                }
            }
            return@LazyColumn
        }
        val current = YearMonth.from(st.today)
        val selected = selectedText?.let { YearMonth.parse(it) }?.takeIf { it in months }
            ?: if (current in months) current else months.last()
        val series = st.math.chartMonths().map { st.math.monthSummary(it) }
        if (series.isNotEmpty()) item { ChartPanel(series) }
        item { MonthPanel(st, months, selected) { selectedText = it.toString() } }
    }
}

@Composable
private fun ChartPanel(series: List<MonthSummary>) {
    val colors = AppTheme.colors
    var picked by remember(series.size) { mutableStateOf<Int?>(null) }
    var showTable by rememberSaveable { mutableStateOf(false) }
    Panel {
        SectionTitle("Entrate e uscite per mese")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem("Entrate", colors.chartIn)
            LegendItem("Spese", colors.chartOut)
        }
        BarChart(series, picked) { picked = it }
        val p = picked?.let(series::getOrNull)
        if (p != null) {
            Text(
                "${Formats.monthLong(p.month)}: entrate ${Formats.eur(p.income)} · spese ${Formats.eur(p.expenses)}",
                style = MaterialTheme.typography.bodySmall.tabular(),
            )
        } else {
            Text("Tocca un mese per vedere i valori.", style = MaterialTheme.typography.bodySmall, color = colors.muted)
        }
        TextButton(onClick = { showTable = !showTable }) {
            Text(if (showTable) "Nascondi la tabella" else "Mostra i valori in tabella", color = colors.muted)
        }
        if (showTable) {
            Table(
                header = listOf("Mese", "Entrate", "Spese", "Differenza"),
                rows = series.map { s ->
                    listOf(Cell.Text(Formats.monthLong(s.month)), Cell.Money(s.income), Cell.Money(s.expenses), Cell.Money(s.difference))
                },
            )
        }
    }
}

@Composable
private fun LegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Text(text, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
    }
}

/** Valore "tondo" per il massimo dell'asse (come `niceMax` del prototipo), in euro. */
private fun niceMax(v: Double): Double {
    if (v <= 0) return 100.0
    val e = 10.0.pow(floor(log10(v)))
    val f = v / e
    return (if (f <= 1) 1.0 else if (f <= 2) 2.0 else if (f <= 2.5) 2.5 else if (f <= 5) 5.0 else 10.0) * e
}

/** Euro interi con il punto delle migliaia, per l'asse del grafico. */
private fun axisLabel(euros: Long): String =
    euros.toString().reversed().chunked(3).joinToString(".").reversed() + " €"

@Composable
private fun BarChart(series: List<MonthSummary>, picked: Int?, onPick: (Int) -> Unit) {
    val colors = AppTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = colors.muted, fontFeatureSettings = "tnum")
    val maxEuro = niceMax(series.maxOf { max(it.income, it.expenses) } / 100.0)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(240.dp)
            .pointerInput(series) {
                detectTapGestures { pos ->
                    val padL = 56.dp.toPx()
                    val gw = (size.width - padL - 4.dp.toPx()) / series.size
                    val i = ((pos.x - padL) / gw).toInt()
                    if (i in series.indices) onPick(i)
                }
            },
    ) {
        val padL = 56.dp.toPx()
        val padR = 4.dp.toPx()
        val padT = 8.dp.toPx()
        val padB = 22.dp.toPx()
        val iw = size.width - padL - padR
        val ih = size.height - padT - padB
        val n = series.size
        val gw = iw / n
        val bw = max(4.dp.toPx(), min(18.dp.toPx(), (gw - 10.dp.toPx()) / 2))
        fun y(euro: Double) = padT + ih - (euro / maxEuro * ih).toFloat()

        for (k in 0..4) {
            val v = maxEuro * k / 4
            val yy = y(v)
            drawLine(
                colors.line, Offset(padL, yy), Offset(size.width - padR, yy), strokeWidth = 1.dp.toPx(),
                pathEffect = if (k > 0) PathEffect.dashPathEffect(floatArrayOf(4f, 6f)) else null,
            )
            val text = measurer.measure(axisLabel(v.toLong()), labelStyle)
            drawText(text, topLeft = Offset(padL - 6.dp.toPx() - text.size.width, yy - text.size.height / 2f))
        }
        val step = if (n > 8 && size.width < 520.dp.toPx()) 2 else 1
        series.forEachIndexed { i, s ->
            val cx = padL + gw * i + gw / 2
            if (picked == i) drawRect(colors.sunk, Offset(padL + gw * i, padT), Size(gw, ih))
            fun bar(x: Float, cents: Long, color: Color) {
                val h = (cents / 100.0 / maxEuro * ih).toFloat()
                if (h <= 0f) return
                drawRoundRect(color, Offset(x, padT + ih - h), Size(bw, h), CornerRadius(min(4.dp.toPx(), bw / 2)))
            }
            bar(cx - bw - 1f, s.income, colors.chartIn)
            bar(cx + 1f, s.expenses, colors.chartOut)
            if ((n - 1 - i) % step == 0) {
                val t = measurer.measure(Formats.monthShort(s.month), labelStyle)
                drawText(t, topLeft = Offset(cx - t.size.width / 2f, size.height - t.size.height))
            }
        }
    }
}

@Composable
private fun MonthPanel(st: UiState, months: List<YearMonth>, selected: YearMonth, onSelect: (YearMonth) -> Unit) {
    val s = st.math.monthSummary(selected)
    Panel {
        SectionTitle("Dettaglio del mese")
        SelectField("Mese", months.reversed(), selected, Formats::monthLong, onSelect)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("Entrate", s.income, Modifier.weight(1f))
            Stat("Spese", s.expenses, Modifier.weight(1f))
            Stat("Differenza", s.difference, Modifier.weight(1f))
        }
        val rows = st.categories.map { c ->
            val m = s.perCategory[c.id]
            listOf(Cell.Text(c.name), Cell.Money(m?.entered ?: 0), Cell.Money(m?.spent ?: 0), Cell.Money(m?.endBalance ?: 0))
        }
        val tot = st.categories.mapNotNull { s.perCategory[it.id] }
        Table(
            header = listOf("Budget", "Entrato", "Speso", "Saldo a fine mese"),
            rows = rows,
            footer = listOf(
                Cell.Text("Totale"),
                Cell.Money(tot.sumOf { it.entered }),
                Cell.Money(tot.sumOf { it.spent }),
                Cell.Money(tot.sumOf { it.endBalance }),
            ),
        )
    }
}

@Composable
private fun Stat(label: String, cents: Long, modifier: Modifier) {
    Column(modifier) {
        SmallLabel(label)
        Amount(cents, style = MaterialTheme.typography.titleSmall)
    }
}

private sealed interface Cell {
    data class Text(val text: String) : Cell
    data class Money(val cents: Long) : Cell
}

/** Tabella semplice: prima colonna testo, le altre importi allineati a destra. */
@Composable
private fun Table(header: List<String>, rows: List<List<Cell>>, footer: List<Cell>? = null) {
    val weights = listOf(1.4f) + List(header.size - 1) { 1f }
    Column {
        Row(Modifier.padding(vertical = 6.dp)) {
            header.forEachIndexed { i, h ->
                Text(
                    h.uppercase(),
                    Modifier.weight(weights[i]),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.muted,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                )
            }
        }
        RowDivider()
        for (r in rows) {
            TableRow(r, weights, bold = false)
            RowDivider()
        }
        if (footer != null) TableRow(footer, weights, bold = true)
    }
}

@Composable
private fun TableRow(cells: List<Cell>, weights: List<Float>, bold: Boolean) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        cells.forEachIndexed { i, c ->
            val style = MaterialTheme.typography.bodySmall.tabular().copy(fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
            when (c) {
                is Cell.Text -> Text(c.text, Modifier.weight(weights[i]), style = style)
                is Cell.Money -> Text(
                    Formats.eur(c.cents),
                    Modifier.weight(weights[i]),
                    style = style,
                    textAlign = TextAlign.End,
                    color = if (c.cents < 0) AppTheme.colors.neg else Color.Unspecified,
                )
            }
        }
    }
}
