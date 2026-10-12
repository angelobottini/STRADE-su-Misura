package it.stradesumisura.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.stradesumisura.app.R
import it.stradesumisura.app.pdf.PdfFiles
import it.stradesumisura.app.ui.components.EmptyState
import it.stradesumisura.app.ui.components.StradeTopBar
import it.stradesumisura.app.ui.viewmodel.AppViewModelFactory
import it.stradesumisura.app.ui.viewmodel.PdfPreviewViewModel
import kotlinx.coroutines.launch

@Composable
fun PdfPreviewScreen(
    onBack: () -> Unit,
    viewModel: PdfPreviewViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val chooserTitle = stringResource(R.string.share_pdf_title)

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val file = state.file
        if (uri != null && file != null) {
            val saved = runCatching { PdfFiles.copyTo(context, file, uri) }.isSuccess
            scope.launch {
                snackbarHostState.showSnackbar(if (saved) "PDF salvato sul dispositivo" else "Salvataggio non riuscito")
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            StradeTopBar(
                title = "Report PDF",
                subtitle = state.fileName.takeIf { it.isNotBlank() },
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val file = state.file
            if (file != null) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Row(
                        Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { saveLauncher.launch(state.fileName) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                        ) { Text("Salva") }
                        Button(
                            onClick = { PdfFiles.share(context, file, state.subject, chooserTitle) },
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
            }
        },
    ) { padding ->
        val error = state.error
        when {
            state.loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Generazione del report in corso…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            error != null -> EmptyState(
                title = "Impossibile generare il PDF",
                message = error,
                modifier = Modifier.padding(padding),
                action = { Button(onClick = viewModel::generate) { Text("Riprova") } },
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.pages.size) { index ->
                    Image(
                        bitmap = state.pages[index].asImageBitmap(),
                        contentDescription = "Pagina ${index + 1}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(4.dp))
                            .background(Color.White),
                    )
                }
                item {
                    Text(
                        if (state.pages.size == 1) "1 pagina · formato A4" else "${state.pages.size} pagine · formato A4",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
