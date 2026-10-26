package it.elia.gestionespese.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sort_order, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts ORDER BY sort_order, name")
    fun observeAll(): Flow<List<ShortcutEntity>>

    @Upsert
    suspend fun upsert(shortcut: ShortcutEntity)

    @Upsert
    suspend fun upsertAll(shortcuts: List<ShortcutEntity>)

    @Query("DELETE FROM shortcuts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM shortcuts WHERE category_id = :categoryId")
    suspend fun deleteByCategory(categoryId: String)

    @Query("DELETE FROM shortcuts")
    suspend fun deleteAll()
}

/** Un movimento con le sue quote. */
data class MovementWithAllocations(
    @Embedded val movement: MovementEntity,
    @Relation(parentColumn = "id", entityColumn = "movement_id")
    val allocations: List<MovementAllocationEntity>,
)

@Dao
interface MovementDao {
    @Transaction
    @Query("SELECT * FROM movements ORDER BY date, created_at")
    fun observeAll(): Flow<List<MovementWithAllocations>>

    @Insert
    suspend fun insertMovement(movement: MovementEntity)

    @Insert
    suspend fun insertAllocations(allocations: List<MovementAllocationEntity>)

    /** Inserisce il movimento e le sue quote in un'unica transazione. */
    @Transaction
    suspend fun insert(movement: MovementEntity, allocations: List<MovementAllocationEntity>) {
        insertMovement(movement)
        if (allocations.isNotEmpty()) insertAllocations(allocations)
    }

    /** Elimina il movimento; le quote seguono per cascade. */
    @Query("DELETE FROM movements WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM movements")
    suspend fun deleteAll()
}
