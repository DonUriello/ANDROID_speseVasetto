package it.elia.gestionespese.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import it.elia.gestionespese.core.CategoryKind
import it.elia.gestionespese.core.FillMode
import it.elia.gestionespese.core.MovementKind
import java.time.LocalDate
import java.time.YearMonth

// Tabelle Room (SPEC.md §3). Importi in centesimi, date come testo ISO yyyy-MM-dd.
// Nessuna foreign key su categoryId: i movimenti di un budget eliminato restano nello storico.

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: CategoryKind,
    val fill: FillMode,
    val quota: Long,
    val target: Long?,
    val deadline: LocalDate?,
    @ColumnInfo(name = "sort_order") val order: Int,
)

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amount: Long,
    @ColumnInfo(name = "category_id", index = true) val categoryId: String,
    @ColumnInfo(name = "sort_order") val order: Int,
)

@Entity(
    tableName = "movements",
    indices = [Index("date"), Index("category_id")],
)
data class MovementEntity(
    @PrimaryKey val id: String,
    val kind: MovementKind,
    val date: LocalDate,
    val amount: Long,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    val note: String?,
    val total: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /** Solo stipendi: mese di riferimento "yyyy-MM" se diverso da quello della data (dalla versione 2). */
    @ColumnInfo(name = "ref_month") val refMonth: YearMonth? = null,
)

/** Quota di un movimento verso un budget, con segno. Si cancella insieme al movimento. */
@Entity(
    tableName = "movement_allocations",
    primaryKeys = ["movement_id", "category_id"],
    foreignKeys = [
        ForeignKey(
            entity = MovementEntity::class,
            parentColumns = ["id"],
            childColumns = ["movement_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("category_id")],
)
data class MovementAllocationEntity(
    @ColumnInfo(name = "movement_id") val movementId: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    val amount: Long,
)
