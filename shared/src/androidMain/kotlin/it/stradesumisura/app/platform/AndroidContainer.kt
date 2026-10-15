package it.stradesumisura.app.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.Room
import it.stradesumisura.app.AppContainer
import it.stradesumisura.app.PlatformInfo
import it.stradesumisura.app.data.KeyValueStore
import it.stradesumisura.app.data.db.AppDatabase
import it.stradesumisura.app.pdf.AndroidPdfBackend
import java.io.File

/** Crea il contenitore delle dipendenze dell'app Android. */
fun createAndroidContainer(context: Context): AppContainer {
    val app = context.applicationContext
    val versionName = runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionName }.getOrNull() ?: "—"
    return AppContainer(
        databaseFactory = { Room.databaseBuilder(app, AppDatabase::class.java, AppDatabase.NAME).build() },
        settingsStorage = SharedPreferencesStore(app.getSharedPreferences("strade_settings", Context.MODE_PRIVATE)),
        pdfBackend = AndroidPdfBackend(app),
        // Cartella condivisa tramite FileProvider (res/xml/file_paths.xml dell'app).
        reportsDir = File(app.cacheDir, "pdf"),
        platform = PlatformInfo(versionName = versionName, deviceNoun = "dispositivo"),
    )
}

private class SharedPreferencesStore(private val prefs: SharedPreferences) : KeyValueStore {

    override fun getString(key: String, default: String): String = prefs.getString(key, default) ?: default

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    override fun putAll(values: Map<String, Any>) {
        prefs.edit {
            values.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
    }
}
