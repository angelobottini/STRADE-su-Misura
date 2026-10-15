package it.stradesumisura.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.stradesumisura.app.AppContainer
import it.stradesumisura.app.ui.components.DateField
import it.stradesumisura.app.ui.components.StradeTopBar
import it.stradesumisura.app.ui.viewmodel.AppViewModelFactory
import it.stradesumisura.app.ui.viewmodel.PersonEditViewModel

@Composable
fun PersonEditScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: PersonEditViewModel = viewModel(factory = AppViewModelFactory),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StradeTopBar(
                title = if (viewModel.isEdit) "Modifica persona" else "Nuova persona",
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = form.firstName,
                onValueChange = { value -> viewModel.update { it.copy(firstName = value) } },
                label = { Text("Nome *") },
                singleLine = true,
                enabled = loaded,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.lastName,
                onValueChange = { value -> viewModel.update { it.copy(lastName = value) } },
                label = { Text("Cognome *") },
                singleLine = true,
                enabled = loaded,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            DateField(
                label = "Data di nascita",
                date = form.birthDate,
                onDateChange = { date -> viewModel.update { it.copy(birthDate = date) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Column {
                Text(
                    "Sesso",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                val options = listOf("M" to "Maschio", "F" to "Femmina", "X" to "Altro")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, option ->
                        val (value, label) = option
                        SegmentedButton(
                            selected = form.sex == value,
                            onClick = { viewModel.update { it.copy(sex = if (it.sex == value) null else value) } },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        ) { Text(label) }
                    }
                }
            }
            OutlinedTextField(
                value = form.code,
                onValueChange = { value -> viewModel.update { it.copy(code = value) } },
                label = { Text("Codice identificativo") },
                supportingText = { Text("Facoltativo: numero di cartella o altro riferimento interno") },
                singleLine = true,
                enabled = loaded,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.notes,
                onValueChange = { value -> viewModel.update { it.copy(notes = value) } },
                label = { Text("Note") },
                minLines = 3,
                enabled = loaded,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = loaded && form.isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(if (viewModel.isEdit) "Salva modifiche" else "Salva persona")
            }
            Text(
                "I dati sono salvati esclusivamente su questo ${AppContainer.instance.platform.deviceNoun}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
