package it.stradesumisura.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.stradesumisura.app.data.db.PersonEntity
import it.stradesumisura.app.domain.ToolRegistry
import it.stradesumisura.app.ui.components.AssessmentListItem
import it.stradesumisura.app.ui.components.ConfirmDialog
import it.stradesumisura.app.ui.components.EmptyState
import it.stradesumisura.app.ui.components.InitialsAvatar
import it.stradesumisura.app.ui.components.LabeledValue
import it.stradesumisura.app.ui.components.SectionHeader
import it.stradesumisura.app.ui.components.ShortNameBadge
import it.stradesumisura.app.ui.components.StradeLazyColumn
import it.stradesumisura.app.ui.components.StradeTopBar
import it.stradesumisura.app.ui.components.assessmentSummary
import it.stradesumisura.app.ui.components.personMeta
import it.stradesumisura.app.ui.viewmodel.AppViewModelFactory
import it.stradesumisura.app.ui.viewmodel.PersonDetailViewModel
import it.stradesumisura.app.util.Dates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onNewAssessment: (toolId: String, personId: String) -> Unit,
    onOpenAssessment: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: PersonDetailViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val person = state.person
    var showDelete by remember { mutableStateOf(false) }
    var showToolPicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StradeTopBar(
                title = person?.fullName ?: "Persona",
                subtitle = person?.let { personMeta(it) },
                onBack = onBack,
                actions = {
                    if (person != null) {
                        IconButton(onClick = { onEdit(person.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifica")
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (person != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val tools = ToolRegistry.tools
                        if (tools.size == 1) {
                            onNewAssessment(tools.first().id, person.id)
                        } else {
                            showToolPicker = true
                        }
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nuova valutazione") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { padding ->
        when {
            state.loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            person == null -> EmptyState(
                title = "Persona non trovata",
                message = "La scheda potrebbe essere stata eliminata.",
                modifier = Modifier.padding(padding),
            )

            else -> StradeLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { PersonHeaderCard(person) }
                if (!person.notes.isNullOrBlank()) {
                    item { NotesCard(title = "Note", text = person.notes) }
                }
                item {
                    SectionHeader(
                        if (state.assessments.isEmpty()) "Valutazioni" else "Valutazioni (${state.assessments.size})",
                    )
                }
                if (state.assessments.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            EmptyState(
                                title = "Nessuna valutazione",
                                message = "Avvia la prima valutazione per questa persona con il pulsante «Nuova valutazione».",
                            )
                        }
                    }
                } else {
                    items(state.assessments, key = { it.id }) { assessment ->
                        val tool = ToolRegistry.byId(assessment.toolId)
                        AssessmentListItem(
                            assessment = assessment,
                            title = tool?.name ?: assessment.toolId,
                            tool = tool,
                            subtitle = listOfNotNull(
                                assessmentSummary(tool, assessment),
                                Dates.formatIsoLong(assessment.date),
                                assessment.evaluator?.takeIf { it.isNotBlank() },
                            ).joinToString(" · "),
                            onClick = { onOpenAssessment(assessment.id) },
                        )
                    }
                }
            }
        }
    }

    if (showDelete && person != null) {
        ConfirmDialog(
            title = "Eliminare la persona?",
            text = "Verranno eliminate anche tutte le valutazioni di ${person.fullName}. L'operazione non può essere annullata.",
            confirmLabel = "Elimina",
            onConfirm = {
                showDelete = false
                viewModel.delete(onDeleted)
            },
            onDismiss = { showDelete = false },
        )
    }

    if (showToolPicker && person != null) {
        ModalBottomSheet(onDismissRequest = { showToolPicker = false }) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Scegli lo strumento", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                ToolRegistry.tools.forEach { tool ->
                    Surface(
                        onClick = {
                            showToolPicker = false
                            onNewAssessment(tool.id, person.id)
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            ShortNameBadge(tool.shortName)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(tool.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    tool.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PersonHeaderCard(person: PersonEntity) {
    val birth = Dates.parse(person.birthDate)
    val age = Dates.ageAt(birth)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(person.initials, size = 60.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(person.fullName, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        personMeta(person),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(14.dp))
            Row {
                LabeledValue(
                    label = "Nascita",
                    value = if (birth != null) "${Dates.formatShort(birth)}${age?.let { " · $it anni" } ?: ""}" else "—",
                    modifier = Modifier.weight(1f),
                )
                LabeledValue(
                    label = "Sesso",
                    value = when (person.sex) {
                        "M" -> "Maschio"
                        "F" -> "Femmina"
                        "X" -> "Altro"
                        else -> "—"
                    },
                    modifier = Modifier.weight(0.7f),
                )
                LabeledValue(
                    label = "Codice",
                    value = person.code?.takeIf { it.isNotBlank() } ?: "—",
                    modifier = Modifier.weight(0.8f),
                )
            }
        }
    }
}
