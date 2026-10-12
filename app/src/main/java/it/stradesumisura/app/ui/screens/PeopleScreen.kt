package it.stradesumisura.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.stradesumisura.app.ui.components.EmptyState
import it.stradesumisura.app.ui.components.PersonRow
import it.stradesumisura.app.ui.components.StradeTopBar
import it.stradesumisura.app.ui.components.personMeta
import it.stradesumisura.app.ui.viewmodel.AppViewModelFactory
import it.stradesumisura.app.ui.viewmodel.PeopleViewModel

@Composable
fun PeopleScreen(
    onOpenPerson: (String) -> Unit,
    onAddPerson: () -> Unit,
    viewModel: PeopleViewModel = viewModel(factory = AppViewModelFactory),
) {
    val people by viewModel.people.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val isEmpty by viewModel.isEmpty.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { StradeTopBar(title = "Persone", subtitle = "Archivio locale delle persone valutate") },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPerson,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuova persona") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
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
            if (isEmpty) {
                EmptyState(
                    title = "Archivio vuoto",
                    message = "Le persone che valuti vengono salvate solo su questo dispositivo. Aggiungi la prima persona per iniziare.",
                    action = { Button(onClick = onAddPerson) { Text("Aggiungi persona") } },
                )
            } else if (people.isEmpty()) {
                EmptyState(title = "Nessun risultato", message = "Nessuna persona corrisponde a «$query».")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(people, key = { it.person.id }) { item ->
                        PersonRow(
                            person = item.person,
                            meta = personMeta(item.person),
                            trailing = when (item.assessmentCount) {
                                0 -> null
                                1 -> "1 valutazione"
                                else -> "${item.assessmentCount} valutazioni"
                            },
                            onClick = { onOpenPerson(item.person.id) },
                        )
                    }
                }
            }
        }
    }
}
