package it.stradesumisura.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.stradesumisura.app.domain.ToolRegistry
import it.stradesumisura.app.ui.components.StradeLazyColumn
import it.stradesumisura.app.ui.components.StradeTopBar
import it.stradesumisura.app.ui.components.toolCardItems
import it.stradesumisura.app.ui.components.toolColumnsFor

@Composable
fun ToolsScreen(
    onOpenTool: (String) -> Unit,
    onStartAssessment: (String) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { StradeTopBar(title = "Strumenti", subtitle = "Scale e strumenti di valutazione disponibili") },
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val columns = toolColumnsFor(maxWidth)
            StradeLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                toolCardItems(ToolRegistry.tools, columns, onOpenTool, onStartAssessment)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                "Altri strumenti in arrivo",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Il catalogo è progettato per crescere: le prossime scale verranno aggiunte con la stessa " +
                                    "compilazione guidata, il calcolo automatico del punteggio e il report PDF.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}
