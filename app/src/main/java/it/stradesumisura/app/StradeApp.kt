package it.stradesumisura.app

import android.app.Application
import it.stradesumisura.app.data.SettingsStore
import it.stradesumisura.app.data.StradeRepository
import it.stradesumisura.app.data.db.AppDatabase

/**
 * Contenitore manuale delle dipendenze: database, repository e impostazioni vengono creati
 * in un unico punto, senza framework di dependency injection.
 */
class AppContainer(val application: Application) {
    val database: AppDatabase by lazy { AppDatabase.create(application) }
    val repository: StradeRepository by lazy { StradeRepository(database) }
    val settings: SettingsStore by lazy { SettingsStore(application) }
}

class StradeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
