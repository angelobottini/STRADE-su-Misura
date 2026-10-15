package it.stradesumisura.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.stradesumisura.app.domain.ToolDefinition

/** Colonne delle schede degli strumenti: due quando lo spazio è ampio (finestra su Windows, tablet). */
fun toolColumnsFor(contentWidth: Dp): Int = if (contentWidth >= 960.dp) 2 else 1

/** Schede degli strumenti su una o più colonne, con le schede di una riga alte uguali. */
fun LazyListScope.toolCardItems(
    tools: List<ToolDefinition>,
    columns: Int,
    onOpenTool: (String) -> Unit,
    onStartAssessment: (String) -> Unit,
) {
    if (columns <= 1) {
        items(tools, key = { it.id }) { tool ->
            ToolCard(tool = tool, onOpen = { onOpenTool(tool.id) }, onStart = { onStartAssessment(tool.id) })
        }
        return
    }
    items(tools.chunked(columns), key = { row -> row.joinToString("+") { it.id } }) { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            row.forEach { tool ->
                ToolCard(
                    tool = tool,
                    onOpen = { onOpenTool(tool.id) },
                    onStart = { onStartAssessment(tool.id) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}
