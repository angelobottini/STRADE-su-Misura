package it.stradesumisura.app.platform

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import it.stradesumisura.app.AppContainer
import it.stradesumisura.app.PlatformInfo
import it.stradesumisura.app.data.KeyValueStore
import it.stradesumisura.app.data.db.AppDatabase
import it.stradesumisura.app.pdf.DesktopPdfBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.awt.Frame
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

/** Stato condiviso tra la finestra di Windows e le schermate. */
object DesktopEnvironment {
    /** Cartella con database, impostazioni e report temporanei. */
    lateinit var dataDir: File
        private set

    /** true quando i dati sono nella cartella «Dati» accanto al programma (versione portatile). */
    var portable: Boolean = false
        private set

    private val unsavedScreens = MutableStateFlow(0)

    /** Schermate con modifiche non salvate: la chiusura della finestra chiede conferma. */
    val hasUnsavedChanges: Boolean get() = unsavedScreens.value > 0

    fun init(dataDir: File, portable: Boolean) {
        this.dataDir = dataDir
        this.portable = portable
    }

    internal fun markUnsaved(active: Boolean) = unsavedScreens.update { (it + if (active) 1 else -1).coerceAtLeast(0) }
}

/** Finestra principale, proprietaria delle finestre di dialogo di sistema (salvataggio, stampa). */
val LocalAppWindow = staticCompositionLocalOf<Frame?> { null }

/** Crea il contenitore delle dipendenze della versione Windows con i dati in [dataDir]. */
fun createDesktopContainer(dataDir: File, versionName: String): AppContainer {
    // I report temporanei contengono dati sanitari: si eliminano a ogni avvio.
    val reportsDir = File(dataDir, "Temporanei").apply {
        deleteRecursively()
        mkdirs()
    }
    return AppContainer(
        databaseFactory = {
            Room.databaseBuilder<AppDatabase>(name = File(dataDir, AppDatabase.NAME).absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        },
        settingsStorage = PropertiesStore(File(dataDir, "impostazioni.properties")),
        pdfBackend = DesktopPdfBackend(),
        reportsDir = reportsDir,
        platform = PlatformInfo(versionName = versionName, deviceNoun = "computer"),
    )
}

/** Impostazioni in un file di proprietà UTF-8, scritto in modo atomico. */
class PropertiesStore(private val file: File) : KeyValueStore {

    private val properties = Properties().apply {
        if (file.exists()) runCatching { file.reader(Charsets.UTF_8).use { load(it) } }
    }

    @Synchronized
    override fun getString(key: String, default: String): String = properties.getProperty(key) ?: default

    @Synchronized
    override fun getBoolean(key: String, default: Boolean): Boolean =
        properties.getProperty(key)?.toBooleanStrictOrNull() ?: default

    @Synchronized
    override fun putAll(values: Map<String, Any>) {
        values.forEach { (key, value) -> properties.setProperty(key, value.toString()) }
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writer(Charsets.UTF_8).use { properties.store(it, "Impostazioni di STRADE su Misura") }
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}
