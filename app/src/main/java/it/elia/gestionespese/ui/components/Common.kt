package it.elia.gestionespese.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.elia.gestionespese.core.Formats
import it.elia.gestionespese.ui.theme.AppTheme
import it.elia.gestionespese.ui.theme.tabular
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Card bianca arrotondata con ombra morbida: il contenitore di base dell'app. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = color,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/** Titolo grande di schermata ("Movimenti", "Riepilogo"…). */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
fun Notice(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
}

@Composable
fun ErrorText(text: String?) {
    if (!text.isNullOrEmpty()) Text(text, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.neg)
}

/** Etichetta maiuscola piccola, es. il periodo nei Movimenti. */
@Composable
fun SmallLabel(text: String, modifier: Modifier = Modifier, color: Color = AppTheme.colors.muted) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
    )
}

@Composable
fun Badge(text: String, background: Color, foreground: Color) {
    Text(
        text.uppercase(),
        modifier = Modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp),
        color = foreground,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

/** Importo con cifre allineate; rosso se [negativeRed] e negativo. */
@Composable
fun Amount(
    cents: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
    negativeRed: Boolean = true,
    prefix: String = "",
) {
    Text(
        prefix + Formats.eur(cents),
        modifier = modifier,
        style = style.tabular(),
        color = if (negativeRed && cents < 0) AppTheme.colors.neg else color,
        maxLines = 1,
    )
}

/** Barra di avanzamento arrotondata (0–1). */
@Composable
fun ProgressBar(fraction: Float, color: Color, track: Color, modifier: Modifier = Modifier, height: Int = 8) {
    Box(modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(RoundedCornerShape(50)).background(color))
    }
}

/** Pillola selezionabile (filtri, periodi). */
@Composable
fun PillChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) colors.blueSoft else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) colors.ink else colors.muted,
        modifier = modifier.heightIn(min = 40.dp),
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal, maxLines = 1)
        }
    }
}

/** Scelta tra poche opzioni in una barra a pillole (es. Stipendio / Extra / Altro). */
@Composable
fun <T> SegmentedPills(options: List<T>, selected: T, optionText: (T) -> String, onSelect: (T) -> Unit) {
    val colors = AppTheme.colors
    Row(
        Modifier.fillMaxWidth().background(colors.sunk, RoundedCornerShape(16.dp)).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (o in options) {
            val on = o == selected
            Surface(
                onClick = { onSelect(o) },
                shape = RoundedCornerShape(12.dp),
                color = if (on) colors.header else Color.Transparent,
                contentColor = if (on) colors.onHeader else colors.muted,
                modifier = Modifier.weight(1f).height(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(optionText(o), fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
                }
            }
        }
    }
}

/** Contenitore pieno arrotondato con etichetta piccola sopra il valore: lo stile dei campi dell'app. */
@Composable
private fun FieldBox(
    label: String,
    modifier: Modifier,
    background: Color,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 60.dp)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.muted)
        content()
    }
}

@Composable
private fun PlainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String?,
    style: TextStyle,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.ink),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(contentAlignment = if (style.textAlign == TextAlign.End) Alignment.CenterEnd else Alignment.CenterStart) {
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, style = style.copy(color = colors.muted.copy(alpha = 0.7f)))
                }
                inner()
            }
        },
    )
}

/**
 * Campo importo in euro: tastiera numerica, testo libero validato al salvataggio.
 * Con [label] è un riquadro pieno; senza è il campo compatto delle righe delle quote.
 */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    placeholder: String = "0,00",
    background: Color = AppTheme.colors.sunk,
) {
    if (label != null) {
        FieldBox(label, modifier, background) {
            PlainTextField(
                value, onValueChange, placeholder,
                MaterialTheme.typography.titleLarge.tabular().copy(fontWeight = FontWeight.Bold),
                KeyboardType.Decimal,
            )
        }
    } else {
        Box(
            modifier
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.5.dp, AppTheme.colors.line, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            PlainTextField(
                value, onValueChange, placeholder,
                MaterialTheme.typography.bodyLarge.tabular().copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End),
                KeyboardType.Decimal,
            )
        }
    }
}

@Composable
fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    FieldBox(label, modifier, AppTheme.colors.sunk) {
        PlainTextField(value, onValueChange, placeholder, MaterialTheme.typography.bodyLarge, KeyboardType.Text)
    }
}

/** Riquadro che si comporta come un pulsante (selezione, data). */
@Composable
private fun ClickableField(text: String, label: String, onClick: () -> Unit, modifier: Modifier, trailing: String) {
    FieldBox(label, modifier, AppTheme.colors.sunk, onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(trailing, color = AppTheme.colors.muted)
        }
    }
}

/** Elenco a discesa. */
@Composable
fun <T> SelectField(
    label: String,
    options: List<T>,
    selected: T,
    optionText: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        ClickableField(optionText(selected), label, { open = true }, Modifier.fillMaxWidth(), "▾")
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (o in options) {
                DropdownMenuItem(text = { Text(optionText(o)) }, onClick = {
                    open = false
                    onSelect(o)
                })
            }
        }
    }
}

/** Data con selettore a calendario; con [clearable] si può anche svuotare. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    clearable: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    ClickableField(date?.let(Formats::date) ?: "—", label, { open = true }, modifier.fillMaxWidth(), "")
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    if (clearable) {
                        TextButton(onClick = {
                            onChange(null)
                            open = false
                        }) { Text("Nessuna") }
                    }
                    TextButton(onClick = { open = false }) { Text("Annulla") }
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

/** Riga etichetta / importo; [emphasized] è il riquadro blu notte (es. "Resta in Da assegnare"). */
@Composable
fun SummaryRow(label: String, cents: Long, emphasized: Boolean = false, sign: Boolean = false) {
    val colors = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (emphasized) colors.navy else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label, Modifier.weight(1f),
            color = if (emphasized) colors.onNavy else colors.ink,
            fontWeight = if (emphasized) FontWeight.Medium else FontWeight.Normal,
        )
        Amount(
            cents,
            prefix = if (sign && cents > 0) "+" else "",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (emphasized) colors.onNavy else Color.Unspecified,
            negativeRed = !emphasized,
        )
    }
}

/** Contenitore a righe (le tabelline dei moduli). */
@Composable
fun BoxedRows(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppTheme.colors.sunk),
        content = content,
    )
}

@Composable
fun RowDivider() = HorizontalDivider(color = AppTheme.colors.line)

/** Pulsanti in fondo ai moduli: Annulla + azione principale (pesca, o rossa se [danger]). */
@Composable
fun SheetButtons(
    confirm: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    cancel: String = "Annulla",
    danger: Boolean = false,
) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, colors.line),
        ) { Text(cancel, color = colors.ink) }
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1.4f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = if (danger) {
                ButtonDefaults.buttonColors(containerColor = colors.neg, contentColor = Color.White)
            } else {
                ButtonDefaults.buttonColors(containerColor = colors.peach, contentColor = colors.onPeach)
            },
        ) { Text(confirm, fontWeight = FontWeight.SemiBold) }
    }
}
