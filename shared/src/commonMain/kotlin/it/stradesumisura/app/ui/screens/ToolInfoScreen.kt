package it.stradesumisura.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.stradesumisura.app.domain.ToolRegistry
import it.stradesumisura.app.ui.components.EmptyState
import it.stradesumisura.app.ui.components.InfoPill
import it.stradesumisura.app.ui.components.ItemImage
import it.stradesumisura.app.ui.components.SectionLabel
import it.stradesumisura.app.ui.components.StradeLazyColumn
import it.stradesumisura.app.ui.components.StradeTopBar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolInfoScreen(
    toolId: String,
    onBack: () -> Unit,
    onStartAssessment: () -> Unit,
) {
    val tool = ToolRegistry.byId(toolId)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { StradeTopBar(title = tool?.shortName ?: "Strumento", subtitle = "Scheda dello strumento", onBack = onBack) },
        bottomBar = {
            if (tool != null) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Button(
                        onClick = onStartAssessment,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .fillMaxWidth()
                            .height(52.dp),
                    ) { Text("Nuova valutazione") }
                }
            }
        },
    ) { padding ->
        if (tool == null) {
            EmptyState(
                title = "Strumento non disponibile",
                message = "Lo strumento richiesto non è presente in questa versione dell'app.",
                modifier = Modifier.padding(padding),
            )
        } else {
            StradeLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Column {
                        Text(tool.name, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${tool.subtitle} · ${tool.citation}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            InfoPill(tool.duration)
                            InfoPill(tool.itemsCountText)
                            InfoPill("${tool.totalLabel} ${tool.scoreRangeLabel}")
                        }
                    }
                }
                item {
                    InfoSection("Descrizione") {
                        Text(tool.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item {
                    InfoSection("Istruzioni per la compilazione") {
                        tool.instructions.forEachIndexed { index, instruction ->
                            Row(Modifier.padding(bottom = 8.dp)) {
                                Text(
                                    "${index + 1}.",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.width(24.dp),
                                )
                                Text(instruction, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                if (tool.fields.isNotEmpty()) {
                    item {
                        InfoSection("Dati raccolti") {
                            Text(
                                "Oltre a data, valutatore e contesto, la scheda registra: " +
                                    tool.fields.joinToString(", ") { it.label.lowercase() } + ".",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                if (tool.highlightedItems.isNotEmpty() && tool.headlineItem?.isComputed == true) {
                    item {
                        InfoSection("Calcolo del punteggio") {
                            Text(
                                "Il valutatore inserisce ${tool.highlightedItems.joinToString(" e ") { it.compactTitle.lowercase() }}; " +
                                    "il ${(tool.headlineItem?.title ?: tool.totalLabel).replaceFirstChar(Char::lowercase)} è " +
                                    (tool.headlineItem?.hint?.replaceFirstChar(Char::lowercase) ?: "calcolato automaticamente."),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                if (tool.hasDecisionTrees) {
                    item {
                        InfoSection("Albero decisionale") {
                            Text(
                                "Per ogni ${tool.itemNounSingular} è disponibile l'albero decisionale del manuale: nel modulo di " +
                                    "compilazione il pulsante «Guida con l'albero decisionale» pone le domande una alla volta " +
                                    "(Sì/No, oppure il numero di episodi) e seleziona il livello raggiunto, che resta " +
                                    "modificabile. La scheda e il report indicano quando il livello è stato scelto con la guida.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                item {
                    InfoSection(tool.itemsSectionTitle) {
                        tool.primaryItems.forEach { item ->
                            val number = tool.numberOf(item)?.let { "$it. " } ?: ""
                            val group = tool.groupOf(item)
                            if (group != null && tool.itemsOf(group).firstOrNull()?.id == item.id) {
                                Column(Modifier.padding(top = if (item.id == tool.primaryItems.first().id) 0.dp else 10.dp, bottom = 4.dp)) {
                                    Text(
                                        "${group.title} (${tool.itemsOf(group).size} ${tool.itemNounPlural})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                    group.subtitle?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            item.section?.let { section ->
                                SectionLabel(section, Modifier.padding(top = 6.dp, bottom = 2.dp))
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    when {
                                        item.isComputed -> "$number${item.title} (calcolata automaticamente)"
                                        item.optional -> "$number${item.title} (facoltativa)"
                                        else -> "$number${item.title}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    tool.rangeLabelOf(item),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            (item.description ?: item.hint?.takeIf { item.isComputed || tool.percentHeadline })?.let { description ->
                                Text(
                                    description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 20.dp, bottom = 6.dp),
                                )
                            }
                            item.imageRes?.let { image ->
                                ItemImage(image, Modifier.padding(start = 20.dp, bottom = 8.dp))
                            }
                            tool.alternativeFor(item.id)?.let { alternative ->
                                Text(
                                    "In alternativa: ${alternative.title} (${tool.rangeLabelOf(alternative)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 20.dp, bottom = 4.dp),
                                )
                            }
                        }
                    }
                }
                item {
                    InfoSection("Interpretazione del punteggio") {
                        val scoringNote = tool.scoringNote
                        if (scoringNote != null) {
                            Text(scoringNote, style = MaterialTheme.typography.bodyMedium)
                        } else if (!tool.hasBands && tool.countsAnswers) {
                            Text(
                                "Lo strumento non prevede fasce interpretative: il punteggio è il numero di risposte " +
                                    "corrette per ogni prova e in totale (fino a ${tool.formatScore(tool.maxScore)}), letto anche in percentuale. " +
                                    "Le prove facoltative non somministrate non entrano nel totale né nel massimo.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else if (!tool.hasBands) {
                            Text(
                                buildString {
                                    append("Lo strumento non prevede fasce interpretative: il punteggio totale è la somma dei livelli ")
                                    append("delle ${tool.answerableItems.size} ${tool.itemNounPlural} (da ${tool.formatScore(tool.minScore)} a ${tool.formatScore(tool.maxScore)}); ")
                                    append("più alto è il punteggio, maggiore è l'indipendenza.")
                                    if (tool.hasGroups) {
                                        append(" Subtotali: ")
                                        append(
                                            tool.groups.joinToString("; ") { group ->
                                                val items = tool.itemsOf(group)
                                                "${group.title.lowercase()} ${tool.formatRange(items.sumOf { it.minScore }, items.sumOf { it.maxScore })}"
                                            },
                                        )
                                        append(".")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        tool.bands.forEach { band ->
                            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                                Box(
                                    Modifier
                                        .padding(top = 5.dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(band.color)),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    band.rangeLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.width(56.dp),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(band.label, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        band.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    InfoSection("Riferimento bibliografico") {
                        Text(
                            tool.reference,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoSection(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
