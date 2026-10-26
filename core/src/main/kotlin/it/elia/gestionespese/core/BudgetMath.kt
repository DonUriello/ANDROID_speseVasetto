package it.elia.gestionespese.core

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Saldi dei budget e del "Da assegnare" (SPEC.md §4.1). */
data class Balances(
    /** Saldo per categoryId; include anche budget eliminati con saldo ≠ 0. */
    val byCategory: Map<String, Long>,
    val toAssign: Long,
) {
    val totalInBudgets: Long get() = byCategory.values.sum()
    val totalAccount: Long get() = totalInBudgets + toAssign

    fun of(categoryId: String): Long = byCategory[categoryId] ?: 0L
}

/** Piano di un obiettivo con soglia e scadenza (SPEC.md §4.3). */
data class GoalPlan(
    /** Stipendi che arrivano entro la scadenza. */
    val salaries: Int,
    /** Quanto manca alla soglia. */
    val missing: Long,
    /** Quota a stipendio necessaria per arrivarci. */
    val quota: Long,
    val overdue: Boolean,
)

/** Previsione di riempimento di un obiettivo (SPEC.md §4.7). */
data class FillForecast(
    /** Stipendi necessari. */
    val salariesNeeded: Int,
    /** Data dello stipendio con cui l'obiettivo è pieno. */
    val date: LocalDate,
    /** Pieno dopo la scadenza. */
    val late: Boolean,
)

/** Proposta di smistamento dello stipendio (SPEC.md §4.5). */
data class SalarySplit(
    /** Quota proposta per categoryId, nell'ordine dei budget. */
    val proposals: Map<String, Long>,
    val toAssign: Long,
)

enum class JarState {
    /** Livello calcolato normalmente. */
    NORMAL,
    /** Nessun "pieno" questo mese e saldo ≥ 0. */
    EMPTY_THIS_MONTH,
    /** Nessun "pieno" questo mese e saldo negativo. */
    BELOW_ZERO,
}

/** Livello del vasetto (SPEC.md §4.6). */
data class JarLevel(
    /** 0–100. */
    val percent: Double,
    /** Riferimento del 100%: soglia per gli obiettivi, "pieno" del mese per gli altri. */
    val full: Long,
    val state: JarState,
) {
    /** Strati di monete da disegnare (0–10). */
    val layers: Int get() = (percent / 10).roundToInt()
}

data class CategoryMonth(
    /** Allocation positive ricevute nel mese, escluso PRELEVA. */
    val entered: Long,
    /** Somma delle SPESA del mese. */
    val spent: Long,
    /** Saldo calcolato fino al mese incluso. */
    val endBalance: Long,
)

/** Riepilogo mensile (SPEC.md §4.8). */
data class MonthSummary(
    val month: YearMonth,
    val income: Long,
    val expenses: Long,
    val perCategory: Map<String, CategoryMonth>,
) {
    val difference: Long get() = income - expenses
}

/**
 * Regole di calcolo dell'app (SPEC.md §4). Classe pura: nessuna dipendenza da Android.
 * Lavora su una fotografia dei movimenti, del giorno di stipendio e della data di oggi.
 */
class BudgetMath(
    movements: List<Movement>,
    private val payday: Int,
    private val today: LocalDate,
) {
    private val movements: List<Movement> =
        movements.sortedWith(compareBy<Movement> { it.date }.thenBy { it.createdAt })

    private val currentMonth: YearMonth = YearMonth.from(today)

    // ---------- 4.1 Saldi ----------

    /** Saldi considerando i movimenti fino al mese [upTo] incluso (tutti se null). */
    fun balances(upTo: YearMonth? = null): Balances {
        val byCategory = LinkedHashMap<String, Long>()
        var toAssign = 0L
        for (m in movements) {
            if (upTo != null && YearMonth.from(m.date) > upTo) continue
            if (m.kind in INCOMING) toAssign += m.amount
            for ((cat, v) in m.allocations) {
                byCategory[cat] = (byCategory[cat] ?: 0L) + v
                toAssign -= v
            }
            if (m.kind == MovementKind.SPESA && m.categoryId != null) {
                byCategory[m.categoryId] = (byCategory[m.categoryId] ?: 0L) - m.amount
            }
        }
        return Balances(byCategory, toAssign)
    }

    // ---------- 4.2 Prossimi stipendi ----------

    /** Vero se nel mese corrente è già registrato uno STIPENDIO. */
    val salaryReceivedThisMonth: Boolean =
        this.movements.any { it.kind == MovementKind.STIPENDIO && YearMonth.from(it.date) == currentMonth }

    /**
     * Date dei prossimi stipendi, uno al mese nel giorno di arrivo (nei mesi corti l'ultimo giorno).
     * Il primo è quello del mese corrente se non è ancora registrato, altrimenti quello del mese dopo.
     */
    fun nextPaydays(limit: Int, until: LocalDate? = null): List<LocalDate> {
        var month = if (salaryReceivedThisMonth) currentMonth.plusMonths(1) else currentMonth
        val out = ArrayList<LocalDate>()
        while (out.size < limit) {
            val date = paydayOf(month)
            if (until != null && date > until) break
            out += date
            month = month.plusMonths(1)
        }
        return out
    }

    /** Giorno di stipendio nel mese indicato. */
    fun paydayOf(month: YearMonth): LocalDate = month.atDay(min(payday, month.lengthOfMonth()))

    /** Stipendi che arrivano entro la scadenza (giorno incluso). */
    fun salariesUntil(deadline: LocalDate): Int = nextPaydays(MAX_PAYDAYS, deadline).size

    // ---------- 4.3 Piano obiettivo ----------

    /** Piano per un obiettivo con soglia > 0 e scadenza; null negli altri casi. */
    fun goalPlan(category: Category, balance: Long): GoalPlan? {
        val target = category.target
        val deadline = category.deadline
        if (!category.isGoal || target == null || target <= 0 || deadline == null) return null
        val missing = max(0L, target - balance)
        val n = salariesUntil(deadline)
        val quota = when {
            missing == 0L -> 0L
            n > 0 -> ceilDiv(missing, n.toLong())
            else -> missing
        }
        return GoalPlan(salaries = n, missing = missing, quota = quota, overdue = n == 0 && missing > 0)
    }

    // ---------- 4.4 Quota effettiva ----------

    fun effectiveQuota(category: Category, balance: Long): Long {
        if (category.isManualGoal) return category.quota
        return goalPlan(category, balance)?.quota ?: category.quota
    }

    // ---------- 4.5 Smistamento dello stipendio ----------

    /** Quote proposte per uno stipendio di [amount], riempiendo i budget nell'ordine di `order`. */
    fun salarySplit(amount: Long, categories: List<Category>, balances: Balances): SalarySplit {
        val proposals = LinkedHashMap<String, Long>()
        var left = amount
        for (c in categories.sortedBy { it.order }) {
            val p = min(effectiveQuota(c, balances.of(c.id)), max(0L, left))
            proposals[c.id] = p
            left -= p
        }
        return SalarySplit(proposals, amount - proposals.values.sum())
    }

    /** Data proposta per lo stipendio: il giorno di stipendio del mese corrente. */
    fun proposedSalaryDate(): LocalDate = paydayOf(currentMonth)

    // ---------- 4.6 Livello del vasetto ----------

    private val previousMonthBalances: Balances by lazy { balances(currentMonth.minusMonths(1)) }

    private val allocatedThisMonth: Map<String, Long> by lazy {
        val out = HashMap<String, Long>()
        for (m in movements) {
            if (YearMonth.from(m.date) != currentMonth) continue
            for ((cat, v) in m.allocations) if (v > 0) out[cat] = (out[cat] ?: 0L) + v
        }
        out
    }

    fun jarLevel(category: Category, balance: Long): JarLevel {
        val target = category.target
        if (category.isGoal && target != null && target > 0) {
            return JarLevel(percent(balance, target), target, JarState.NORMAL)
        }
        val full = max(0L, previousMonthBalances.of(category.id)) + (allocatedThisMonth[category.id] ?: 0L)
        if (full <= 0) {
            return JarLevel(0.0, 0, if (balance < 0) JarState.BELOW_ZERO else JarState.EMPTY_THIS_MONTH)
        }
        return JarLevel(percent(balance, full), full, JarState.NORMAL)
    }

    // ---------- 4.7 Tempo per riempire un obiettivo ----------

    /** Quando l'obiettivo sarà pieno con la quota effettiva; null se manca nulla o la quota è zero. */
    fun fillForecast(category: Category, balance: Long): FillForecast? {
        val target = category.target ?: return null
        val missing = target - balance
        val q = effectiveQuota(category, balance)
        if (missing <= 0 || q <= 0) return null
        val need = ceilDiv(missing, q).toInt()
        val date = nextPaydays(need)[need - 1]
        val late = category.deadline != null && date > category.deadline
        return FillForecast(need, date, late)
    }

    // ---------- 4.8 Riepilogo mensile ----------

    fun monthSummary(month: YearMonth): MonthSummary {
        var income = 0L
        var expenses = 0L
        val entered = HashMap<String, Long>()
        val spent = HashMap<String, Long>()
        for (m in movements) {
            if (YearMonth.from(m.date) != month) continue
            when (m.kind) {
                MovementKind.STIPENDIO, MovementKind.ENTRATA -> income += m.amount
                MovementKind.SPESA -> {
                    expenses += m.amount
                    if (m.categoryId != null) spent[m.categoryId] = (spent[m.categoryId] ?: 0L) + m.amount
                }
                else -> Unit
            }
            if (m.kind != MovementKind.PRELEVA) {
                for ((cat, v) in m.allocations) if (v > 0) entered[cat] = (entered[cat] ?: 0L) + v
            }
        }
        val end = balances(month)
        val ids = LinkedHashSet<String>().apply { addAll(end.byCategory.keys); addAll(entered.keys); addAll(spent.keys) }
        val perCategory = ids.associateWith { CategoryMonth(entered[it] ?: 0L, spent[it] ?: 0L, end.of(it)) }
        return MonthSummary(month, income, expenses, perCategory)
    }

    /** Mesi che contengono almeno un movimento, in ordine crescente. */
    fun monthsWithData(): List<YearMonth> = movements.map { YearMonth.from(it.date) }.distinct().sorted()

    /** Mesi del grafico: ultimi 12 fino al mese corrente, a partire dal primo mese con dati. */
    fun chartMonths(): List<YearMonth> {
        val first = monthsWithData().firstOrNull() ?: return emptyList()
        var start = currentMonth.minusMonths(11)
        if (first > start) start = first
        val out = ArrayList<YearMonth>()
        var m = start
        while (m <= currentMonth) {
            out += m
            m = m.plusMonths(1)
        }
        return out
    }

    // ---------- 4.9 / 4.10 ----------

    /**
     * Allocation del PRELEVA che chiude un budget con saldo [balance] (SPEC.md §4.9).
     * Saldo positivo: va in [destinationId] o, se null, in "Da assegnare".
     * Saldo negativo: viene coperto da "Da assegnare". Saldo zero: nessun movimento (mappa vuota).
     */
    fun closingAllocations(categoryId: String, balance: Long, destinationId: String?): Map<String, Long> = when {
        balance > 0 && destinationId != null -> linkedMapOf(categoryId to -balance, destinationId to balance)
        balance != 0L -> mapOf(categoryId to -balance)
        else -> emptyMap()
    }

    /** Differenza da registrare come RETTIFICA per portare il totale conto a [newTotal] (SPEC.md §4.10). */
    fun reconcileDifference(newTotal: Long): Long = newTotal - balances().totalAccount

    companion object {
        /** Limite pratico per "infiniti" stipendi (50 anni). */
        const val MAX_PAYDAYS = 600

        private val INCOMING = setOf(MovementKind.STIPENDIO, MovementKind.ENTRATA, MovementKind.RETTIFICA)

        /** Divisione intera arrotondata per eccesso, per a ≥ 0 e b > 0. */
        fun ceilDiv(a: Long, b: Long): Long = (a + b - 1) / b

        private fun percent(value: Long, of: Long): Double = (value.toDouble() / of * 100).coerceIn(0.0, 100.0)
    }
}
