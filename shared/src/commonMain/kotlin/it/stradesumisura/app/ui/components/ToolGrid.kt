package it.stradesumisura.app.ui.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
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
        EqualHeightRow(columns = columns, count = row.size, spacing = 14.dp) { index, modifier ->
            val tool = row[index]
            ToolCard(
                tool = tool,
                onOpen = { onOpenTool(tool.id) },
                onStart = { onStartAssessment(tool.id) },
                modifier = modifier,
            )
        }
    }
}

/**
 * Riga di [count] celle larghe uguali su [columns] colonne, alte quanto la cella più alta.
 * Le celle vengono prima misurate alla loro altezza naturale e poi ricomposte a quell'altezza
 * (le misure «intrinseche» non sono affidabili con i contenuti che vanno a capo).
 */
@Composable
private fun EqualHeightRow(
    columns: Int,
    count: Int,
    spacing: Dp,
    cell: @Composable (index: Int, modifier: Modifier) -> Unit,
) {
    SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
        val gap = spacing.roundToPx()
        val cellWidth = ((constraints.maxWidth - gap * (columns - 1)) / columns).coerceAtLeast(0)
        val natural = subcompose("misura") { repeat(count) { cell(it, Modifier) } }
            .map { it.measure(Constraints.fixedWidth(cellWidth)) }
        val height = natural.maxOfOrNull { it.height } ?: 0
        val placeables = subcompose("celle") { repeat(count) { cell(it, Modifier.fillMaxHeight()) } }
            .map { it.measure(Constraints.fixed(cellWidth, height)) }
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable -> placeable.place(index * (cellWidth + gap), 0) }
        }
    }
}
