package it.elia.gestionespese.core

import it.elia.gestionespese.core.backup.BackupCodec
import it.elia.gestionespese.core.backup.BackupData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Stipendio con mese di riferimento diverso da quello della data (es. pagato il 31 ottobre per novembre). */
class SalaryMonthTest {

    private fun d(s: String) = LocalDate.parse(s)
    private fun ym(s: String) = YearMonth.parse(s)

    /** Stipendio di novembre pagato il 31 ottobre, con 300 € allo Svago. */
    private val novemberSalary = Movement(
        "s", MovementKind.STIPENDIO, d("2026-10-31"), 200000, createdAt = 2,
        allocations = mapOf("a" to 30000), refMonth = ym("2026-11"),
    )
    private val octoberSpending = listOf(
        Movement("o", MovementKind.ASSEGNA, d("2026-10-01"), 0, createdAt = 0, allocations = mapOf("a" to 10000)),
        Movement("x", MovementKind.SPESA, d("2026-10-20"), 4000, categoryId = "a", createdAt = 1),
    )

    @Test
    fun `il mese contabile e quello di riferimento`() {
        assertEquals(ym("2026-11"), novemberSalary.month)
        assertEquals(ym("2026-10"), novemberSalary.copy(refMonth = null).month)
    }

    @Test
    fun `riepilogo e saldi per mese di riferimento`() {
        val math = BudgetMath(octoberSpending + novemberSalary, 31, d("2026-11-05"))
        val oct = math.monthSummary(ym("2026-10"))
        val nov = math.monthSummary(ym("2026-11"))
        assertEquals(0, oct.income)
        assertEquals(200000, nov.income)
        assertEquals(30000, nov.perCategory["a"]!!.entered)
        // a fine ottobre lo stipendio di novembre non c'è ancora
        assertEquals(6000, math.balances(ym("2026-10")).of("a"))
        assertEquals(36000, math.balances().of("a"))
        assertEquals(listOf(ym("2026-10"), ym("2026-11")), math.monthsWithData())
    }

    @Test
    fun `vasetto di novembre riempito dallo stipendio pagato a ottobre`() {
        val math = BudgetMath(octoberSpending + novemberSalary, 31, d("2026-11-05"))
        val level = math.jarLevel(Category("a", "Svago", CategoryKind.SPESA), 36000)
        assertEquals(36000, level.full) // 60 € rimasti da ottobre + 300 € di novembre
        assertEquals(100.0, level.percent, 0.0001)
    }

    @Test
    fun `prossimi stipendi pagati in anticipo`() {
        // 26 ottobre: registrato lo stipendio di ottobre (pagato il 30 settembre), quello di novembre arriva il 31 ottobre
        val october = Movement("p", MovementKind.STIPENDIO, d("2026-09-30"), 1000, refMonth = ym("2026-10"))
        val before = BudgetMath(listOf(october), 31, d("2026-10-26"))
        assertEquals(1, before.salaryOffset)
        assertFalse(before.salaryReceivedThisMonth)
        assertEquals(listOf(d("2026-10-31"), d("2026-11-30"), d("2026-12-31")), before.nextPaydays(3))
        assertEquals(ym("2026-11"), before.proposedSalaryMonth())
        assertEquals(d("2026-10-31"), before.proposedSalaryDate())

        // 2 novembre, registrato anche quello di novembre: il prossimo è il 30 novembre (per dicembre)
        val after = BudgetMath(listOf(october, novemberSalary), 31, d("2026-11-02"))
        // lo stipendio che arriva a novembre è quello di dicembre, non ancora registrato
        assertFalse(after.salaryReceivedThisMonth)
        assertEquals(listOf(d("2026-11-30"), d("2026-12-31")), after.nextPaydays(2))
        assertEquals(ym("2026-12"), after.proposedSalaryMonth())
        assertEquals(d("2026-11-30"), after.proposedSalaryDate())
    }

    @Test
    fun `senza anticipo il comportamento non cambia`() {
        val october = Movement("s", MovementKind.STIPENDIO, d("2026-10-10"), 1000)
        val math = BudgetMath(listOf(october), 10, d("2026-10-26"))
        assertEquals(0, math.salaryOffset)
        assertEquals(ym("2026-11"), math.proposedSalaryMonth())
        assertEquals(d("2026-11-10"), math.proposedSalaryDate())
        val empty = BudgetMath(emptyList(), 10, d("2026-10-26"))
        assertEquals(ym("2026-10"), empty.proposedSalaryMonth())
        assertEquals(d("2026-10-10"), empty.proposedSalaryDate())
    }

    @Test
    fun `backup con mese di riferimento`() {
        val data = BackupData(Settings(31), emptyList(), emptyList(), listOf(novemberSalary))
        val text = BackupCodec.encode(data, "2026-11-02T10:00:00+01:00")
        assertTrue(text.contains("\"refMonth\": \"2026-11\""))
        assertEquals(data, BackupCodec.decode(text))
        // un mese uguale a quello della data equivale a nessun mese
        val same = BackupCodec.decode(text.replace("\"2026-11\"", "\"2026-10\""))
        assertNull(same.movements.single().refMonth)
    }

    @Test
    fun `titolo del movimento`() {
        assertEquals("Stipendio di novembre 2026", MovementText.describe(novemberSalary) { "Svago" }.title)
    }
}
