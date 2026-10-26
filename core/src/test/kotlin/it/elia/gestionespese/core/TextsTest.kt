package it.elia.gestionespese.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Testi di Movimenti e delle righe Budget, confrontati con il prototipo. */
class TextsTest {

    private fun d(s: String) = LocalDate.parse(s)
    private val names = mapOf("a" to "Svago", "b" to "Benzina")
    private val nameOf: (String) -> String? = { names[it] }
    private fun eur(c: Long) = Formats.eur(c)

    @Test
    fun `righe dei movimenti`() {
        val salary = Movement("1", MovementKind.STIPENDIO, d("2026-10-10"), 10000, allocations = mapOf("a" to 3000, "b" to 2000))
        assertEquals(
            MovementLine("Stipendio di ottobre 2026", "Svago ${eur(3000)} · Benzina ${eur(2000)} · Da assegnare ${eur(5000)}", 10000, "+", true),
            MovementText.describe(salary, nameOf),
        )
        val extra = Movement("2", MovementKind.ENTRATA, d("2026-10-10"), 500, note = "regalo")
        assertEquals(MovementLine("Entrata extra · regalo", "Da assegnare ${eur(500)}", 500, "+", true), MovementText.describe(extra, nameOf))

        val spesa = Movement("3", MovementKind.SPESA, d("2026-10-11"), 290, categoryId = "zzz", note = "")
        assertEquals(MovementLine("Spesa", "Budget eliminato", 290, "−", false), MovementText.describe(spesa, nameOf))

        val take = Movement("4", MovementKind.PRELEVA, d("2026-10-11"), 0, note = "troppo", allocations = mapOf("a" to -700))
        assertEquals(
            MovementLine("Soldi tolti da un budget · troppo", "Svago → Da assegnare", 700, "", false),
            MovementText.describe(take, nameOf),
        )
        val move = take.copy(note = null, allocations = mapOf("a" to -700, "b" to 700))
        assertEquals("Svago → Benzina", MovementText.describe(move, nameOf).subtitle)

        val assign = Movement("5", MovementKind.ASSEGNA, d("2026-10-12"), 0, allocations = mapOf("a" to 100, "b" to 50))
        assertEquals(MovementLine("Assegnazione", "Svago ${eur(100)} · Benzina ${eur(50)}", 150, "", false), MovementText.describe(assign, nameOf))

        val reset = Movement("6", MovementKind.RETTIFICA, d("2026-10-12"), -165030, total = 0)
        assertEquals(
            MovementLine("Rettifica totale conto", "Totale impostato a ${eur(0)} · differenza in \"Da assegnare\"", 165030, "−", false),
            MovementText.describe(reset, nameOf),
        )
        assertEquals("+", MovementText.describe(reset.copy(amount = 100), nameOf).sign)
    }

    @Test
    fun `righe dei budget`() {
        val today = d("2026-10-26")
        val math = BudgetMath(listOf(Movement("s", MovementKind.STIPENDIO, d("2026-10-10"), 0)), 10, today)

        val manual = Category("g", "Tasse", CategoryKind.OBIETTIVO, FillMode.MANUALE, 30000, 80000, d("2026-12-11"))
        val card = BudgetCard.of(manual, 0, math)
        assertEquals(BudgetBadge.OBIETTIVO, card.badge)
        assertEquals("0% di ${eur(80000)}", card.levelLine)
        val text = card.lines.map { line -> line.joinToString("") { it.text } }
        assertEquals(
            listOf(
                "Scadenza 11 dic 2026 · 2 stipendi prima della scadenza",
                "Quota tua: ${eur(30000)} a stipendio",
                "Pieno con 3 stipendi, il 10 gen 2027 · dopo la scadenza, servono ${eur(40000)} a stipendio",
            ),
            text,
        )
        assertEquals(true, card.lines[2].last().warn)

        val zero = BudgetCard.of(manual.copy(quota = 0), 0, math)
        assertEquals("Quota a zero: per arrivarci servono ${eur(40000)} a stipendio", zero.lines.last().joinToString("") { it.text })

        assertEquals(BudgetBadge.RAGGIUNTO, BudgetCard.of(manual, 80000, math).badge)
        assertEquals(listOf("Pronta per la spesa"), BudgetCard.of(manual, 80000, math).lines.single().map { it.text })

        val spesa = Category("a", "Svago", CategoryKind.SPESA, quota = 15000)
        val empty = BudgetCard.of(spesa, 0, math)
        assertEquals(BudgetBadge.SPESE, empty.badge)
        assertEquals("Ancora vuoto questo mese", empty.levelLine)
        assertEquals("+${eur(15000)} a stipendio", empty.lines.single().single().text)
        assertEquals(BudgetBadge.IN_ROSSO, BudgetCard.of(spesa, -1, math).badge)
        assertEquals("Sotto zero", BudgetCard.of(spesa, -1, math).levelLine)
    }

    @Test
    fun `riga del livello con pieno del mese`() {
        val movs = listOf(
            Movement("1", MovementKind.ASSEGNA, d("2026-10-10"), 0, allocations = mapOf("a" to 15000)),
            Movement("2", MovementKind.SPESA, d("2026-10-11"), 6000, categoryId = "a"),
        )
        val math = BudgetMath(movs, 10, d("2026-10-26"))
        val card = BudgetCard.of(Category("a", "Svago", CategoryKind.SPESA, quota = 15000), 9000, math)
        assertEquals("Restano ${eur(9000)} di ${eur(15000)} · 60%", card.levelLine)
        assertEquals(6, card.level.layers)
    }
}
