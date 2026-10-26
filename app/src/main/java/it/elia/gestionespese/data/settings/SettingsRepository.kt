package it.elia.gestionespese.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import it.elia.gestionespese.core.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Nome del file DataStore: files/datastore/settings.preferences_pb (incluso nel backup Android). */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Impostazioni dell'app (SPEC.md §3, Settings). */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(payday = prefs[PAYDAY] ?: Settings.DEFAULT_PAYDAY)
    }

    suspend fun setPayday(day: Int) {
        require(day in 1..31) { "Giorno di stipendio non valido: $day" }
        store.edit { it[PAYDAY] = day }
    }

    suspend fun replace(settings: Settings) = setPayday(settings.payday)

    private companion object {
        val PAYDAY = intPreferencesKey("payday")
    }
}
