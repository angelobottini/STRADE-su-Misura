package it.stradesumisura.desktop

import java.io.File

/**
 * Dove il programma salva database, impostazioni e report temporanei.
 *
 * Versione portatile: la cartella «Dati» accanto a «STRADE su Misura.exe», così il programma
 * funziona senza installazione e si può copiare insieme ai dati (anche su una chiavetta USB).
 * Se quella cartella non è scrivibile (es. programma copiato in «Programmi»), i dati vanno in
 * %LOCALAPPDATA%\STRADE su Misura.
 */
internal data class DataLocation(val dataDir: File, val portable: Boolean) {

    companion object {
        private const val PORTABLE_FOLDER = "Dati"

        fun resolve(): DataLocation {
            // Impostata dal lanciatore creato da jpackage: percorso di «STRADE su Misura.exe».
            val launcher = System.getProperty("jpackage.app-path")?.let(::File)
            if (launcher != null) {
                val portableDir = File(launcher.absoluteFile.parentFile, PORTABLE_FOLDER)
                if (isWritable(portableDir)) return DataLocation(portableDir, portable = true)
                val localAppData = System.getenv("LOCALAPPDATA")?.let(::File)
                    ?: File(System.getProperty("user.home"), "AppData/Local")
                return DataLocation(File(localAppData, "STRADE su Misura").apply { mkdirs() }, portable = false)
            }
            // Avvio dallo sviluppo (gradle run): cartella separata nel profilo utente.
            val devDir = File(System.getProperty("user.home"), ".strade-su-misura-sviluppo").apply { mkdirs() }
            return DataLocation(devDir, portable = false)
        }

        private fun isWritable(dir: File): Boolean = runCatching {
            dir.mkdirs()
            val probe = File(dir, ".verifica-scrittura")
            probe.writeText("ok")
            probe.delete()
        }.getOrDefault(false)
    }
}
