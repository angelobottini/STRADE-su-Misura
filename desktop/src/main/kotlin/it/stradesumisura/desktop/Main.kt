package it.stradesumisura.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import it.stradesumisura.app.AppContainer
import it.stradesumisura.app.AppTexts
import it.stradesumisura.app.platform.DesktopEnvironment
import it.stradesumisura.app.platform.LocalAppWindow
import it.stradesumisura.app.platform.PropertiesStore
import it.stradesumisura.app.platform.createDesktopContainer
import it.stradesumisura.app.ui.components.ConfirmDialog
import it.stradesumisura.app.ui.navigation.StradeNavigation
import it.stradesumisura.app.ui.theme.StradeTheme
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.time.LocalDateTime
import javax.imageio.ImageIO
import javax.swing.JOptionPane
import javax.swing.UIManager
import kotlin.system.exitProcess

/** Blocco sul file «.in-uso» della cartella dei dati: impedisce due finestre sullo stesso archivio. */
private var instanceLock: FileLock? = null

fun main() {
    runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }

    val location = DataLocation.resolve()
    instanceLock = runCatching { RandomAccessFile(File(location.dataDir, ".in-uso"), "rw").channel.tryLock() }.getOrNull()
    if (instanceLock == null) {
        JOptionPane.showMessageDialog(
            null,
            "STRADE su Misura è già aperto.\nUsa la finestra già aperta oppure chiudila prima di avviare di nuovo il programma.",
            AppTexts.APP_NAME,
            JOptionPane.INFORMATION_MESSAGE,
        )
        exitProcess(0)
    }
    installErrorLog(location.dataDir)

    DesktopEnvironment.init(location.dataDir, location.portable)
    AppContainer.instance = createDesktopContainer(location.dataDir, BuildInfo.VERSION_NAME)
    val windowPrefs = PropertiesStore(File(location.dataDir, "finestra.properties"))
    val icon = loadIcon()

    application {
        StradeWindow(icon, windowPrefs)
    }
}

@Composable
private fun ApplicationScope.StradeWindow(icon: Painter?, prefs: PropertiesStore) {
    val initial = remember { initialWindow(prefs) }
    val state = rememberWindowState(
        placement = initial.placement,
        position = WindowPosition(Alignment.Center),
        size = initial.size,
    )
    var confirmClose by remember { mutableStateOf(false) }

    fun close() {
        saveWindow(prefs, state)
        exitApplication()
    }

    Window(
        onCloseRequest = { if (DesktopEnvironment.hasUnsavedChanges) confirmClose = true else close() },
        state = state,
        title = AppTexts.APP_NAME,
        icon = icon,
    ) {
        LaunchedEffect(Unit) { window.minimumSize = minimumWindowSize() }
        val dark = isSystemInDarkTheme()
        CompositionLocalProvider(LocalAppWindow provides window) {
            StradeTheme(darkTheme = dark) {
                val caption = MaterialTheme.colorScheme.background
                val captionText = MaterialTheme.colorScheme.onBackground
                LaunchedEffect(caption, captionText, dark) {
                    WindowsChrome.applyColors(window, caption, captionText, dark)
                }
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    StradeNavigation()
                }
                if (confirmClose) {
                    ConfirmDialog(
                        title = "Chiudere STRADE su Misura?",
                        text = "La valutazione aperta ha modifiche non salvate: chiudendo il programma andranno perse.",
                        confirmLabel = "Chiudi senza salvare",
                        onConfirm = {
                            confirmClose = false
                            close()
                        },
                        onDismiss = { confirmClose = false },
                    )
                }
            }
        }
    }
}

private data class InitialWindow(val size: DpSize, val placement: WindowPlacement)

/** Dimensioni dell'ultima sessione, adattate allo schermo attuale. */
private fun initialWindow(prefs: PropertiesStore): InitialWindow {
    val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    val width = prefs.getString(KEY_WIDTH, "").toFloatOrNull() ?: 1320f
    val height = prefs.getString(KEY_HEIGHT, "").toFloatOrNull() ?: 860f
    val size = DpSize(
        width.coerceAtMost(screen.width * 0.95f).dp,
        height.coerceAtMost(screen.height * 0.95f).dp,
    )
    val placement = if (prefs.getBoolean(KEY_MAXIMIZED, false)) WindowPlacement.Maximized else WindowPlacement.Floating
    return InitialWindow(size, placement)
}

private fun saveWindow(prefs: PropertiesStore, state: WindowState) {
    val maximized = state.placement == WindowPlacement.Maximized
    val values = mutableMapOf<String, Any>(KEY_MAXIMIZED to maximized)
    if (state.placement == WindowPlacement.Floating) {
        values[KEY_WIDTH] = state.size.width.value.toInt()
        values[KEY_HEIGHT] = state.size.height.value.toInt()
    }
    runCatching { prefs.putAll(values) }
}

private fun minimumWindowSize(): Dimension {
    val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    return Dimension(minOf(980, screen.width), minOf(640, screen.height))
}

private fun loadIcon(): Painter? = runCatching {
    val stream = checkNotNull(object {}.javaClass.getResourceAsStream("/strade-icon.png"))
    stream.use { BitmapPainter(ImageIO.read(it).toComposeImageBitmap()) }
}.getOrNull()

/** Gli errori imprevisti vengono annotati in «registro-errori.txt» nella cartella dei dati. */
private fun installErrorLog(dataDir: File) {
    val log = File(dataDir, "registro-errori.txt")
    Thread.setDefaultUncaughtExceptionHandler { _, error ->
        runCatching {
            log.appendText("---- ${LocalDateTime.now()} · versione ${BuildInfo.VERSION_NAME}\n${error.stackTraceToString()}\n")
        }
        runCatching {
            JOptionPane.showMessageDialog(
                null,
                "Si è verificato un errore imprevisto.\nI dettagli sono stati salvati in:\n${log.absolutePath}",
                AppTexts.APP_NAME,
                JOptionPane.ERROR_MESSAGE,
            )
        }
    }
}

private const val KEY_WIDTH = "larghezza"
private const val KEY_HEIGHT = "altezza"
private const val KEY_MAXIMIZED = "massimizzata"
