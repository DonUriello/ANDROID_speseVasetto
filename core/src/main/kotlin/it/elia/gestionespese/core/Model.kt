package it.elia.gestionespese.core

import java.time.LocalDate

// Modello di dominio (SPEC.md §3). Importi sempre in centesimi.

enum class CategoryKind { SPESA, OBIETTIVO }

/** Modo di riempimento: significativo solo per gli obiettivi. */
enum class FillMode { AUTO, MANUALE }

enum class MovementKind { STIPENDIO, ENTRATA, SPESA, ASSEGNA, RETTIFICA, PRELEVA }

/** Un budget ("vasetto"). */
data class Category(
    val id: String,
    val name: String,
    val kind: CategoryKind,
    val fill: FillMode = FillMode.AUTO,
    /** Centesimi a stipendio, ≥ 0. */
    val quota: Long = 0,
    /** Soglia, solo per gli obiettivi. */
    val target: Long? = null,
    /** Scadenza, solo per gli obiettivi. */
    val deadline: LocalDate? = null,
    val order: Int = 0,
) {
    val isGoal: Boolean get() = kind == CategoryKind.OBIETTIVO
    val isManualGoal: Boolean get() = isGoal && fill == FillMode.MANUALE
}

/** Spesa rapida. */
data class Shortcut(
    val id: String,
    val name: String,
    /** Centesimi, > 0. */
    val amount: Long,
    val categoryId: String,
    val order: Int = 0,
)

data class Movement(
    val id: String,
    val kind: MovementKind,
    val date: LocalDate,
    /** Centesimi; per RETTIFICA ha segno. */
    val amount: Long,
    /** Solo per SPESA. */
    val categoryId: String? = null,
    val note: String? = null,
    /** Solo per RETTIFICA: totale conto impostato. */
    val total: Long? = null,
    /** Epoch millis, per ordinare i movimenti dello stesso giorno. */
    val createdAt: Long = 0,
    /** categoryId → centesimi con segno (STIPENDIO, ENTRATA, ASSEGNA, PRELEVA). */
    val allocations: Map<String, Long> = emptyMap(),
)

data class Settings(
    /** Giorno del mese in cui arriva lo stipendio (1–31). */
    val payday: Int = DEFAULT_PAYDAY,
) {
    companion object {
        const val DEFAULT_PAYDAY = 27
    }
}
