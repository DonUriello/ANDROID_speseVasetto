package it.elia.gestionespese.core

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

/** Formattazione in italiano (it-IT), senza passare per Double. */
object Formats {
    private const val NBSP = ' '

    private val MONTHS_SHORT = listOf("gen", "feb", "mar", "apr", "mag", "giu", "lug", "ago", "set", "ott", "nov", "dic")
    private val MONTHS_LONG = listOf(
        "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
        "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre",
    )

    /** 123456 → "1.234,56 €" (spazio non separabile prima di €). */
    fun eur(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val a = abs(cents)
        val units = (a / 100).toString().reversed().chunked(3).joinToString(".").reversed()
        val dec = (a % 100).toString().padStart(2, '0')
        return "$sign$units,$dec$NBSP€"
    }

    /** 2026-11-10 → "10 nov 2026". */
    fun date(d: LocalDate): String = "${d.dayOfMonth} ${MONTHS_SHORT[d.monthValue - 1]} ${d.year}"

    /** 2026-11-10 → "10 nov". */
    fun dayMonth(d: LocalDate): String = "${d.dayOfMonth.toString().padStart(2, '0')} ${MONTHS_SHORT[d.monthValue - 1]}"

    /**
     * Intervallo di date: "10 ott – 9 nov 2026", "10 dic 2026 – 9 gen 2027";
     * un mese intero diventa "ottobre 2026".
     */
    fun range(start: LocalDate, end: LocalDate): String {
        val ym = YearMonth.from(start)
        if (start.dayOfMonth == 1 && end == ym.atEndOfMonth()) return monthLong(ym)
        val s = "${start.dayOfMonth} ${MONTHS_SHORT[start.monthValue - 1]}"
        val e = "${end.dayOfMonth} ${MONTHS_SHORT[end.monthValue - 1]} ${end.year}"
        return if (start.year == end.year) "$s – $e" else "$s ${start.year} – $e"
    }

    /** 2026-10 → "ottobre 2026". */
    fun monthLong(m: YearMonth): String = "${MONTHS_LONG[m.monthValue - 1]} ${m.year}"

    /** 2026-10 → "ott 26". */
    fun monthShort(m: YearMonth): String = "${MONTHS_SHORT[m.monthValue - 1]} ${m.year.toString().takeLast(2)}"

    /** "1 stipendio" / "3 stipendi". */
    fun salaries(n: Int): String = if (n == 1) "1 stipendio" else "$n stipendi"
}
