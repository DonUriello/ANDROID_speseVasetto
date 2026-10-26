package it.elia.gestionespese.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        ShortcutEntity::class,
        MovementEntity::class,
        MovementAllocationEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun shortcutDao(): ShortcutDao
    abstract fun movementDao(): MovementDao

    companion object {
        const val NAME = "gestione-spese.db"

        /**
         * Migrazioni tra versioni dello schema (schemi esportati in app/schemas).
         * Non si usa mai fallbackToDestructiveMigration: i dati di Elia non devono andare persi.
         */
        val MIGRATIONS: Array<Migration> = arrayOf(
            // 1 → 2: mese di riferimento dello stipendio
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE movements ADD COLUMN ref_month TEXT")
                }
            },
        )

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
