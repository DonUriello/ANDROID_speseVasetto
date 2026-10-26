package it.elia.gestionespese.core

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

/** Importi scritti dall'utente nei campi di testo. */
object Money {
    /** Punto delle migliaia: un punto seguito da esattamente tre cifre. */
    private val THOUSANDS = Regex("""\.(?=\d{3}(\D|$))""")

    /**
     * "1.234,56" → 123456; "12,5" → 1250; "12.50" → 1250; vuoto o non valido → null.
     * Arrotonda al centesimo.
     */
    fun parse(text: String?): Long? {
        if (text == null) return null
        var s = text.filterNot { it.isWhitespace() }
        if (s.isEmpty()) return null
        s = THOUSANDS.replace(s, "").replaceFirst(',', '.')
        return try {
            BigDecimal(s).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (e: ArithmeticException) {
            null
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** Valore iniziale di un campo importo: 1250 → "12,50" (senza separatore delle migliaia). */
    fun input(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val a = kotlin.math.abs(cents)
        return "$sign${a / 100},${(a % 100).toString().padStart(2, '0')}"
    }
}

/** Identificativi generati sul telefono, nello stesso stile del prototipo web. */
object Ids {
    fun newId(now: Long = System.currentTimeMillis(), random: Random = Random.Default): String {
        val suffix = (1..6).map { "0123456789abcdefghijklmnopqrstuvwxyz"[random.nextInt(36)] }.joinToString("")
        return now.toString(36) + suffix
    }
}
