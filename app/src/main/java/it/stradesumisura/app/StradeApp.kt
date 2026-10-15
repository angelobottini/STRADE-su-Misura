package it.stradesumisura.app

import android.app.Application
import it.stradesumisura.app.platform.createAndroidContainer

/**
 * Applicazione Android: crea il contenitore delle dipendenze (database, repository, impostazioni,
 * motore PDF) del modulo condiviso con la versione Windows.
 */
class StradeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContainer.instance = createAndroidContainer(this)
    }
}
