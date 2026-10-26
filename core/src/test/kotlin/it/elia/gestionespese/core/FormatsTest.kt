package it.elia.gestionespese.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FormatsTest {

    @Test
    fun euro() {
        assertEquals("1.234,56 €", Formats.eur(123456))
        assertEquals("0,00 €", Formats.eur(0))
        assertEquals("0,05 €", Formats.eur(5))
        assertEquals("-12,30 €", Formats.eur(-1230))
        assertEquals("1.650,30 €", Formats.eur(165030))
        assertEquals("1.000.000,00 €", Formats.eur(100000000))
    }

    @Test
    fun date() {
        assertEquals("10 nov 2026", Formats.date(LocalDate.of(2026, 11, 10)))
        assertEquals("05 gen", Formats.dayMonth(LocalDate.of(2027, 1, 5)))
        assertEquals("ottobre 2026", Formats.monthLong(YearMonth.of(2026, 10)))
        assertEquals("ott 26", Formats.monthShort(YearMonth.of(2026, 10)))
    }
}
