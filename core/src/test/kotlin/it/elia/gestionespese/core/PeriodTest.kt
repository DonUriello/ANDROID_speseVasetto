package it.elia.gestionespese.core

import it.elia.gestionespese.core.backup.BackupCodec
import it.elia.gestionespese.core.backup.BackupData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Periodi contabili da giorno di stipendio a giorno di stipendio, e stipendi extra. */
class PeriodTest {

    private fun d(s: String) = LocalDate.parse(s)
    private fun ym(s: String) = YearMonth.parse(s)

    @Test
    fun `periodo dal giorno di stipendio al successivo`() {
        val math = BudgetMath(emptyList(), 10, d("2026-10-26"))
        assertEquals(ym("2026-10"), math.periodOf(d("2026-10-10")))
        assertEquals(ym("2026-10"), math.periodOf(d("2026-11-09")))
        assertEquals(ym("2026-11"), math.periodOf(d("2026-11-10")))
        assertEquals(ym("2026-09"), math.periodOf(d("2026-10-09")))
        assertEquals(d("2026-10-10"), math.periodStart(ym("2026-10")))
        assertEquals(d("2026-11-09"), math.periodEnd(ym("2026-10")))
        assertEquals("10 ott – 9 nov 2026", math.periodLabel(ym("2026-10")))
        assertEquals("10 dic 2026 – 9 gen 2027", math.periodLabel(ym("2026-12")))
        assertEquals(ym("2026-10"), math.currentPeriod)
    }

    @Test
    fun `stipendio a fine mese e mesi corti`() {
        val math = BudgetMath(emptyList(), 31, d("2027-02-28"))
        assertEquals(d("2027-02-28"), math.periodStart(ym("2027-02")))
        assertEquals(d("2027-03-30"), math.periodEnd(ym("2027-02")))
        assertEquals(ym("2027-02"), math.currentPeriod)
        assertEquals(ym("2027-01"), math.periodOf(d("2027-02-27")))
    }

    @Test
    fun `con stipendio il primo del mese il periodo e il mese`() {
        val math = BudgetMath(emptyList(), 1, d("2026-10-26"))
        assertEquals("ottobre 2026", math.periodLabel(ym("2026-10")))
    }

    @Test
    fun `stipendio arrivato qualche giorno prima`() {
        val early = Movement("s", MovementKind.STIPENDIO, d("2026-10-08"), 1000)
        val math = BudgetMath(listOf(early), 10, d("2026-10-26"))
        assertEquals(ym("2026-10"), math.periodOf(early))
        assertTrue(math.salaryReceivedThisMonth)
        assertEquals(d("2026-11-10"), math.nextPaydays(1).single())
        // una spesa dello stesso giorno resta nel periodo precedente
        assertEquals(ym("2026-09"), math.periodOf(early.copy(kind = MovementKind.SPESA)))
    }

    @Test
    fun `vasetti riempiti fino al prossimo stipendio`() {
        // Stipendio il 10 ottobre: il "pieno" vale fino al 9 novembre
        val movs = listOf(
            Movement("s", MovementKind.STIPENDIO, d("2026-10-10"), 100000, createdAt = 1, allocations = mapOf("a" to 15000)),
            Movement("x", MovementKind.SPESA, d("2026-11-05"), 6000, categoryId = "a", createdAt = 2),
        )
        val cat = Category("a", "Svago", CategoryKind.SPESA, quota = 15000)
        val nov5 = BudgetMath(movs, 10, d("2026-11-05"))
        val level = nov5.jarLevel(cat, 9000)
        assertEquals(15000, level.full)
        assertEquals(60.0, level.percent, 0.0001)
        // il 10 novembre inizia un periodo nuovo: senza nuovo stipendio il pieno è il saldo rimasto
        val nov10 = BudgetMath(movs, 10, d("2026-11-10"))
        assertEquals(9000, nov10.jarLevel(cat, 9000).full)
        assertEquals(100.0, nov10.jarLevel(cat, 9000).percent, 0.0001)
    }

    @Test
    fun `riepilogo per periodo`() {
        val movs = listOf(
            Movement("s", MovementKind.STIPENDIO, d("2026-10-10"), 100000),
            Movement("x", MovementKind.SPESA, d("2026-11-05"), 6000, categoryId = "a"),
            Movement("y", MovementKind.SPESA, d("2026-11-12"), 1000, categoryId = "a"),
        )
        val math = BudgetMath(movs, 10, d("2026-11-20"))
        assertEquals(6000, math.monthSummary(ym("2026-10")).expenses)
        assertEquals(1000, math.monthSummary(ym("2026-11")).expenses)
        assertEquals(listOf(ym("2026-10"), ym("2026-11")), math.monthsWithData())
    }

    @Test
    fun `stipendio extra`() {
        val movs = listOf(
            Movement("s", MovementKind.STIPENDIO, d("2026-12-10"), 200000, createdAt = 1, allocations = mapOf("a" to 15000)),
            Movement("t", MovementKind.STIPENDIO_EXTRA, d("2026-12-15"), 180000, createdAt = 2, note = "Tredicesima"),
        )
        val math = BudgetMath(movs, 10, d("2026-12-20"))
        // entra tutto in "Da assegnare" e non riempie i vasetti
        assertEquals(15000, math.balances().of("a"))
        assertEquals(200000 - 15000 + 180000L, math.balances().toAssign)
        assertEquals(15000, math.jarLevel(Category("a", "Svago", CategoryKind.SPESA), 15000).full)
        // conta come entrata del periodo
        assertEquals(380000, math.monthSummary(ym("2026-12")).income)
        // non conta come stipendio del periodo
        val onlyExtra = BudgetMath(movs.drop(1), 10, d("2026-12-20"))
        assertFalse(onlyExtra.salaryReceivedThisMonth)

        val line = MovementText.describe(movs[1]) { null }
        assertEquals(MovementLine("Stipendio extra · Tredicesima", "Da assegnare ${Formats.eur(180000)}", 180000, "+", true), line)

        val data = BackupData(Settings(10), emptyList(), emptyList(), movs)
        val text = BackupCodec.encode(data, "2026-12-20T10:00:00+01:00")
        assertTrue(text.contains("\"stipendio_extra\""))
        assertEquals(data, BackupCodec.decode(text))
    }
}
