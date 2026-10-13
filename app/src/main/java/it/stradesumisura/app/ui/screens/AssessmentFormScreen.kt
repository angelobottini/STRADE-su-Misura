package it.stradesumisura.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import it.stradesumisura.app.ui.components.ItemImage
import it.stradesumisura.app.ui.components.SectionLabel
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.stradesumisura.app.domain.AssessmentAnswers
import it.stradesumisura.app.domain.ScoreResult
import it.stradesumisura.app.domain.Selection
import it.stradesumisura.app.domain.ToolDefinition
import it.stradesumisura.app.domain.ToolField
import it.stradesumisura.app.domain.ToolItem
import it.stradesumisura.app.domain.ToolOption
import it.stradesumisura.app.ui.components.BandChip
import it.stradesumisura.app.ui.components.ConfirmDialog
import it.stradesumisura.app.ui.components.DateField
import it.stradesumisura.app.ui.components.DecisionTreeDialog
import it.stradesumisura.app.ui.components.EmptyState
import it.stradesumisura.app.ui.components.GroupHeaderRow
import it.stradesumisura.app.ui.components.GroupSubtotals
import it.stradesumisura.app.ui.components.ScoreHighlights
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
                item { ProgressCard(tool, score, state.answers) }
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
                                if (field.isChoice) {
                                    ChoiceField(
                                        field = field,
                                        value = state.answers.fields[field.id],
                                        onSelect = { viewModel.setField(field.id, it) },
                                    )
                                    return@forEach
                                }
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
                tool.answerableItems.forEachIndexed { index, toolItem ->
                    // Intestazione dell'area (es. motoria/cognitiva) prima del suo primo item.
                    val group = tool.groupOf(toolItem)
                    if (group != null && tool.itemsOf(group).firstOrNull()?.id == toolItem.id) {
                        item(key = "group_${group.id}") { GroupHeaderRow(tool = tool, group = group, answers = state.answers) }
                    }
                    toolItem.section?.let { section ->
                        item(key = "section_${toolItem.id}") { SectionLabel(section) }
                    }
                    item(key = toolItem.id) {
                        val alternative = tool.alternativeFor(toolItem.id)
                        val useAlternative = alternative != null && state.usesAlternative(toolItem.id)
                        val shown = if (useAlternative && alternative != null) alternative else toolItem
                        val variantMissing = tool.variantFieldId != null && tool.variantOf(state.answers) == null &&
                            shown.options.any { it.variantScores.isNotEmpty() }
                        ItemCard(
                            number = tool.numberOf(toolItem) ?: "${index + 1}",
                            item = toolItem,
                            alternative = alternative,
                            shown = shown,
                            options = tool.optionsFor(shown, state.answers),
                            selection = tool.selectionFor(toolItem, state.answers),
                            useAlternative = useAlternative,
                            note = state.answers.itemNotes[shown.id],
                            subtaskResults = state.answers.subtaskResults[shown.id].orEmpty(),
                            decisionSummary = tool.decisionSummary(shown, state.answers),
                            formatScore = tool::formatScore,
                            variantWarning = if (variantMissing) "Indicare il tipo di carrozzina nei dati della valutazione: alcuni punteggi dipendono da esso." else null,
                            onSelect = viewModel::select,
                            onToggle = viewModel::toggleOption,
                            onToggleAlternative = { viewModel.setUseAlternative(toolItem.id, it) },
                            onNoteChange = { viewModel.setItemNote(shown.id, it) },
                            onSetCount = { viewModel.setCount(shown.id, it) },
                            onSetSubtask = { subtaskIndex, correct -> viewModel.setSubtask(shown.id, subtaskIndex, correct) },
                            onClear = { viewModel.clearItem(shown.id) },
                            onApplyDecision = { path -> viewModel.applyDecision(shown.id, path) },
                        )
                    }
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
private fun ProgressCard(tool: ToolDefinition, score: ScoreResult?, answers: AssessmentAnswers) {
    val totalText = if (score != null && (score.isComplete || tool.sumsItems)) tool.formatScore(score.total) else "—"
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        tool.totalLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            totalText,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            if (tool.percentHeadline && score != null && score.answered > 0) {
                                " / ${tool.formatScore(score.max)} · ${score.percent}%"
                            } else {
                                " / ${tool.formatScore(score?.max ?: 0)}"
                            },
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
            when {
                band != null -> BandChip(band)
                !tool.hasBands && score != null && tool.showsPercent -> Text(
                    buildString {
                        append(if (score.answered == 0) "Nessuna ${tool.itemNounSingular} valutata." else "${score.percent}% ${tool.percentNoun} finora.")
                        if (score.skipped > 0) append(" ${score.skipped} ${if (score.skipped == 1) "prova facoltativa non valutata" else "prove facoltative non valutate"}.")
                        if (score.notApplicable > 0) append(" ${score.notApplicable} ${if (score.notApplicable == 1) "voce non applicabile esclusa" else "voci non applicabili escluse"} dal massimo.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                !tool.hasBands && score != null -> Text(
                    if (score.answered == 0) {
                        "Nessuna ${tool.itemNounSingular} valutata: il punteggio è la somma dei livelli (intervallo ${tool.scoreRangeLabel})."
                    } else {
                        "Somma dei livelli delle ${tool.itemNounPlural} compilate (intervallo ${tool.scoreRangeLabel})."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                else -> Text(
                    "Completa tutte le voci per ottenere l'interpretazione del punteggio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (tool.hasGroups) {
                Spacer(Modifier.height(12.dp))
                GroupSubtotals(tool = tool, answers = answers)
            }
            if (tool.highlightedItems.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                ScoreHighlights(
                    tool = tool,
                    answers = answers,
                    score = score,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                val computedHint = tool.headlineItem?.takeIf { it.isComputed }?.hint
                if (computedHint != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        computedHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemCard(
    number: String,
    item: ToolItem,
    alternative: ToolItem?,
    shown: ToolItem,
    options: List<ToolOption>,
    selection: Selection?,
    useAlternative: Boolean,
    note: String?,
    subtaskResults: Map<Int, Boolean>,
    decisionSummary: String?,
    formatScore: (Int) -> String,
    variantWarning: String?,
    onSelect: (itemId: String, optionIndex: Int) -> Unit,
    onToggle: (itemId: String, optionIndex: Int) -> Unit,
    onToggleAlternative: (Boolean) -> Unit,
    onNoteChange: (String) -> Unit,
    onSetCount: (Int?) -> Unit,
    onSetSubtask: (index: Int, correct: Boolean?) -> Unit,
    onClear: () -> Unit,
    onApplyDecision: (path: List<Int>) -> Unit,
) {
    var showDefinition by rememberSaveable(shown.id) { mutableStateOf(false) }
    var showDecisionTree by rememberSaveable(shown.id) { mutableStateOf(false) }
    if (showDecisionTree) {
        DecisionTreeDialog(
            item = shown,
            onConfirm = { path ->
                onApplyDecision(path)
                showDecisionTree = false
            },
            onDismiss = { showDecisionTree = false },
        )
    }
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
                        number,
                        style = if (number.length > 2) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                        color = if (selection != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(shown.title, style = MaterialTheme.typography.titleMedium)
                    if (shown.optional) {
                        Text(
                            "Facoltativa: si può lasciare non valutata",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    shown.hint?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (selection != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (selection.option.notApplicable) "n.a." else "${formatScore(selection.option.score)}/${formatScore(shown.maxScore)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            shown.imageRes?.let { image ->
                Spacer(Modifier.height(10.dp))
                ItemImage(image)
            }
            if (variantWarning != null) {
                Spacer(Modifier.height(8.dp))
                Text(variantWarning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
            if (shown.hasDecisionTree) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showDecisionTree = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (selection == null) "Guida con l'albero decisionale" else "Ripeti l'albero decisionale") }
                if (decisionSummary != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        decisionSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                } else if (selection != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Livello scelto direttamente dalle descrizioni.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            when {
                shown.hasSubtasks -> SubtaskChecklist(
                    item = shown,
                    results = subtaskResults,
                    onSetSubtask = onSetSubtask,
                    onClear = onClear,
                )
                shown.isCount -> CountInput(
                    item = shown,
                    value = selection?.option?.score,
                    onSetCount = onSetCount,
                )
                shown.multiSelect && shown.hasOptionGroups -> GroupedMultiSelect(
                    item = shown,
                    options = options,
                    mask = if (selection != null && selection.item.id == shown.id) selection.optionIndex else 0,
                    formatScore = formatScore,
                    onToggle = { onToggle(shown.id, it) },
                )
                shown.multiSelect -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val mask = if (selection != null && selection.item.id == shown.id) selection.optionIndex else 0
                    options.forEachIndexed { index, option ->
                        OptionRow(
                            option = option,
                            selected = mask and (1 shl index) != 0,
                            scoreText = optionScoreText(option, formatScore),
                            multi = true,
                            onClick = { onToggle(shown.id, index) },
                        )
                    }
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEachIndexed { index, option ->
                        val selected = selection != null && selection.item.id == shown.id && selection.optionIndex == index
                        OptionRow(
                            option = option,
                            selected = selected,
                            scoreText = optionScoreText(option, formatScore),
                            multi = false,
                            onClick = { onSelect(shown.id, index) },
                        )
                    }
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

/**
 * Elenco delle sotto-prove di un item a conteggio: per ciascuna l'operatore segna riuscita (✓) o
 * non riuscita (✗). Il conteggio e lo stato di compilazione si aggiornano da soli.
 */
@Composable
private fun SubtaskChecklist(
    item: ToolItem,
    results: Map<Int, Boolean>,
    onSetSubtask: (index: Int, correct: Boolean?) -> Unit,
    onClear: () -> Unit,
) {
    val correct = results.values.count { it }
    val marked = results.size
    val pending = item.subtasks.size - marked
    Column {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    buildString {
                        append("Riuscite: $correct su ${item.subtasks.size}")
                        if (pending > 0) append(" · $pending da valutare") else append(" · prova completata")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (marked > 0) {
                    TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text("Azzera")
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        item.subtasks.forEachIndexed { index, text ->
            val state = results[index]
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${index + 1}.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(26.dp),
                )
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                MarkButton(
                    icon = Icons.Default.Check,
                    description = "Riuscita",
                    selected = state == true,
                    activeColor = MaterialTheme.colorScheme.primary,
                    onClick = { onSetSubtask(index, if (state == true) null else true) },
                )
                Spacer(Modifier.width(6.dp))
                MarkButton(
                    icon = Icons.Default.Close,
                    description = "Non riuscita",
                    selected = state == false,
                    activeColor = MaterialTheme.colorScheme.error,
                    onClick = { onSetSubtask(index, if (state == false) null else false) },
                )
            }
        }
    }
}

@Composable
private fun MarkButton(
    icon: ImageVector,
    description: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) activeColor else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) activeColor else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.size(38.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Conteggio delle risposte corrette per gli item senza elenco di sotto-prove: cursore e pulsanti −/+. */
@Composable
private fun CountInput(item: ToolItem, value: Int?, onSetCount: (Int?) -> Unit) {
    val max = item.countMax ?: return
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { onSetCount(((value ?: 0) - 1).coerceAtLeast(0)) },
                enabled = value != null && value > 0,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(44.dp),
            ) { Text("−", style = MaterialTheme.typography.titleMedium) }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (value == null) "—" else "$value",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (value == null) "non valutata · ${item.countUnit} su $max" else "${item.countUnit} su $max",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = { onSetCount(((value ?: -1) + 1).coerceAtMost(max)) },
                enabled = value == null || value < max,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(44.dp),
            ) { Text("+", style = MaterialTheme.typography.titleMedium) }
        }
        Slider(
            value = (value ?: 0).toFloat(),
            onValueChange = { onSetCount(it.toInt()) },
            valueRange = 0f..max.toFloat(),
            steps = (max - 1).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth(),
        )
        if (value != null) {
            TextButton(onClick = { onSetCount(null) }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
                Text("Segna come non valutata")
            }
        }
    }
}

/** Testo del punteggio di un'opzione: il valore formattato oppure «n.a.» per le opzioni non applicabili. */
private fun optionScoreText(option: ToolOption, formatScore: (Int) -> String): String =
    if (option.notApplicable) "n.a." else formatScore(option.score)

@Composable
private fun OptionRow(option: ToolOption, selected: Boolean, scoreText: String, multi: Boolean, onClick: () -> Unit) {
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
            if (multi) Checkbox(checked = selected, onCheckedChange = null) else RadioButton(selected = selected, onClick = null)
            option.imageRes?.let { image ->
                Image(
                    painter = painterResource(image),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(top = 6.dp, end = 10.dp)
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.small),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(top = 10.dp),
            ) {
                Text(option.title, style = MaterialTheme.typography.titleSmall)
                if (option.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            ScoreBadge(scoreText, selected)
        }
    }
}

@Composable
private fun ScoreBadge(text: String, selected: Boolean) {
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
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Campo a scelta (es. il tipo di carrozzina): pulsanti segmentati con l'etichetta sopra. */
@Composable
private fun ChoiceField(field: ToolField, value: String?, onSelect: (String) -> Unit) {
    Column {
        Text(
            field.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (value == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            field.choices.forEachIndexed { index, choice ->
                SegmentedButton(
                    selected = value == choice,
                    onClick = { onSelect(choice) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = field.choices.size),
                ) { Text(choice, maxLines = 1) }
            }
        }
    }
}

/**
 * Scelta multipla raggruppata (es. le posizioni della mano nella TAI): si sceglie prima il gruppo
 * (la superficie) con i chip, poi le opzioni del gruppo, con la loro immagine, in una griglia.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupedMultiSelect(
    item: ToolItem,
    options: List<ToolOption>,
    mask: Int,
    formatScore: (Int) -> String,
    onToggle: (Int) -> Unit,
) {
    val selectedGroup = item.selectedIndices(mask).firstOrNull()?.let { options[it].group }
    var currentGroup by rememberSaveable(item.id) { mutableStateOf(selectedGroup) }
    LaunchedEffect(selectedGroup) { if (selectedGroup != null) currentGroup = selectedGroup }
    Column {
        Text(
            item.optionGroupLabel ?: "Gruppo",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item.optionGroups.forEach { group ->
                FilterChip(
                    selected = currentGroup == group,
                    onClick = { currentGroup = group },
                    label = { Text(group) },
                )
            }
        }
        val group = currentGroup
        if (group == null) {
            Text(
                "Scegliere la superficie per vedere le posizioni della mano.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(Modifier.height(8.dp))
            val entries = options.withIndex().filter { it.value.group == group }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { entry ->
                            ImageOptionCard(
                                option = entry.value,
                                selected = mask and (1 shl entry.index) != 0,
                                scoreText = optionScoreText(entry.value, formatScore),
                                onClick = { onToggle(entry.index) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageOptionCard(option: ToolOption, selected: Boolean, scoreText: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier,
    ) {
        Column {
            option.imageRes?.let { image ->
                Image(
                    painter = painterResource(image),
                    contentDescription = option.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.15f),
                )
            }
            Row(Modifier.padding(start = 2.dp, end = 8.dp, top = 2.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = selected, onCheckedChange = null)
                Text(
                    option.title,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(4.dp))
                ScoreBadge(scoreText, selected)
            }
            if (option.description.isNotBlank()) {
                Text(
                    option.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
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
                val optionalNote = if (score.skipped > 0) " Le prove facoltative non valutate non contano." else ""
                Text(
                    "$verb $missing $noun: la valutazione verrà salvata come parziale.$optionalNote",
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
