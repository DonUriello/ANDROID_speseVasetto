package it.elia.gestionespese

import android.app.Application
import it.elia.gestionespese.data.SpeseRepository
import it.elia.gestionespese.data.db.AppDatabase
import it.elia.gestionespese.data.settings.SettingsRepository

/** Punto di creazione delle dipendenze (iniezione manuale, senza framework). */
class GestioneSpeseApp : Application() {
    val repository: SpeseRepository by lazy {
        SpeseRepository(AppDatabase.build(this), SettingsRepository(this))
    }
}
