package it.stradesumisura.app.ui.platform

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import java.io.File

/** Intercetta il tasto o il gesto «Indietro» del sistema (solo Android). */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/** Segnala che la schermata ha modifiche non salvate: su Windows la chiusura della finestra chiede conferma. */
@Composable
expect fun UnsavedChangesGuard(active: Boolean)

/**
 * Azioni sul report generato: salvataggio e condivisione su Android; salvataggio con nome,
 * apertura nel lettore PDF e stampa su Windows. [onMessage] mostra l'esito all'utente.
 */
@Composable
expect fun PdfReportActions(
    file: File,
    fileName: String,
    subject: String,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
)

/** Sezione delle impostazioni propria della piattaforma (su Windows: la cartella dei dati). */
@Composable
expect fun PlatformSettingsSection()

/** Caratteri dell'interfaccia: quelli di sistema su Android, Roboto incluso su Windows. */
@Composable
expect fun platformFontFamily(): FontFamily

/** Barra di scorrimento verticale accanto alle liste (solo Windows). */
@Composable
expect fun VerticalScrollbarFor(state: LazyListState, modifier: Modifier = Modifier)

@Composable
expect fun VerticalScrollbarFor(state: ScrollState, modifier: Modifier = Modifier)

/** Rende le pagine del PDF come immagini per l'anteprima nell'app. */
expect fun renderPdfPages(file: File): List<ImageBitmap>
