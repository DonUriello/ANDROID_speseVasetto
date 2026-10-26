package it.elia.gestionespese.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.elia.gestionespese.core.Formats
import it.elia.gestionespese.ui.UiState
import it.elia.gestionespese.ui.theme.AppTheme
import it.elia.gestionespese.ui.theme.tabular
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Riquadro con bordo sottile, come i pannelli del prototipo. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AppTheme.colors.line),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
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

/** Etichetta maiuscola piccola, es. il nome del mese nei Movimenti. */
@Composable
fun SmallLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = AppTheme.colors.muted,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
    )
}

@Composable
fun Badge(text: String, background: Color, foreground: Color) {
    Text(
        text.uppercase(),
        modifier = Modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 7.dp, vertical = 2.dp),
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
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
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

/** Campo importo in euro: tastiera numerica, testo libero validato al momento del salvataggio. */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    placeholder: String = "0,00",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it) } },
        placeholder = { Text(placeholder) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.tabular().copy(textAlign = TextAlign.End),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Campo di sola lettura che si comporta come un pulsante. */
@Composable
private fun ClickableField(text: String, label: String, onClick: () -> Unit, modifier: Modifier, trailing: String) {
    Box(modifier) {
        OutlinedTextField(
            value = text,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Text(trailing, color = AppTheme.colors.muted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable(onClick = onClick))
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
    ClickableField(date?.let(Formats::date) ?: "—", label, { open = true }, modifier.fillMaxWidth(), "📅")
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

/** Riga etichetta / valore in un riquadro di riepilogo (es. "Resta in Da assegnare"). */
@Composable
fun SummaryRow(label: String, cents: Long, emphasized: Boolean = false, sign: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (emphasized) AppTheme.colors.sunk else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal)
        Amount(cents, prefix = if (sign && cents > 0) "+" else "", style = MaterialTheme.typography.bodyLarge)
    }
}

/** Contenitore a righe con bordo (le tabelline dei moduli). */
@Composable
fun BoxedRows(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, AppTheme.colors.line),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
fun RowDivider() = HorizontalDivider(color = AppTheme.colors.line)

/** Pulsanti in fondo ai moduli: Annulla + azione principale. */
@Composable
fun SheetButtons(
    confirm: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    cancel: String = "Annulla",
    danger: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(cancel) }
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            colors = if (danger) {
                ButtonDefaults.buttonColors(containerColor = AppTheme.colors.neg, contentColor = Color.White)
            } else {
                ButtonDefaults.buttonColors()
            },
        ) { Text(confirm) }
    }
}

/** Testata: totali del conto e azioni principali (SPEC.md §5, Struttura generale). */
@Composable
fun AccountHeader(
    st: UiState,
    onReset: () -> Unit,
    onSalary: () -> Unit,
    onAssign: () -> Unit,
) {
    val b = st.balances
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SmallLabel("Gestione spese")
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.Bottom) {
            Text("Totale conto", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Amount(b.totalAccount, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        }
        RowDivider()
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Totale nei budget", Modifier.weight(1f), color = AppTheme.colors.muted)
            Amount(b.totalInBudgets, style = MaterialTheme.typography.titleMedium)
        }
        RowDivider()
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Da assegnare", Modifier.weight(1f), color = AppTheme.colors.muted)
            Amount(
                b.toAssign,
                style = MaterialTheme.typography.titleMedium,
                color = if (b.toAssign > 0) AppTheme.colors.ok else Color.Unspecified,
            )
        }
        RowDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onReset) { Text("Reimposta totale iniziale", color = AppTheme.colors.muted) }
        }
        val hasCats = st.categories.isNotEmpty()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSalary, enabled = hasCats, modifier = Modifier.weight(1f)) { Text("+ Stipendio") }
            OutlinedButton(onClick = onAssign, enabled = hasCats && b.toAssign > 0, modifier = Modifier.weight(1f)) {
                Text("Assegna dal Da assegnare", textAlign = TextAlign.Center)
            }
        }
    }
}
