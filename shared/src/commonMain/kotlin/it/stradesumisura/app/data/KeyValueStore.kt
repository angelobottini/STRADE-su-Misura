package it.stradesumisura.app.data

/**
 * Archivio chiave-valore delle impostazioni: SharedPreferences su Android, file di
 * proprietà nella cartella dei dati su Windows.
 */
interface KeyValueStore {
    fun getString(key: String, default: String): String
    fun getBoolean(key: String, default: Boolean): Boolean

    /** Scrive insieme tutti i valori (String o Boolean). */
    fun putAll(values: Map<String, Any>)
}
