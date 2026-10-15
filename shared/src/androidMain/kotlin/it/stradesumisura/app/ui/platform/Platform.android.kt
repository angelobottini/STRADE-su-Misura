package it.stradesumisura.app.ui.platform

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import it.stradesumisura.app.pdf.PdfFiles
import java.io.File

private const val PREVIEW_WIDTH_PX = 1080

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

@Composable
actual fun UnsavedChangesGuard(active: Boolean) = Unit

@Composable
actual fun PdfReportActions(
    file: File,
    fileName: String,
    subject: String,
    onMessage: (String) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            val saved = runCatching { PdfFiles.copyTo(context, file, uri) }.isSuccess
            onMessage(if (saved) "PDF salvato sul dispositivo" else "Salvataggio non riuscito")
        }
    }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = { saveLauncher.launch(fileName) },
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
        ) { Text("Salva") }
        Button(
            onClick = { PdfFiles.share(context, file, subject, "Condividi report PDF") },
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Condividi")
        }
    }
}

@Composable
actual fun PlatformSettingsSection() = Unit

@Composable
actual fun platformFontFamily(): FontFamily = FontFamily.SansSerif

@Composable
actual fun VerticalScrollbarFor(state: LazyListState, modifier: Modifier) = Unit

@Composable
actual fun VerticalScrollbarFor(state: ScrollState, modifier: Modifier) = Unit

actual fun renderPdfPages(file: File): List<ImageBitmap> =
    PdfFiles.renderPages(file, PREVIEW_WIDTH_PX).map { it.asImageBitmap() }
