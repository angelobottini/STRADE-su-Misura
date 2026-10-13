package it.stradesumisura.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.stradesumisura.app.domain.AssessmentAnswers
import it.stradesumisura.app.domain.ToolDefinition
import it.stradesumisura.app.domain.ToolGroup

/** Chip con il subtotale di ciascuna area dello strumento (es. motoria e cognitiva nella FIM). */
@Composable
fun GroupSubtotals(
    tool: ToolDefinition,
    answers: AssessmentAnswers,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (!tool.hasGroups) return
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tool.groups.forEach { group ->
            val score = tool.groupScore(group, answers)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.weight(1f),
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(
                        group.compactTitle.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            if (score.answered == 0) "—" else "${score.total}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            " / ${score.max}",
                            style = MaterialTheme.typography.labelMedium,
                            color = labelColor,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                    if (!score.isComplete) {
                        Text(
                            "${score.answered} di ${score.required} ${tool.itemNounPlural}",
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor,
                        )
                    }
                }
            }
        }
    }
}

/** Intestazione di un'area nell'elenco delle voci, con il subtotale a destra. */
@Composable
fun GroupHeaderRow(tool: ToolDefinition, group: ToolGroup, answers: AssessmentAnswers, modifier: Modifier = Modifier) {
    val score = tool.groupScore(group, answers)
    Column(modifier.padding(top = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(group.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                if (score.answered == 0) "— / ${score.max}" else "${score.total} / ${score.max}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        group.subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
