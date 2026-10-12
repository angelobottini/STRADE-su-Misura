package it.stradesumisura.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.stradesumisura.app.domain.ScoreResult
import it.stradesumisura.app.domain.Selection
import it.stradesumisura.app.domain.ToolDefinition
import it.stradesumisura.app.domain.ToolItem
import it.stradesumisura.app.domain.ToolOption
import it.stradesumisura.app.ui.components.BandChip
import it.stradesumisura.app.ui.components.ConfirmDialog
import it.stradesumisura.app.ui.components.DateField
import it.stradesumisura.app.ui.components.EmptyState
import it.stradesumisura.app.ui.components.StradeTopBar
import it.stradesumisura.app.ui.viewmodel.AppViewModelFactory
import it.stradesumisura.app.ui.viewmodel.AssessmentFormViewModel

@Composable
fun AssessmentFormScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: AssessmentFormViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showExit by remember { mutableStateOf(false) }
    val tool = state.tool
    val person = state.person
    val score = state.score

    val requestExit: () -> Unit = {
        if (state.dirty) {
            showExit = true
        } else {
            onBack()
        }
    }
    BackHandler(enabled = state.dirty) { showExit = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StradeTopBar(
                title = tool?.name ?: "Valutazione",
                subtitle = person?.fullName,
                onBack = requestExit,
            )
        },
        bottomBar = {
            if (tool != null && person != null) {
                SaveBar(
                    tool = tool,
                    score = score,
                    isEdit = state.isEdit,
                    saving = state.saving,
                    onSave = { viewModel.save(onSaved) },
                )
            }
        },
    ) { padding ->
        when {
            !state.loaded -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.notFound || tool == null || person == null -> EmptyState(
                title = "Dati non disponibili",
                message = "La persona o lo strumento non sono più disponibili.",
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { ProgressCard(score) }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Dati della valutazione", style = MaterialTheme.typography.titleSmall)
                            DateField(
                                label = "Data della valutazione",
                                date = state.date,
                                onDateChange = viewModel::setDate,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = state.evaluator,
                                onValueChange = viewModel::setEvaluator,
                                label = { Text("Valutatore") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = state.setting,
                                onValueChange = viewModel::setSetting,
                                label = { Text("Contesto") },
                                placeholder = { Text("es. domicilio, reparto, ambulatorio") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            tool.fields.forEach { field ->
                                val hint = field.placeholder
                                OutlinedTextField(
                                    value = state.answers.fields[field.id] ?: "",
                                    onValueChange = { viewModel.setField(field.id, it) },
                                    label = { Text(field.label) },
                                    placeholder = if (hint != null) {
                                        { Text(hint) }
                                    } else {
                                        null
                                    },
                                    singleLine = !field.multiline,
                                    minLines = if (field.multiline) 3 else 1,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
                itemsIndexed(tool.primaryItems, key = { _, item -> item.id }) { index, item ->
                    val alternative = tool.alternativeFor(item.id)
                    val useAlternative = alternative != null && state.usesAlternative(item.id)
                    val shown = if (useAlternative && alternative != null) alternative else item
                    ItemCard(
                        number = index + 1,
                        item = item,
                        alternative = alternative,
                        shown = shown,
                        selection = tool.selectionFor(item, state.answers),
                        useAlternative = useAlternative,
                        note = state.answers.itemNotes[shown.id],
                        onSelect = viewModel::select,
                        onToggleAlternative = { viewModel.setUseAlternative(item.id, it) },
                        onNoteChange = { viewModel.setItemNote(shown.id, it) },
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = viewModel::setNotes,
                        label = { Text(tool.notesLabel) },
                        placeholder = { Text(tool.notesHint ?: "Note cliniche, ausili utilizzati, condizioni della valutazione…") },
                        minLines = 4,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showExit) {
        ConfirmDialog(
            title = "Uscire senza salvare?",
            text = "Le modifiche non salvate andranno perse.",
            confirmLabel = "Esci",
            onConfirm = {
                showExit = false
                onBack()
            },
            onDismiss = { showExit = false },
        )
    }
}

@Composable
private fun ProgressCard(score: ScoreResult?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        "PUNTEGGIO ATTUALE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${score?.total ?: 0}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            " / ${score?.max ?: 0}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "COMPLETAMENTO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        "${score?.answered ?: 0} di ${score?.required ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            val fraction = if (score == null || score.required == 0) 0f else score.answered.toFloat() / score.required
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.18f),
            )
            Spacer(Modifier.height(12.dp))
            val band = score?.band
            if (band != null) {
                BandChip(band)
            } else {
                Text(
                    "Completa tutte le voci per ottenere l'interpretazione del punteggio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ItemCard(
    number: Int,
    item: ToolItem,
    alternative: ToolItem?,
    shown: ToolItem,
    selection: Selection?,
    useAlternative: Boolean,
    note: String?,
    onSelect: (itemId: String, optionIndex: Int) -> Unit,
    onToggleAlternative: (Boolean) -> Unit,
    onNoteChange: (String) -> Unit,
) {
    var showDefinition by rememberSaveable(shown.id) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            if (selection != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            if (selection != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$number",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selection != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(shown.title, style = MaterialTheme.typography.titleMedium)
                    shown.hint?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (selection != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${selection.option.score}/${shown.maxScore}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            val definition = shown.description
            if (definition != null) {
                TextButton(
                    onClick = { showDefinition = !showDefinition },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                ) {
                    Text(if (showDefinition) "Nascondi definizione" else "Mostra definizione")
                }
                if (showDefinition) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            definition,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
            if (alternative != null) {
                Spacer(Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !useAlternative,
                        onClick = { onToggleAlternative(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text(item.title) }
                    SegmentedButton(
                        selected = useAlternative,
                        onClick = { onToggleAlternative(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text(alternative.title) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shown.options.forEachIndexed { index, option ->
                    val selected = selection != null && selection.item.id == shown.id && selection.optionIndex == index
                    OptionRow(option = option, selected = selected, onClick = { onSelect(shown.id, index) })
                }
            }
            val noteLabel = shown.noteLabel
            if (noteLabel != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note ?: "",
                    onValueChange = onNoteChange,
                    label = { Text(noteLabel) },
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OptionRow(option: ToolOption, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 6.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.Top) {
            RadioButton(selected = selected, onClick = null)
            Column(
                Modifier
                    .weight(1f)
                    .padding(top = 10.dp),
            ) {
                Text(option.title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "${option.score}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun SaveBar(tool: ToolDefinition, score: ScoreResult?, isEdit: Boolean, saving: Boolean, onSave: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            if (score != null && !score.isComplete) {
                val missing = score.required - score.answered
                val noun = if (missing == 1) tool.itemNounSingular else tool.itemNounPlural
                val verb = if (missing == 1) "Manca" else "Mancano"
                Text(
                    "$verb $missing $noun: la valutazione verrà salvata come parziale.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = onSave,
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(if (isEdit) "Salva modifiche" else "Salva valutazione")
            }
        }
    }
}
