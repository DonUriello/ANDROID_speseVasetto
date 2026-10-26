package it.elia.gestionespese.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FormsTest {

    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun `parsing degli importi`() {
        assertEquals(123456L, Money.parse("1.234,56"))
        assertEquals(1250L, Money.parse("12,5"))
        assertEquals(1250L, Money.parse("12.50"))
        assertEquals(1250000L, Money.parse("12.500"))
        assertEquals(1000L, Money.parse(" 10 "))
        assertEquals(1L, Money.parse("0,005"))
        assertEquals(-500L, Money.parse("-5"))
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("1,2,3"))
        assertEquals("12,50", Money.input(1250))
        assertEquals("1234,05", Money.input(123405))
        assertEquals("0,00", Money.input(0))
    }

    @Test
    fun `id univoci`() {
        val ids = (1..1000).map { Ids.newId() }.toSet()
        assertEquals(1000, ids.size)
    }

    @Test
    fun `modulo entrata`() {
        assertEquals(Checked.Error(Forms.AMOUNT_REQUIRED), Forms.income("0", emptyMap()))
        assertEquals(Checked.Error("Le quote non possono essere negative."), Forms.income("10", mapOf("a" to "-1")))
        assertEquals(
            Checked.Error("Le quote superano l'importo di 1,00 €."),
            Forms.income("10", mapOf("a" to "6", "b" to "5")),
        )
        assertEquals(
            Checked.Ok(Forms.Income(1000, mapOf("a" to 600L))),
            Forms.income("10", mapOf("a" to "6", "b" to "0,00", "c" to "")),
        )
    }

    @Test
    fun `modulo assegna`() {
        assertEquals(Checked.Error("Inserisci almeno un importo."), Forms.assign(mapOf("a" to "0"), 1000))
        assertEquals(Checked.Error("Stai assegnando 2,00 € in più del disponibile."), Forms.assign(mapOf("a" to "12"), 1000))
        assertEquals(Checked.Ok(mapOf("a" to 1000L)), Forms.assign(mapOf("a" to "10"), 1000))
    }

    @Test
    fun `togli e rettifica`() {
        assertEquals(Checked.Error("Puoi togliere al massimo 5,00 €."), Forms.take("6", 500))
        assertEquals(Checked.Ok(500L), Forms.take("5", 500))
        assertEquals(Checked.Ok(0L), Forms.accountTotal("0"))
        assertTrue(Forms.accountTotal("-1") is Checked.Error)
    }

    @Test
    fun `bozza di un budget`() {
        val c = Category("x", "Svago", CategoryKind.SPESA, quota = 15000, order = 3)
        val draft = CategoryDraft.of(c)
        assertFalse(draft.differsFrom(c))
        assertTrue(draft.copy(quota = "151").differsFrom(c))
        assertEquals(Checked.Error("Scrivi un nome per il budget."), draft.copy(name = " ").toCategory(c))

        val goal = draft.copy(kind = CategoryKind.OBIETTIVO, target = "800", deadline = d("2026-12-11"))
        assertTrue(goal.hasAutoQuota)
        assertFalse(goal.copy(fill = FillMode.MANUALE).hasAutoQuota)
        val saved = (goal.toCategory(c) as Checked.Ok).value
        assertEquals(c.copy(kind = CategoryKind.OBIETTIVO, target = 80000, deadline = d("2026-12-11")), saved)

        // tornando a "Spese" soglia e scadenza si azzerano
        val back = (CategoryDraft.of(saved).copy(kind = CategoryKind.SPESA).toCategory(saved) as Checked.Ok).value
        assertEquals(c, back)
    }

    @Test
    fun `bozza di una spesa rapida`() {
        val s = Shortcut("s", "Caffe", 130, "c", 2)
        assertFalse(ShortcutDraft.of(s).differsFrom(s))
        assertEquals(Checked.Error("Scegli un budget."), ShortcutDraft.of(s).toShortcut(s) { false })
        assertEquals(Checked.Ok(s.copy(amount = 150)), ShortcutDraft.of(s).copy(amount = "1,5").toShortcut(s) { true })
    }

    @Test
    fun `dati di esempio`() {
        val (cats, shorts) = SampleData.create(d("2026-10-26"))
        assertEquals(7, cats.size)
        assertEquals((0..6).toList(), cats.map { it.order })
        assertEquals(d("2027-07-15"), cats.single { it.name == "Vacanze" }.deadline)
        assertEquals(cats[2].id, shorts.single { it.name == "Caffè" }.categoryId)
    }
}
