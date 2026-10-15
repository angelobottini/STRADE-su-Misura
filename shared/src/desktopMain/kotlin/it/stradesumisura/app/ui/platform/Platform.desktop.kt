package it.stradesumisura.app.ui.platform

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.stradesumisura.app.platform.DesktopEnvironment
import it.stradesumisura.app.platform.LocalAppWindow
import it.stradesumisura.app.resources.Res
import it.stradesumisura.app.resources.ic_folder_open
import it.stradesumisura.app.resources.ic_open_in_new
import it.stradesumisura.app.resources.ic_print
import it.stradesumisura.app.resources.ic_save
import it.stradesumisura.app.resources.roboto_bold
import it.stradesumisura.app.resources.roboto_italic
import it.stradesumisura.app.resources.roboto_light
import it.stradesumisura.app.resources.roboto_medium
import it.stradesumisura.app.resources.roboto_regular
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.printing.PDFPageable
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.print.PrinterJob
import java.io.File

/** Larghezza in pixel delle pagine dell'anteprima (nitide anche con lo schermo ridimensionato al 150%). */
private const val PREVIEW_WIDTH_PX = 1700

/** Ultima cartella usata per salvare un report, proposta al salvataggio successivo. */
private var lastSaveDirectory: File? = null

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

@Composable
actual fun UnsavedChangesGuard(active: Boolean) {
    DisposableEffect(active) {
        if (active) DesktopEnvironment.markUnsaved(true)
        onDispose { if (active) DesktopEnvironment.markUnsaved(false) }
    }
}

@Composable
actual fun PdfReportActions(
    file: File,
    fileName: String,
    subject: String,
    onMessage: (String) -> Unit,
    modifier: Modifier,
) {
    val window = LocalAppWindow.current
    val scope = rememberCoroutineScope()
    var printing by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Report pronto: salvalo, aprilo nel lettore PDF o stampalo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(
            onClick = {
                scope.launch {
                    val opened = withContext(Dispatchers.IO) { runCatching { Desktop.getDesktop().open(file) }.isSuccess }
                    if (!opened) onMessage("Nessun programma per aprire i PDF: salva il report e aprilo dalla cartella scelta.")
                }
            },
        ) { ActionLabel(painterResource(Res.drawable.ic_open_in_new), "Apri") }
        OutlinedButton(
            enabled = !printing,
            onClick = {
                printing = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { printPdf(file, subject) } }
                    printing = false
                    result.fold(
                        onSuccess = { printed -> if (printed) onMessage("Report inviato alla stampante") },
                        onFailure = { onMessage("Stampa non riuscita: ${it.message ?: "errore della stampante"}") },
                    )
                }
            },
        ) { ActionLabel(painterResource(Res.drawable.ic_print), "Stampa…") }
        Button(
            onClick = {
                val target = runCatching { savePdf(window, file, fileName) }
                target.fold(
                    onSuccess = { saved -> if (saved != null) onMessage("PDF salvato in ${saved.absolutePath}") },
                    onFailure = { onMessage("Salvataggio non riuscito: ${it.message ?: "errore sconosciuto"}") },
                )
            },
        ) { ActionLabel(painterResource(Res.drawable.ic_save), "Salva PDF…") }
    }
}

@Composable
private fun ActionLabel(icon: Painter, text: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(text)
}

/** Finestra «Salva con nome» di Windows; restituisce il file salvato o null se l'utente annulla. */
private fun savePdf(owner: Frame?, file: File, fileName: String): File? {
    val dialog = FileDialog(owner, "Salva il report PDF", FileDialog.SAVE)
    val directory = lastSaveDirectory?.takeIf { it.isDirectory } ?: defaultDocumentsDirectory()
    dialog.directory = directory.absolutePath
    dialog.file = fileName
    dialog.isVisible = true
    val name = dialog.file ?: return null
    val folder = dialog.directory ?: return null
    val target = File(folder, if (name.endsWith(".pdf", ignoreCase = true)) name else "$name.pdf")
    file.copyTo(target, overwrite = true)
    lastSaveDirectory = target.parentFile
    return target
}

private fun defaultDocumentsDirectory(): File {
    val home = File(System.getProperty("user.home"))
    return listOf(File(home, "Documents"), File(home, "Documenti"), home).first { it.isDirectory }
}

/** Finestra di stampa di Windows; restituisce false se l'utente annulla. */
private fun printPdf(file: File, jobName: String): Boolean =
    Loader.loadPDF(file).use { document ->
        val job = PrinterJob.getPrinterJob()
        job.jobName = jobName
        job.setPageable(PDFPageable(document))
        if (job.printDialog()) {
            job.print()
            true
        } else {
            false
        }
    }

@Composable
actual fun PlatformSettingsSection() {
    val dataDir = DesktopEnvironment.dataDir
    val scope = rememberCoroutineScope()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Dati del programma", style = MaterialTheme.typography.titleMedium)
            Text(
                if (DesktopEnvironment.portable) {
                    "Versione portatile: archivio delle persone, valutazioni e impostazioni sono nella cartella " +
                        "«Dati» accanto al programma. Per usare STRADE su Misura su un altro computer o da una " +
                        "chiavetta USB copia l'intera cartella del programma."
                } else {
                    "La cartella del programma non è scrivibile, quindi i dati sono salvati nel profilo dell'utente " +
                        "di Windows."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SelectionContainer {
                Text(dataDir.absolutePath, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Per una copia di sicurezza chiudi il programma e copia la cartella dei dati. I report generati " +
                    "per l'anteprima vengono eliminati a ogni avvio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { scope.launch(Dispatchers.IO) { runCatching { Desktop.getDesktop().open(dataDir) } } },
            ) { ActionLabel(painterResource(Res.drawable.ic_folder_open), "Apri la cartella dei dati") }
        }
    }
}

@Composable
actual fun platformFontFamily(): FontFamily = FontFamily(
    Font(Res.font.roboto_light, FontWeight.Light),
    Font(Res.font.roboto_regular, FontWeight.Normal),
    Font(Res.font.roboto_italic, FontWeight.Normal, FontStyle.Italic),
    Font(Res.font.roboto_medium, FontWeight.Medium),
    // Roboto statico non ha il semigrassetto: i titoli usano il medio, come nel report.
    Font(Res.font.roboto_medium, FontWeight.SemiBold),
    Font(Res.font.roboto_bold, FontWeight.Bold),
    Font(Res.font.roboto_bold, FontWeight.Black),
)

@Composable
private fun stradeScrollbarStyle(): ScrollbarStyle = ScrollbarStyle(
    minimalHeight = 24.dp,
    thickness = 8.dp,
    shape = RoundedCornerShape(4.dp),
    hoverDurationMillis = 250,
    unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
    hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
)

@Composable
actual fun VerticalScrollbarFor(state: LazyListState, modifier: Modifier) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier.padding(end = 3.dp, top = 4.dp, bottom = 4.dp),
        style = stradeScrollbarStyle(),
    )
}

@Composable
actual fun VerticalScrollbarFor(state: ScrollState, modifier: Modifier) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier.padding(end = 3.dp, top = 4.dp, bottom = 4.dp),
        style = stradeScrollbarStyle(),
    )
}

actual fun renderPdfPages(file: File): List<ImageBitmap> =
    Loader.loadPDF(file).use { document ->
        val renderer = PDFRenderer(document)
        (0 until document.numberOfPages).map { index ->
            val scale = PREVIEW_WIDTH_PX / document.getPage(index).mediaBox.width
            renderer.renderImage(index, scale, ImageType.RGB).toComposeImageBitmap()
        }
    }
