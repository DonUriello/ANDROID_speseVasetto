package it.elia.gestionespese.core

import kotlin.math.max
import kotlin.math.roundToInt

enum class BudgetBadge { SPESE, OBIETTIVO, IN_ROSSO, RAGGIUNTO }

/** Pezzo di testo di una riga informativa: [highlight] in evidenza (colore obiettivo), [warn] in rosso. */
data class TextPart(val text: String, val highlight: Boolean = false, val warn: Boolean = false)

/** Contenuto della riga di un budget nella schermata Budget (SPEC.md §5, Budget). */
data class BudgetCard(
    val category: Category,
    val balance: Long,
    val level: JarLevel,
    val badge: BudgetBadge,
    /** Riga del livello, es. "Restano 90,00 € di 150,00 € · 60%". */
    val levelLine: String,
    /** Righe informative. */
    val lines: List<List<TextPart>>,
) {
    val negative: Boolean get() = balance < 0

    companion object {
        fun of(c: Category, balance: Long, math: BudgetMath): BudgetCard {
            val level = math.jarLevel(c, balance)
            val target = c.target
            val hasTarget = c.isGoal && target != null && target > 0
            val badge = when {
                balance < 0 -> BudgetBadge.IN_ROSSO
                hasTarget && balance >= target!! -> BudgetBadge.RAGGIUNTO
                c.isGoal -> BudgetBadge.OBIETTIVO
                else -> BudgetBadge.SPESE
            }
            val pct = level.percent.roundToInt()
            val lines = ArrayList<List<TextPart>>()
            val levelLine: String
            if (hasTarget) {
                levelLine = "$pct% di ${Formats.eur(target!!)}"
                val missing = target - balance
                val plan = math.goalPlan(c, balance)
                when {
                    missing <= 0 -> lines += listOf(TextPart("Pronta per la spesa"))
                    plan != null && plan.overdue ->
                        lines += listOf(TextPart("Scadenza superata: mancano ${Formats.eur(missing)}", warn = true))
                    else -> {
                        if (c.deadline != null && plan != null) {
                            lines += listOf(
                                TextPart("Scadenza ${Formats.date(c.deadline)} · ${Formats.salaries(plan.salaries)} prima della scadenza"),
                            )
                        }
                        val q = math.effectiveQuota(c, balance)
                        val forecast = math.fillForecast(c, balance)
                        if (q > 0 && forecast != null) {
                            val label = when {
                                c.isManualGoal -> "Quota tua"
                                plan != null -> "Quota calcolata"
                                else -> "Quota"
                            }
                            lines += listOf(TextPart("$label: "), TextPart(Formats.eur(q), highlight = true), TextPart(" a stipendio"))
                            val full = mutableListOf(
                                TextPart("Pieno con ${Formats.salaries(forecast.salariesNeeded)}, il ${Formats.date(forecast.date)}"),
                            )
                            if (forecast.late && plan != null) {
                                full += TextPart(" · ")
                                full += TextPart("dopo la scadenza, servono ${Formats.eur(plan.quota)} a stipendio", warn = true)
                            }
                            lines += full
                        } else if (plan != null) {
                            lines += listOf(
                                TextPart("Quota a zero: per arrivarci servono "),
                                TextPart(Formats.eur(plan.quota), highlight = true),
                                TextPart(" a stipendio"),
                            )
                        } else {
                            lines += listOf(TextPart("Quota a zero: lo riempi a mano"))
                        }
                    }
                }
            } else {
                levelLine = when (level.state) {
                    JarState.BELOW_ZERO -> "Sotto zero"
                    JarState.EMPTY_THIS_MONTH -> "Ancora vuoto questo mese"
                    JarState.NORMAL -> "Restano ${Formats.eur(max(0L, balance))} di ${Formats.eur(level.full)} · $pct%"
                }
                val q = math.effectiveQuota(c, balance)
                lines += listOf(TextPart(if (q > 0) "+${Formats.eur(q)} a stipendio" else "Nessuna quota fissa"))
            }
            return BudgetCard(c, balance, level, badge, levelLine, lines)
        }
    }
}
