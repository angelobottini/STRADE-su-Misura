package it.stradesumisura.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.stradesumisura.app.domain.AssessmentAnswers
import it.stradesumisura.app.domain.ScoreResult
import it.stradesumisura.app.domain.ToolDefinition

/**
 * Evidenzia i valori inseriti dal valutatore (es. completezza e qualità della PQRS) e il valore
 * calcolato da essi, con la formula di calcolo. Non mostra nulla per gli strumenti che non
 * dichiarano item evidenziati.
 */
@Composable
fun ScoreHighlights(
    tool: ToolDefinition,
    answers: AssessmentAnswers,
    score: ScoreResult?,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val items = tool.highlightedItems
    if (items.isEmpty()) return
    val headline = tool.headlineItem
    val computed = headline != null && headline.isComputed
    val explanation = if (headline != null && computed) tool.computedExplanation(headline, answers) else null

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val value = tool.selectionFor(item, answers)?.option?.score?.let { tool.formatScore(it) } ?: "—"
                HighlightChip(
                    label = item.compactTitle,
                    value = value,
                    max = tool.formatScore(item.maxScore),
                    emphasised = false,
                    modifier = Modifier.weight(1f),
                )
            }
            if (computed) {
                Text("=", style = MaterialTheme.typography.titleMedium, color = labelColor)
                HighlightChip(
                    label = tool.shortName,
                    value = if (score != null && score.isComplete) tool.formatScore(score.total) else "—",
                    max = tool.formatScore(tool.maxScore),
                    emphasised = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (computed) {
            Spacer(Modifier.height(6.dp))
            Text(
                explanation?.let { "Calcolo: ${it.replaceFirstChar(Char::uppercase)}" }
                    ?: "Il punteggio viene calcolato quando tutti i valori sono inseriti.",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = labelColor,
            )
        }
    }
}

@Composable
private fun HighlightChip(label: String, value: String, max: String, emphasised: Boolean, modifier: Modifier = Modifier) {
    val container = if (emphasised) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface
    val content = if (emphasised) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
    val muted = if (emphasised) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = container,
        border = if (emphasised) null else BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = content)
                Text(
                    " / $max",
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
    }
}
