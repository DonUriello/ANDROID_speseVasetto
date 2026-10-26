package it.elia.gestionespese.core.backup

import it.elia.gestionespese.core.Category
import it.elia.gestionespese.core.CategoryKind
import it.elia.gestionespese.core.FillMode
import it.elia.gestionespese.core.Movement
import it.elia.gestionespese.core.MovementKind
import it.elia.gestionespese.core.Settings
import it.elia.gestionespese.core.Shortcut
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** Contenuto completo di un backup, nel modello di dominio. */
data class BackupData(
    val settings: Settings,
    val categories: List<Category>,
    val shortcuts: List<Shortcut>,
    val movements: List<Movement>,
)

class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Lettura e scrittura del file di backup (SPEC.md §6), compatibile con il prototipo web:
 * `kind` minuscolo, `alloc` come mappa categoryId → centesimi, `ts` = createdAt.
 */
object BackupCodec {
    const val FORMAT = "gestione-spese-backup"
    const val VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        prettyPrint = true
        encodeDefaults = true
    }

    fun decode(text: String): BackupData {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw BackupFormatException("File di backup non leggibile", e)
        }
        if (file.format != FORMAT) throw BackupFormatException("Il file non è un backup di Gestione spese")
        if (file.version > VERSION) throw BackupFormatException("Versione del backup non supportata: ${file.version}")
        if (file.amountsIn != "cents") throw BackupFormatException("Importi non in centesimi")
        return try {
            BackupData(
                settings = Settings(payday = (file.settings?.payday ?: Settings.DEFAULT_PAYDAY).coerceIn(1, 31)),
                categories = file.categories.map { it.toDomain() },
                shortcuts = file.shortcuts.map { it.toDomain() },
                movements = file.movements.map { it.toDomain() },
            )
        } catch (e: RuntimeException) {
            throw BackupFormatException("Dati del backup non validi: ${e.message}", e)
        }
    }

    /** [exportedAt] in formato ISO 8601 con offset, es. "2026-10-26T12:15:00+01:00". */
    fun encode(data: BackupData, exportedAt: String): String = json.encodeToString(
        BackupFile.serializer(),
        BackupFile(
            format = FORMAT,
            version = VERSION,
            exportedAt = exportedAt,
            settings = BackupSettings(data.settings.payday),
            categories = data.categories.map { it.toFile() },
            shortcuts = data.shortcuts.map { it.toFile() },
            movements = data.movements.map { it.toFile() },
        ),
    )

    /** Nome proposto per il file esportato. */
    fun fileName(date: LocalDate): String = "backup-gestione-spese-$date.json"
}

// ---------- struttura del file ----------

@Serializable
private data class BackupFile(
    val format: String,
    val version: Int,
    val exportedAt: String? = null,
    val currency: String = "EUR",
    val amountsIn: String = "cents",
    val settings: BackupSettings? = null,
    val categories: List<BackupCategory> = emptyList(),
    val shortcuts: List<BackupShortcut> = emptyList(),
    val movements: List<BackupMovement> = emptyList(),
)

@Serializable
private data class BackupSettings(val payday: Int? = null)

@Serializable
private data class BackupCategory(
    val id: String,
    val name: String,
    val kind: String,
    val fill: String? = null,
    val quota: Long? = null,
    val target: Long? = null,
    val deadline: String? = null,
    val order: Int? = null,
)

@Serializable
private data class BackupShortcut(
    val id: String,
    val name: String,
    val amount: Long,
    val categoryId: String,
    val order: Int? = null,
)

@Serializable
private data class BackupMovement(
    val id: String,
    val kind: String,
    val date: String,
    val amount: Long,
    val alloc: Map<String, Long>? = null,
    val categoryId: String? = null,
    val note: String? = null,
    val total: Long? = null,
    val ts: Long? = null,
)

// ---------- conversioni ----------

private inline fun <reified E : Enum<E>> parseEnum(value: String): E =
    enumValues<E>().firstOrNull { it.name.equals(value, ignoreCase = true) }
        ?: throw IllegalArgumentException("valore sconosciuto '$value'")

private fun BackupCategory.toDomain() = Category(
    id = id,
    name = name,
    kind = parseEnum<CategoryKind>(kind),
    fill = fill?.let { parseEnum<FillMode>(it) } ?: FillMode.AUTO,
    quota = quota ?: 0,
    target = target,
    deadline = deadline?.let(LocalDate::parse),
    order = order ?: 0,
)

private fun Category.toFile() = BackupCategory(
    id = id,
    name = name,
    kind = kind.name.lowercase(),
    fill = fill.name.lowercase(),
    quota = quota,
    target = target,
    deadline = deadline?.toString(),
    order = order,
)

private fun BackupShortcut.toDomain() = Shortcut(id, name, amount, categoryId, order ?: 0)

private fun Shortcut.toFile() = BackupShortcut(id, name, amount, categoryId, order)

private fun BackupMovement.toDomain() = Movement(
    id = id,
    kind = parseEnum<MovementKind>(kind),
    date = LocalDate.parse(date),
    amount = amount,
    categoryId = categoryId,
    note = note,
    total = total,
    createdAt = ts ?: 0,
    allocations = alloc ?: emptyMap(),
)

private fun Movement.toFile() = BackupMovement(
    id = id,
    kind = kind.name.lowercase(),
    date = date.toString(),
    amount = amount,
    alloc = allocations.ifEmpty { null },
    categoryId = categoryId,
    note = note,
    total = total,
    ts = createdAt,
)
