package it.stradesumisura.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.stradesumisura.app.R
import it.stradesumisura.app.domain.AssessmentAnswers
import it.stradesumisura.app.domain.ScoreBand
import it.stradesumisura.app.domain.ScoreResult
import it.stradesumisura.app.domain.Selection
import it.stradesumisura.app.domain.ToolDefinition
import it.stradesumisura.app.domain.ToolItem
import it.stradesumisura.app.ui.components.BandChip
import it.stradesumisura.app.ui.components.ConfirmDialog
import it.stradesumisura.app.ui.components.EmptyState
import it.stradesumisura.app.ui.components.GroupHeaderRow
import it.stradesumisura.app.ui.components.GroupSubtotals
import it.stradesumisura.app.ui.components.LabeledValue
import it.stradesumisura.app.ui.components.ScoreHighlights
import it.stradesumisura.app.ui.components.SectionHeader
import it.stradesumisura.app.ui.components.StradeTopBar
import it.stradesumisura.app.ui.viewmodel.AppViewModelFactory
import it.stradesumisura.app.ui.viewmodel.AssessmentDetailViewModel
import it.stradesumisura.app.util.Dates
import kotlin.math.max

@Composable
fun AssessmentDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onExportPdf: (String) -> Unit,
    onOpenPerson: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: AssessmentDetailViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDelete by remember { mutableStateOf(false) }
    val data = state.data
    val tool = state.tool
    val score = state.score

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StradeTopBar(
                title = tool?.name ?: "Valutazione",
                subtitle = data?.person?.fullName,
                onBack = onBack,
                actions = {
                    if (data != null) {
                        IconButton(onClick = { onEdit(data.assessment.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifica")
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (data != null) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Row(
                        Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onEdit(data.assessment.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                        ) { Text("Modifica") }
                        Button(
                            onClick = { onExportPdf(data.assessment.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                        ) {
                            Icon(painterResource(R.drawable.ic_document), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Report PDF")
                        }
                    }
                }
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

            data == null || tool == null || score == null -> EmptyState(
                title = "Valutazione non trovata",
                message = "La valutazione potrebbe essere stata eliminata.",
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { ScoreHeroCard(tool = tool, score = score, answers = state.answers) }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        onClick = { onOpenPerson(data.person.id) },
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row {
                                LabeledValue("Persona", data.person.fullName, Modifier.weight(1f))
                                LabeledValue("Data", Dates.formatIsoLong(data.assessment.date), Modifier.weight(1f))
                            }
                            Row {
                                LabeledValue("Valutatore", data.assessment.evaluator?.takeIf { it.isNotBlank() } ?: "—", Modifier.weight(1f))
                                LabeledValue("Contesto", data.assessment.setting?.takeIf { it.isNotBlank() } ?: "—", Modifier.weight(1f))
                            }
                            tool.fields.filter { !it.multiline }.chunked(2).forEach { pair ->
                                Row {
                                    pair.forEach { field ->
                                        LabeledValue(
                                            field.label,
                                            state.answers.fields[field.id]?.takeIf { it.isNotBlank() } ?: "—",
                                            Modifier.weight(1f),
                                        )
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                tool.fields.filter { it.multiline }.forEach { field ->
                    val value = state.answers.fields[field.id]?.trim()
                    if (!value.isNullOrEmpty()) {
                        item(key = "field_${field.id}") { NotesCard(title = field.label, text = value) }
                    }
                }
                item { SectionHeader(tool.itemsSectionTitle) }
                tool.primaryItems.forEach { toolItem ->
                    val group = tool.groupOf(toolItem)
                    if (group != null && tool.itemsOf(group).firstOrNull()?.id == toolItem.id) {
                        item(key = "group_${group.id}") { GroupHeaderRow(tool = tool, group = group, answers = state.answers) }
                    }
                    item(key = toolItem.id) {
                        val selection = tool.selectionFor(toolItem, state.answers)
                        val noteKey = selection?.item?.id ?: toolItem.id
                        DetailRow(
                            number = tool.numberOf(toolItem) ?: "=",
                            item = toolItem,
                            selection = selection,
                            note = state.answers.itemNotes[noteKey] ?: state.answers.itemNotes[toolItem.id],
                            highlighted = toolItem.id in tool.highlightedItemIds,
                            explanation = if (toolItem.isComputed) tool.computedExplanation(toolItem, state.answers) else null,
                            failedSubtasks = tool.failedSubtasks(toolItem, state.answers),
                            decisionSummary = selection?.item?.let { tool.decisionSummary(it, state.answers) },
                        )
                    }
                }
                val notes = data.assessment.notes
                if (!notes.isNullOrBlank()) {
                    item { NotesCard(title = tool.notesLabel, text = notes) }
                }
                item {
                    Text(
                        "Ultima modifica: ${Dates.formatTimestamp(data.assessment.updatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Eliminare la valutazione?",
            text = "L'operazione non può essere annullata.",
            confirmLabel = "Elimina",
            onConfirm = {
                showDelete = false
                viewModel.delete(onDeleted)
            },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun ScoreHeroCard(tool: ToolDefinition, score: ScoreResult, answers: AssessmentAnswers) {
    val band = score.band
    val totalText = if (score.isComplete || tool.sumsItems) "${score.total}" else "—"
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                tool.totalLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(totalText, style = MaterialTheme.typography.displayMedium)
                Text(
                    " / ${score.max}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            val skippedText = if (score.skipped > 0) {
                " ${score.skipped} ${if (score.skipped == 1) "prova facoltativa non somministrata" else "prove facoltative non somministrate"}."
            } else {
                ""
            }
            when {
                band != null -> {
                    BandChip(band)
                    Spacer(Modifier.height(10.dp))
                    Text(band.description, style = MaterialTheme.typography.bodyMedium)
                }
                !tool.hasBands && score.isComplete && tool.countsAnswers -> Text(
                    "${score.percent}% di risposte corrette (${score.total} su ${score.max}).$skippedText",
                    style = MaterialTheme.typography.bodyMedium,
                )
                !tool.hasBands && score.isComplete -> Text(
                    "Somma dei livelli delle ${score.required} ${tool.itemNounPlural} (intervallo ${tool.scoreRangeLabel}).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> Text(
                    "Valutazione parziale: ${score.answered} ${tool.itemNounPlural} su ${score.required}. " +
                        when {
                            tool.hasBands -> "Completa la valutazione per ottenere l'interpretazione."
                            tool.countsAnswers -> "Finora ${score.percent}% di risposte corrette."
                            else -> "Il punteggio totale ha senso solo con tutte le ${tool.itemNounPlural} compilate."
                        } +
                        skippedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (tool.hasGroups) {
                Spacer(Modifier.height(16.dp))
                GroupSubtotals(tool = tool, answers = answers)
            }
            if (tool.highlightedItems.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                ScoreHighlights(tool = tool, answers = answers, score = score)
            }
            Spacer(Modifier.height(18.dp))
            if (tool.hasBands) {
                ScoreBar(bands = tool.bands, minScore = tool.minScore, maxScore = tool.maxScore, score = score.total.takeIf { score.isComplete })
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth()) {
                    val weights = bandWeights(tool.bands, tool.minScore, tool.maxScore)
                    tool.bands.forEachIndexed { index, b ->
                        Text(
                            b.rangeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.weight(weights[index]),
                        )
                    }
                }
            } else {
                LinearProgressIndicator(
                    progress = { score.fraction },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${score.min}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (tool.countsAnswers) "${score.percent}%" else "${score.total}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("${score.max}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun bandWeights(bands: List<ScoreBand>, minScore: Int, maxScore: Int): List<Float> =
    bands.map { max((it.max - it.min + 1).toFloat() / (maxScore - minScore + 1).toFloat(), 0.085f) }

/** Barra interpretativa orizzontale con l'indicatore del punteggio corrente. */
@Composable
fun ScoreBar(bands: List<ScoreBand>, minScore: Int, maxScore: Int, score: Int?, modifier: Modifier = Modifier) {
    val markerColor = MaterialTheme.colorScheme.onSurface
    val weights = bandWeights(bands, minScore, maxScore)
    val totalWeight = weights.sum()
    Canvas(
        modifier
            .fillMaxWidth()
            .height(26.dp),
    ) {
        val barHeight = 10.dp.toPx()
        val barTop = size.height - barHeight
        val radius = barHeight / 2f
        val clip = Path().apply {
            addRoundRect(RoundRect(Rect(0f, barTop, size.width, barTop + barHeight), CornerRadius(radius)))
        }
        var cursor = 0f
        clipPath(clip) {
            bands.forEachIndexed { index, band ->
                val width = size.width * weights[index] / totalWeight
                drawRect(Color(band.color), topLeft = Offset(cursor, barTop), size = Size(width, barHeight))
                cursor += width
            }
        }
        if (score != null) {
            var start = 0f
            bands.forEachIndexed { index, band ->
                val width = size.width * weights[index] / totalWeight
                if (band.contains(score)) {
                    val fraction = ((score - band.min) + 0.5f) / (band.max - band.min + 1f)
                    val x = start + width * fraction
                    val marker = Path().apply {
                        moveTo(x, barTop - 2.dp.toPx())
                        lineTo(x - 6.dp.toPx(), barTop - 11.dp.toPx())
                        lineTo(x + 6.dp.toPx(), barTop - 11.dp.toPx())
                        close()
                    }
                    drawPath(marker, markerColor)
                }
                start += width
            }
        }
    }
}

@Composable
private fun DetailRow(
    number: String,
    item: ToolItem,
    selection: Selection?,
    note: String?,
    highlighted: Boolean = false,
    explanation: String? = null,
    failedSubtasks: List<String> = emptyList(),
    decisionSummary: String? = null,
) {
    val shown = selection?.item ?: item
    val borderColor = when {
        item.isComputed -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
        highlighted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (item.isComputed) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (item.isComputed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    number,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.isComputed) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(shown.title, style = MaterialTheme.typography.titleSmall)
                if (selection?.isAlternative == true) {
                    Text(
                        "in alternativa a «${item.title}»",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                if (item.isComputed) {
                    Text(
                        item.hint ?: "Calcolato automaticamente",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    selection?.option?.title ?: when {
                        item.isComputed -> "In attesa dei valori"
                        item.optional -> "Non somministrata (facoltativa)"
                        else -> "Non valutata"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selection == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                if (failedSubtasks.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Non riuscite: ${failedSubtasks.joinToString("; ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (explanation != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Calcolo: ${explanation.replaceFirstChar(Char::uppercase)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                if (decisionSummary != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        decisionSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                val trimmedNote = note?.trim()
                if (!trimmedNote.isNullOrEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        trimmedNote,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                val scoreText = selection?.option?.score?.toString() ?: "—"
                if ((highlighted || item.isComputed) && selection != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (item.isComputed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            scoreText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (item.isComputed) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        )
                    }
                } else {
                    Text(scoreText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    "su ${shown.maxScore}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun NotesCard(title: String, text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
