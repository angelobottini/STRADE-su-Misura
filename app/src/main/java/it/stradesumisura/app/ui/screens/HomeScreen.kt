package it.stradesumisura.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.stradesumisura.app.R
import it.stradesumisura.app.domain.ToolRegistry
import it.stradesumisura.app.ui.components.AssessmentListItem
import it.stradesumisura.app.ui.components.EmptyState
import it.stradesumisura.app.ui.components.SectionHeader
import it.stradesumisura.app.ui.components.StatCard
import it.stradesumisura.app.ui.components.StradeMark
import it.stradesumisura.app.ui.components.ToolCard
import it.stradesumisura.app.ui.viewmodel.AppViewModelFactory
import it.stradesumisura.app.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onOpenPeople: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTool: (String) -> Unit,
    onStartAssessment: (String) -> Unit,
    onOpenAssessment: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        StradeMark(height = 76.dp)
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Impostazioni")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "Persone in archivio",
                        value = state.peopleCount.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenPeople,
                    )
                    StatCard(
                        label = "Valutazioni salvate",
                        value = state.assessmentCount.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenPeople,
                    )
                }
            }
            item { SectionHeader("Strumenti") }
            items(state.tools, key = { it.id }) { tool ->
                ToolCard(tool = tool, onOpen = { onOpenTool(tool.id) }, onStart = { onStartAssessment(tool.id) })
            }
            item {
                SectionHeader(
                    "Valutazioni recenti",
                    action = if (state.recent.isNotEmpty()) {
                        { TextButton(onClick = onOpenPeople) { Text("Archivio") } }
                    } else {
                        null
                    },
                )
            }
            if (state.recent.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        EmptyState(
                            title = "Nessuna valutazione",
                            message = "Aggiungi una persona all'archivio e avvia la prima valutazione con l'Indice di Barthel Modificato.",
                        )
                    }
                }
            } else {
                items(state.recent, key = { it.assessment.id }) { item ->
                    AssessmentListItem(
                        assessment = item.assessment,
                        title = item.person.fullName,
                        tool = ToolRegistry.byId(item.assessment.toolId),
                        onClick = { onOpenAssessment(item.assessment.id) },
                    )
                }
            }
        }
    }
}
