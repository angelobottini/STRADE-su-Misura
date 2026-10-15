package it.stradesumisura.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.stradesumisura.app.AppContainer
import it.stradesumisura.app.ui.components.StradeMark
import it.stradesumisura.app.ui.components.StradeTopBar
import it.stradesumisura.app.ui.platform.PlatformSettingsSection
import it.stradesumisura.app.ui.platform.VerticalScrollbarFor
import it.stradesumisura.app.ui.viewmodel.AppViewModelFactory
import it.stradesumisura.app.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory),
) {
    val saved by viewModel.profile.collectAsStateWithLifecycle()
    var draft by remember(saved) { mutableStateOf(saved) }
    val isDirty = draft != saved
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val platform = remember { AppContainer.instance.platform }
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { StradeTopBar(title = "Impostazioni", subtitle = "Profilo del valutatore e informazioni") },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Profilo del valutatore", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Questi dati vengono proposti nelle nuove valutazioni e stampati nell'intestazione e nella firma dei report PDF.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = { draft = draft.copy(name = it) },
                            label = { Text("Nome e cognome") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = draft.qualification,
                            onValueChange = { draft = draft.copy(qualification = it) },
                            label = { Text("Qualifica") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = draft.organization,
                            onValueChange = { draft = draft.copy(organization = it) },
                            label = { Text("Struttura o studio") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = draft.place,
                            onValueChange = { draft = draft.copy(place = it) },
                            label = { Text("Luogo (per la firma)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = draft.includeSignature,
                                onCheckedChange = { draft = draft.copy(includeSignature = it) },
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Includi lo spazio per la firma nel PDF", style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(
                            onClick = {
                                viewModel.save(draft)
                                scope.launch { snackbarHostState.showSnackbar("Impostazioni salvate") }
                            },
                            enabled = isDirty,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) { Text("Salva impostazioni") }
                    }
                }

                PlatformSettingsSection()

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        StradeMark(height = 56.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("STRADE su Misura", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Versione ${platform.versionName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Strumenti e scale di valutazione per terapisti occupazionali. Tutti i dati restano sul " +
                                "${platform.deviceNoun}: nessuna informazione viene inviata a server esterni. I report PDF vengono " +
                                "generati localmente e condivisi solo quando lo decidi tu.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Carattere tipografico dei report: Roboto © Google, Apache License 2.0.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            VerticalScrollbarFor(
                state = scrollState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
            )
        }
    }
}
