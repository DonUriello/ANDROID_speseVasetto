package it.elia.gestionespese.data

import androidx.room.withTransaction
import it.elia.gestionespese.core.Category
import it.elia.gestionespese.core.Movement
import it.elia.gestionespese.core.Settings
import it.elia.gestionespese.core.Shortcut
import it.elia.gestionespese.core.backup.BackupData
import it.elia.gestionespese.data.db.AppDatabase
import it.elia.gestionespese.data.db.allocationEntities
import it.elia.gestionespese.data.db.toDomain
import it.elia.gestionespese.data.db.toEntity
import it.elia.gestionespese.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Fotografia di tutti i dati dell'app. */
data class AppData(
    val settings: Settings,
    val categories: List<Category>,
    val shortcuts: List<Shortcut>,
    val movements: List<Movement>,
)

/** Repository unico: unico punto di accesso a Room e DataStore. */
class SpeseRepository(
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository,
) {
    private val categoryDao = db.categoryDao()
    private val shortcutDao = db.shortcutDao()
    private val movementDao = db.movementDao()

    val categories: Flow<List<Category>> = categoryDao.observeAll().map { list -> list.map { it.toDomain() } }
    val shortcuts: Flow<List<Shortcut>> = shortcutDao.observeAll().map { list -> list.map { it.toDomain() } }
    val movements: Flow<List<Movement>> = movementDao.observeAll().map { list -> list.map { it.toDomain() } }
    val settings: Flow<Settings> = settingsRepository.settings

    val data: Flow<AppData> = combine(settings, categories, shortcuts, movements, ::AppData)

    // ---------- budget ----------

    suspend fun saveCategory(category: Category) = categoryDao.upsert(category.toEntity())

    suspend fun saveCategories(categories: List<Category>) = categoryDao.upsertAll(categories.map { it.toEntity() })

    /**
     * Elimina un budget (SPEC.md §4.9): registra l'eventuale movimento di chiusura,
     * elimina le spese rapide collegate e il budget. I movimenti restano nello storico.
     */
    suspend fun deleteCategory(categoryId: String, closing: Movement?) = db.withTransaction {
        if (closing != null) movementDao.insert(closing.toEntity(), closing.allocationEntities())
        shortcutDao.deleteByCategory(categoryId)
        categoryDao.delete(categoryId)
    }

    // ---------- spese rapide ----------

    suspend fun saveShortcut(shortcut: Shortcut) = shortcutDao.upsert(shortcut.toEntity())

    suspend fun saveShortcuts(shortcuts: List<Shortcut>) = shortcutDao.upsertAll(shortcuts.map { it.toEntity() })

    suspend fun deleteShortcut(id: String) = shortcutDao.delete(id)

    // ---------- movimenti ----------

    suspend fun addMovement(movement: Movement) =
        movementDao.insert(movement.toEntity(), movement.allocationEntities())

    suspend fun deleteMovement(id: String) = movementDao.delete(id)

    // ---------- impostazioni ----------

    suspend fun setPayday(day: Int) = settingsRepository.setPayday(day)

    // ---------- backup ----------

    /** Sostituisce tutti i dati con quelli del backup, in una transazione (SPEC.md §6). */
    suspend fun replaceAll(backup: BackupData) {
        db.withTransaction {
            movementDao.deleteAll()
            shortcutDao.deleteAll()
            categoryDao.deleteAll()
            categoryDao.upsertAll(backup.categories.map { it.toEntity() })
            shortcutDao.upsertAll(backup.shortcuts.map { it.toEntity() })
            for (m in backup.movements) movementDao.insert(m.toEntity(), m.allocationEntities())
        }
        settingsRepository.replace(backup.settings)
    }
}
