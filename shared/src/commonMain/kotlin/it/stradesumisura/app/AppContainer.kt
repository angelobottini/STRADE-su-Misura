package it.stradesumisura.app

import it.stradesumisura.app.data.KeyValueStore
import it.stradesumisura.app.data.SettingsStore
import it.stradesumisura.app.data.StradeRepository
import it.stradesumisura.app.data.db.AppDatabase
import it.stradesumisura.app.pdf.PdfBackend
import java.io.File

/** Informazioni sulla piattaforma mostrate nell'interfaccia. */
data class PlatformInfo(
    val versionName: String,
    /** «dispositivo» su Android, «computer» su Windows: usato nei testi sulla riservatezza dei dati. */
    val deviceNoun: String,
)

/**
 * Contenitore manuale delle dipendenze: database, repository, impostazioni e motore PDF vengono
 * creati in un unico punto, senza framework di dependency injection. Ogni piattaforma lo crea
 * all'avvio (Application su Android, main() su Windows) e lo registra in [instance].
 */
class AppContainer(
    databaseFactory: () -> AppDatabase,
    settingsStorage: KeyValueStore,
    val pdfBackend: PdfBackend,
    /** Cartella dei PDF generati per l'anteprima, la condivisione e il salvataggio. */
    val reportsDir: File,
    val platform: PlatformInfo,
) {
    val database: AppDatabase by lazy(databaseFactory)
    val repository: StradeRepository by lazy { StradeRepository(database) }
    val settings: SettingsStore by lazy { SettingsStore(settingsStorage) }

    companion object {
        @Volatile
        private var current: AppContainer? = null

        var instance: AppContainer
            get() = checkNotNull(current) { "AppContainer non inizializzato" }
            set(value) {
                current = value
            }
    }
}
