package it.stradesumisura.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.stradesumisura.app.domain.ToolRegistry
import it.stradesumisura.app.ui.components.EmptyState
import it.stradesumisura.app.ui.components.PersonRow
import it.stradesumisura.app.ui.components.StradeLazyColumn
import it.stradesumisura.app.ui.components.StradeTopBar
import it.stradesumisura.app.ui.components.personMeta
import it.stradesumisura.app.ui.viewmodel.AppViewModelFactory
import it.stradesumisura.app.ui.viewmodel.PeopleViewModel

@Composable
fun PersonPickerScreen(
    toolId: String,
    onBack: () -> Unit,
    onPick: (String) -> Unit,
    onCreatePerson: () -> Unit,
    viewModel: PeopleViewModel = viewModel(factory = AppViewModelFactory),
) {
    val tool = ToolRegistry.byId(toolId)
    val people by viewModel.people.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val isEmpty by viewModel.isEmpty.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { StradeTopBar(title = "Per chi è la valutazione?", subtitle = tool?.name, onBack = onBack) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                placeholder = { Text("Cerca per nome o codice") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancella ricerca")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            OutlinedButton(
                onClick = onCreatePerson,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nuova persona")
            }
            when {
                isEmpty -> EmptyState(
                    title = "Archivio vuoto",
                    message = "Crea la prima persona per avviare la valutazione.",
                )

                people.isEmpty() -> EmptyState(
                    title = "Nessun risultato",
                    message = "Nessuna persona corrisponde a «$query».",
                )

                else -> StradeLazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(people, key = { it.person.id }) { item ->
                        PersonRow(
                            person = item.person,
                            meta = personMeta(item.person),
                            trailing = null,
                            onClick = { onPick(item.person.id) },
                        )
                    }
                }
            }
        }
    }
}
