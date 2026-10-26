package it.elia.gestionespese.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Casi di SPEC.md §9, più qualche caso limite. */
class BudgetMathTest {

    private val today = LocalDate.of(2026, 10, 26)
    private val octoberSalary = Movement("s", MovementKind.STIPENDIO, LocalDate.of(2026, 10, 10), 100000, createdAt = 1)
    private val withSalary = BudgetMath(listOf(octoberSalary), payday = 10, today = today)
    private val withoutSalary = BudgetMath(emptyList(), payday = 10, today = today)

    private fun d(s: String) = LocalDate.parse(s)

    private fun goal(fill: FillMode, quota: Long = 0) = Category(
        id = "g", name = "Tasse", kind = CategoryKind.OBIETTIVO, fill = fill,
        quota = quota, target = 80000, deadline = d("2026-12-11"),
    )

    // ---------- 4.2 ----------

    @Test
    fun `prossimi stipendi con stipendio del mese gia registrato`() {
        assertEquals(listOf(d("2026-11-10"), d("2026-12-10"), d("2027-01-10")), withSalary.nextPaydays(3))
    }

    @Test
    fun `prossimo stipendio con stipendio del mese non registrato`() {
        assertEquals(listOf(d("2026-10-10")), withoutSalary.nextPaydays(1))
    }

    @Test
    fun `stipendi entro la scadenza`() {
        assertEquals(2, withSalary.salariesUntil(d("2026-12-11")))
        assertEquals(1, withSalary.salariesUntil(d("2026-12-05")))
        assertEquals(0, withSalary.salariesUntil(d("2026-10-30")))
    }

    @Test
    fun `nei mesi corti lo stipendio cade l'ultimo giorno`() {
        val math = BudgetMath(emptyList(), payday = 31, today = d("2027-01-31"))
        assertEquals(listOf(d("2027-01-31"), d("2027-02-28"), d("2027-03-31"), d("2027-04-30")), math.nextPaydays(4))
    }

    // ---------- 4.3 / 4.4 / 4.7 ----------

    @Test
    fun `piano obiettivo automatico`() {
        val plan = withSalary.goalPlan(goal(FillMode.AUTO), 0)!!
        assertEquals(2, plan.salaries)
        assertEquals(80000, plan.missing)
        assertEquals(40000, plan.quota)
        assertFalse(plan.overdue)
        assertEquals(40000, withSalary.effectiveQuota(goal(FillMode.AUTO), 0))
    }

    @Test
    fun `piano arrotonda per eccesso`() {
        val plan = withSalary.goalPlan(goal(FillMode.AUTO), 1)!!
        assertEquals(79999, plan.missing)
        assertEquals(40000, plan.quota)
    }

    @Test
    fun `piano con scadenza superata`() {
        val c = goal(FillMode.AUTO).copy(deadline = d("2026-10-01"))
        val plan = withSalary.goalPlan(c, 30000)!!
        assertEquals(0, plan.salaries)
        assertEquals(50000, plan.quota)
        assertTrue(plan.overdue)
    }

    @Test
    fun `piano assente senza scadenza o per budget spesa`() {
        assertNull(withSalary.goalPlan(goal(FillMode.AUTO).copy(deadline = null, quota = 7000), 0))
        assertEquals(7000, withSalary.effectiveQuota(goal(FillMode.AUTO).copy(deadline = null, quota = 7000), 0))
        val spesa = Category("x", "Svago", CategoryKind.SPESA, quota = 15000)
        assertNull(withSalary.goalPlan(spesa, 0))
        assertEquals(15000, withSalary.effectiveQuota(spesa, 0))
    }

    @Test
    fun `obiettivo manuale pieno con 1 stipendio`() {
        val c = goal(FillMode.MANUALE, quota = 80000)
        assertEquals(80000, withSalary.effectiveQuota(c, 0))
        val f = withSalary.fillForecast(c, 0)!!
        assertEquals(1, f.salariesNeeded)
        assertEquals(d("2026-11-10"), f.date)
        assertFalse(f.late)
        assertEquals(
            "Pieno con 1 stipendio, il 10 nov 2026",
            "Pieno con ${Formats.salaries(f.salariesNeeded)}, il ${Formats.date(f.date)}",
        )
    }

    @Test
    fun `obiettivo manuale in ritardo sulla scadenza`() {
        val c = goal(FillMode.MANUALE, quota = 30000)
        val f = withSalary.fillForecast(c, 0)!!
        assertEquals(3, f.salariesNeeded)
        assertEquals(d("2027-01-10"), f.date)
        assertTrue(f.late)
        assertEquals(40000, withSalary.goalPlan(c, 0)!!.quota)
    }

    @Test
    fun `nessuna previsione con quota zero o obiettivo raggiunto`() {
        assertNull(withSalary.fillForecast(goal(FillMode.MANUALE, quota = 0), 0))
        assertNull(withSalary.fillForecast(goal(FillMode.MANUALE, quota = 30000), 80000))
    }

    // ---------- 4.5 ----------

    @Test
    fun `smistamento dello stipendio`() {
        val cats = listOf(
            Category("c", "Colazione", CategoryKind.SPESA, quota = 5000, order = 2),
            Category("a", "Svago", CategoryKind.SPESA, quota = 15000, order = 0),
            Category("b", "Benzina", CategoryKind.SPESA, quota = 6000, order = 1),
        )
        val split = withSalary.salarySplit(20000, cats, withSalary.balances())
        assertEquals(listOf("a" to 15000L, "b" to 5000L, "c" to 0L), split.proposals.toList())
        assertEquals(0, split.toAssign)

        val big = withSalary.salarySplit(100000, cats, withSalary.balances())
        assertEquals(74000, big.toAssign)
    }

    @Test
    fun `data proposta per lo stipendio`() {
        // ottobre è già registrato: si propone il prossimo
        assertEquals(d("2026-11-10"), withSalary.proposedSalaryDate())
        assertEquals(d("2026-10-10"), withoutSalary.proposedSalaryDate())
    }

    // ---------- 4.1 ----------

    @Test
    fun `saldi con spese, prelievi e rettifiche`() {
        val movs = listOf(
            Movement("1", MovementKind.STIPENDIO, d("2026-10-10"), 10000, createdAt = 1, allocations = mapOf("a" to 3000, "b" to 2000)),
            Movement("2", MovementKind.SPESA, d("2026-10-11"), 4000, categoryId = "a", createdAt = 2),
            Movement("3", MovementKind.PRELEVA, d("2026-10-12"), 0, createdAt = 3, allocations = mapOf("b" to -500, "a" to 500)),
            Movement("4", MovementKind.RETTIFICA, d("2026-10-13"), -1000, total = 9000, createdAt = 4),
            Movement("5", MovementKind.ASSEGNA, d("2026-10-14"), 0, createdAt = 5, allocations = mapOf("b" to 1000)),
        )
        val b = BudgetMath(movs, 10, today).balances()
        assertEquals(-500, b.of("a"))
        assertEquals(2500, b.of("b"))
        assertEquals(3000, b.toAssign)
        assertEquals(2000, b.totalInBudgets)
        assertEquals(5000, b.totalAccount)
    }

    @Test
    fun `saldi fino a un mese`() {
        val movs = listOf(
            Movement("1", MovementKind.ENTRATA, d("2026-09-10"), 5000, allocations = mapOf("a" to 5000)),
            Movement("2", MovementKind.SPESA, d("2026-10-12"), 1000, categoryId = "a"),
        )
        val math = BudgetMath(movs, 10, today)
        assertEquals(5000, math.balances(YearMonth.of(2026, 9)).of("a"))
        assertEquals(4000, math.balances().of("a"))
    }

    // ---------- 4.6 ----------

    @Test
    fun `livello del vasetto`() {
        val movs = listOf(
            Movement("1", MovementKind.ASSEGNA, d("2026-09-10"), 0, allocations = mapOf("a" to 2000)),
            Movement("2", MovementKind.ASSEGNA, d("2026-10-10"), 0, allocations = mapOf("a" to 6000)),
            Movement("3", MovementKind.SPESA, d("2026-10-12"), 4000, categoryId = "a"),
        )
        val math = BudgetMath(movs, 10, today)
        val cat = Category("a", "Svago", CategoryKind.SPESA)
        val level = math.jarLevel(cat, math.balances().of("a"))
        assertEquals(8000, level.full)
        assertEquals(50.0, level.percent, 0.0001)
        assertEquals(5, level.layers)
        assertEquals(JarState.NORMAL, level.state)
    }

    @Test
    fun `vasetto vuoto, sotto zero e obiettivo`() {
        val math = BudgetMath(emptyList(), 10, today)
        val cat = Category("a", "Svago", CategoryKind.SPESA)
        assertEquals(JarState.EMPTY_THIS_MONTH, math.jarLevel(cat, 0).state)
        assertEquals(JarState.BELOW_ZERO, math.jarLevel(cat, -100).state)
        assertEquals(0, math.jarLevel(cat, -100).layers)

        val g = goal(FillMode.AUTO)
        assertEquals(45.0, math.jarLevel(g, 36000).percent, 0.0001)
        assertEquals(100.0, math.jarLevel(g, 99999).percent, 0.0001)
        assertEquals(10, math.jarLevel(g, 99999).layers)
    }

    // ---------- 4.8 ----------

    @Test
    fun `riepilogo mensile`() {
        val movs = listOf(
            Movement("1", MovementKind.STIPENDIO, d("2026-10-10"), 10000, allocations = mapOf("a" to 3000)),
            Movement("2", MovementKind.ENTRATA, d("2026-10-11"), 500),
            Movement("3", MovementKind.SPESA, d("2026-10-12"), 1200, categoryId = "a"),
            Movement("4", MovementKind.PRELEVA, d("2026-10-13"), 0, allocations = mapOf("a" to -800, "b" to 800)),
            Movement("5", MovementKind.RETTIFICA, d("2026-10-14"), 999),
            Movement("6", MovementKind.SPESA, d("2026-09-12"), 50, categoryId = "a"),
        )
        val s = BudgetMath(movs, 10, today).monthSummary(YearMonth.of(2026, 10))
        assertEquals(10500, s.income)
        assertEquals(1200, s.expenses)
        assertEquals(9300, s.difference)
        assertEquals(CategoryMonth(entered = 3000, spent = 1200, endBalance = 950), s.perCategory["a"])
        assertEquals(CategoryMonth(entered = 0, spent = 0, endBalance = 800), s.perCategory["b"])
    }

    @Test
    fun `mesi del grafico`() {
        val movs = listOf(Movement("1", MovementKind.ENTRATA, d("2026-08-13"), 100))
        assertEquals(
            listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10)),
            BudgetMath(movs, 10, today).chartMonths(),
        )
        val old = listOf(Movement("1", MovementKind.ENTRATA, d("2024-01-03"), 100))
        val months = BudgetMath(old, 10, today).chartMonths()
        assertEquals(12, months.size)
        assertEquals(YearMonth.of(2025, 11), months.first())
    }

    // ---------- 4.9 / 4.10 ----------

    @Test
    fun `chiusura di un budget`() {
        assertEquals(mapOf("a" to -500L, "b" to 500L), withSalary.closingAllocations("a", 500, "b"))
        assertEquals(mapOf("a" to -500L), withSalary.closingAllocations("a", 500, null))
        assertEquals(mapOf("a" to 300L), withSalary.closingAllocations("a", -300, "b"))
        assertEquals(emptyMap<String, Long>(), withSalary.closingAllocations("a", 0, null))
    }

    @Test
    fun `rettifica del totale conto`() {
        assertEquals(-1000, withSalary.reconcileDifference(99000))
    }
}
