package it.elia.gestionespese.core

/** Riga della lista Movimenti (SPEC.md §5, Movimenti). */
data class MovementLine(
    val title: String,
    val subtitle: String,
    /** Importo mostrato, sempre ≥ 0. */
    val amount: Long,
    /** "+", "−" oppure "". */
    val sign: String,
    /** Da mostrare in verde. */
    val incoming: Boolean,
)

object MovementText {
    const val DELETED = "Budget eliminato"
    const val TO_ASSIGN = "Da assegnare"

    fun kindLabel(kind: MovementKind): String = when (kind) {
        MovementKind.STIPENDIO -> "Stipendio"
        MovementKind.ENTRATA -> "Entrata extra"
        MovementKind.SPESA -> "Spesa"
        MovementKind.ASSEGNA -> "Assegnazione"
        MovementKind.RETTIFICA -> "Rettifica totale conto"
        MovementKind.PRELEVA -> "Soldi tolti da un budget"
    }

    /** [nameOf] restituisce il nome del budget, o null se è stato eliminato. */
    fun describe(m: Movement, nameOf: (String) -> String?): MovementLine {
        val name = { id: String? -> id?.let(nameOf) ?: DELETED }
        val isIn = m.kind == MovementKind.STIPENDIO || m.kind == MovementKind.ENTRATA
        val note = m.note?.trim().orEmpty()
        // Lo stipendio mostra sempre il mese a cui si riferisce (può essere diverso da quello della data)
        var title = if (m.kind == MovementKind.STIPENDIO) "Stipendio di ${Formats.monthLong(m.month)}" else kindLabel(m.kind)
        var sub = ""
        when {
            m.kind == MovementKind.SPESA -> {
                title = note.ifEmpty { "Spesa" }
                sub = name(m.categoryId)
            }
            m.kind == MovementKind.PRELEVA -> {
                val from = m.allocations.entries.firstOrNull { it.value < 0 }?.key
                val to = m.allocations.entries.firstOrNull { it.value > 0 }?.key
                sub = "${name(from)} → ${if (to != null) name(to) else TO_ASSIGN}"
                if (note.isNotEmpty()) title += " · $note"
            }
            m.kind == MovementKind.RETTIFICA -> {
                sub = "Totale impostato a ${Formats.eur(m.total ?: 0)} · differenza in \"$TO_ASSIGN\""
            }
            m.allocations.isNotEmpty() || isIn -> {
                val parts = m.allocations.filterValues { it > 0 }.map { (id, v) -> "${name(id)} ${Formats.eur(v)}" }.toMutableList()
                if (isIn) {
                    val rest = m.amount - m.allocations.values.sum()
                    if (rest > 0) parts += "$TO_ASSIGN ${Formats.eur(rest)}"
                }
                sub = parts.joinToString(" · ")
                if (note.isNotEmpty()) title += " · $note"
            }
        }
        val amount = when (m.kind) {
            MovementKind.PRELEVA -> -(m.allocations.values.firstOrNull { it < 0 } ?: 0)
            MovementKind.ASSEGNA -> m.allocations.values.sum()
            MovementKind.RETTIFICA -> kotlin.math.abs(m.amount)
            else -> m.amount
        }
        val sign = when {
            m.kind == MovementKind.SPESA || (m.kind == MovementKind.RETTIFICA && m.amount < 0) -> "−"
            isIn || m.kind == MovementKind.RETTIFICA -> "+"
            else -> ""
        }
        return MovementLine(title, sub, amount, sign, isIn || (m.kind == MovementKind.RETTIFICA && m.amount > 0))
    }

    /** Vero se il movimento riguarda il budget (spesa o quota). */
    fun touches(m: Movement, categoryId: String): Boolean =
        m.categoryId == categoryId || m.allocations.containsKey(categoryId)
}
