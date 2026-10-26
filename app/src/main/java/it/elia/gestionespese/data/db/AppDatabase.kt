package it.elia.gestionespese.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration

@Database(
    entities = [
        CategoryEntity::class,
        ShortcutEntity::class,
        MovementEntity::class,
        MovementAllocationEntity::class,
    ],
    version = 1,
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
        val MIGRATIONS: Array<Migration> = arrayOf()

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
