package it.elia.gestionespese.core

import java.time.LocalDate

/** Esito della validazione di un modulo: un valore oppure un messaggio d'errore in italiano. */
sealed interface Checked<out T> {
    data class Ok<T>(val value: T) : Checked<T>
    data class Error(val message: String) : Checked<Nothing>
}

/** Validazioni dei moduli (SPEC.md §5, Moduli), uguali a quelle del prototipo. */
object Forms {
    const val AMOUNT_REQUIRED = "Inserisci un importo maggiore di zero."

    data class Income(val amount: Long, val allocations: Map<String, Long>)

    /** Stipendio o entrata extra: importo > 0, quote ≥ 0, somma quote ≤ importo. */
    fun income(amountText: String, allocationTexts: Map<String, String>): Checked<Income> {
        val amount = Money.parse(amountText)
        if (amount == null || amount <= 0) return Checked.Error(AMOUNT_REQUIRED)
        val alloc = LinkedHashMap<String, Long>()
        for ((id, text) in allocationTexts) {
            val v = Money.parse(text) ?: 0
            if (v < 0) return Checked.Error("Le quote non possono essere negative.")
            if (v > 0) alloc[id] = v
        }
        val sum = alloc.values.sum()
        if (sum > amount) return Checked.Error("Le quote superano l'importo di ${Formats.eur(sum - amount)}.")
        return Checked.Ok(Income(amount, alloc))
    }

    /** Assegnazione dal "Da assegnare": almeno un importo, nessuno negativo, totale ≤ disponibile. */
    fun assign(allocationTexts: Map<String, String>, available: Long): Checked<Map<String, Long>> {
        val alloc = LinkedHashMap<String, Long>()
        for ((id, text) in allocationTexts) {
            val v = Money.parse(text) ?: 0
            if (v < 0) return Checked.Error("Gli importi non possono essere negativi.")
            if (v > 0) alloc[id] = v
        }
        val sum = alloc.values.sum()
        if (sum == 0L) return Checked.Error("Inserisci almeno un importo.")
        if (sum > available) return Checked.Error("Stai assegnando ${Formats.eur(sum - available)} in più del disponibile.")
        return Checked.Ok(alloc)
    }

    fun positiveAmount(text: String): Checked<Long> {
        val v = Money.parse(text)
        return if (v == null || v <= 0) Checked.Error(AMOUNT_REQUIRED) else Checked.Ok(v)
    }

    /** Togli da un budget: 0 < importo ≤ saldo. */
    fun take(text: String, balance: Long): Checked<Long> {
        val v = Money.parse(text)
        if (v == null || v <= 0) return Checked.Error(AMOUNT_REQUIRED)
        if (v > balance) return Checked.Error("Puoi togliere al massimo ${Formats.eur(balance)}.")
        return Checked.Ok(v)
    }

    /** Totale reale del conto: ≥ 0. */
    fun accountTotal(text: String): Checked<Long> {
        val v = Money.parse(text)
        return if (v == null || v < 0) Checked.Error("Inserisci un importo valido, anche zero.") else Checked.Ok(v)
    }

    /** Totali in tempo reale mostrati nei moduli, ignorando i campi non validi. */
    fun sumOf(texts: Collection<String>): Long = texts.sumOf { Money.parse(it) ?: 0 }
}

/** Bozza di un budget in Impostazioni: nulla viene salvato prima di "Salva" o "Crea". */
data class CategoryDraft(
    val name: String = "",
    val kind: CategoryKind = CategoryKind.SPESA,
    val fill: FillMode = FillMode.AUTO,
    val quota: String = "",
    val target: String = "",
    val deadline: LocalDate? = null,
) {
    val isGoal: Boolean get() = kind == CategoryKind.OBIETTIVO

    /** Obiettivo automatico con soglia e scadenza: la quota la calcola l'app. */
    val hasAutoQuota: Boolean get() = isGoal && fill == FillMode.AUTO && (Money.parse(target) ?: 0) > 0 && deadline != null

    private val targetOrNull: Long? get() = Money.parse(target)?.takeIf { it > 0 }

    /** Diversa dal budget salvato. */
    fun differsFrom(c: Category): Boolean =
        name.trim() != c.name || kind != c.kind || (isGoal && fill != c.fill) ||
            (Money.parse(quota.ifBlank { "0" }) ?: -1) != c.quota ||
            (isGoal && (targetOrNull != c.target || deadline != c.deadline))

    /** Budget risultante, partendo da [base] (id e ordine). */
    fun toCategory(base: Category): Checked<Category> {
        val n = name.trim()
        if (n.isEmpty()) return Checked.Error("Scrivi un nome per il budget.")
        val q = Money.parse(quota.ifBlank { "0" })
        if (q == null || q < 0) return Checked.Error("La quota non è un importo valido.")
        return Checked.Ok(
            base.copy(
                name = n,
                kind = kind,
                quota = q,
                fill = if (isGoal) fill else FillMode.AUTO,
                target = if (isGoal) targetOrNull else null,
                deadline = if (isGoal) deadline else null,
            ),
        )
    }

    companion object {
        fun of(c: Category) = CategoryDraft(
            name = c.name,
            kind = c.kind,
            fill = c.fill,
            quota = Money.input(c.quota),
            target = c.target?.let(Money::input).orEmpty(),
            deadline = c.deadline,
        )
    }
}

/** Bozza di una spesa rapida in Impostazioni. */
data class ShortcutDraft(
    val name: String = "",
    val amount: String = "",
    val categoryId: String? = null,
) {
    fun differsFrom(s: Shortcut): Boolean =
        name.trim() != s.name || Money.parse(amount) != s.amount || categoryId != s.categoryId

    fun toShortcut(base: Shortcut, categoryExists: (String) -> Boolean): Checked<Shortcut> {
        val n = name.trim()
        if (n.isEmpty()) return Checked.Error("Scrivi un nome per la spesa rapida.")
        val a = Money.parse(amount)
        if (a == null || a <= 0) return Checked.Error(Forms.AMOUNT_REQUIRED)
        val cat = categoryId
        if (cat == null || !categoryExists(cat)) return Checked.Error("Scegli un budget.")
        return Checked.Ok(base.copy(name = n, amount = a, categoryId = cat))
    }

    companion object {
        fun of(s: Shortcut) = ShortcutDraft(s.name, Money.input(s.amount), s.categoryId)
    }
}

/** Budget e spese rapide di esempio ("Parti da un esempio modificabile"). */
object SampleData {
    fun create(today: LocalDate, newId: () -> String = { Ids.newId() }): Pair<List<Category>, List<Shortcut>> {
        val y = today.year
        val base = listOf(
            Category("", "Affitto e casa", CategoryKind.SPESA, quota = 65000),
            Category("", "Bollette", CategoryKind.SPESA, quota = 15000),
            Category("", "Spesa alimentare", CategoryKind.SPESA, quota = 35000),
            Category("", "Trasporti", CategoryKind.SPESA, quota = 12000),
            Category("", "Svago", CategoryKind.SPESA, quota = 15000),
            Category("", "Vacanze", CategoryKind.OBIETTIVO, quota = 0, target = 150000, deadline = LocalDate.of(y + 1, 7, 15)),
            Category("", "Fondo emergenze", CategoryKind.OBIETTIVO, quota = 10000, target = 300000),
        )
        val cats = base.mapIndexed { i, c -> c.copy(id = newId(), order = i) }
        val shorts = listOf(Triple("Caffè", 120L, 2), Triple("Pranzo fuori", 1200L, 4), Triple("Benzina", 5000L, 3))
            .mapIndexed { i, (name, amount, ci) -> Shortcut(newId(), name, amount, cats[ci].id, i) }
        return cats to shorts
    }
}
