package it.stradesumisura.app.ui.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.stradesumisura.app.AppContainer
import it.stradesumisura.app.data.EvaluatorProfile
import it.stradesumisura.app.data.db.AssessmentEntity
import it.stradesumisura.app.data.db.AssessmentWithPerson
import it.stradesumisura.app.data.db.PersonEntity
import it.stradesumisura.app.data.db.PersonWithCount
import it.stradesumisura.app.domain.AnswersCodec
import it.stradesumisura.app.domain.AssessmentAnswers
import it.stradesumisura.app.domain.ScoreResult
import it.stradesumisura.app.domain.ToolDefinition
import it.stradesumisura.app.domain.ToolRegistry
import it.stradesumisura.app.domain.resolve
import it.stradesumisura.app.pdf.AssessmentPdfExporter
import it.stradesumisura.app.ui.platform.renderPdfPages
import it.stradesumisura.app.util.Dates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.UUID

/** Factory condivisa da tutte le schermate: `viewModel(factory = AppViewModelFactory)`. */
val AppViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { HomeViewModel(container()) }
    initializer { PeopleViewModel(container()) }
    initializer { PersonEditViewModel(container(), createSavedStateHandle()) }
    initializer { PersonDetailViewModel(container(), createSavedStateHandle()) }
    initializer { AssessmentFormViewModel(container(), createSavedStateHandle()) }
    initializer { AssessmentDetailViewModel(container(), createSavedStateHandle()) }
    initializer { PdfPreviewViewModel(container(), createSavedStateHandle()) }
    initializer { SettingsViewModel(container()) }
}

private fun container(): AppContainer = AppContainer.instance

private fun <T> Flow<T>.stateWhileSubscribed(scope: CoroutineScope, initial: T): StateFlow<T> =
    stateIn(scope, SharingStarted.WhileSubscribed(5_000), initial)

// ---------------------------------------------------------------------------
// Panoramica
// ---------------------------------------------------------------------------

data class HomeUiState(
    val peopleCount: Int = 0,
    val assessmentCount: Int = 0,
    val recent: List<AssessmentWithPerson> = emptyList(),
    val tools: List<ToolDefinition> = ToolRegistry.tools,
)

class HomeViewModel(container: AppContainer) : ViewModel() {
    private val repository = container.repository

    val state: StateFlow<HomeUiState> = combine(
        repository.peopleCount,
        repository.assessmentCount,
        repository.observeRecentAssessments(8),
    ) { people, assessments, recent ->
        HomeUiState(peopleCount = people, assessmentCount = assessments, recent = recent)
    }.stateWhileSubscribed(viewModelScope, HomeUiState())
}

// ---------------------------------------------------------------------------
// Elenco persone / selezione persona
// ---------------------------------------------------------------------------

class PeopleViewModel(container: AppContainer) : ViewModel() {
    private val repository = container.repository
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val people: StateFlow<List<PersonWithCount>> = combine(repository.people, _query) { list, query ->
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) list else list.filter { it.matches(needle) }
    }.stateWhileSubscribed(viewModelScope, emptyList())

    val isEmpty: StateFlow<Boolean> =
        repository.peopleCount.map { it == 0 }.stateWhileSubscribed(viewModelScope, false)

    fun setQuery(value: String) {
        _query.value = value
    }

    private fun PersonWithCount.matches(needle: String): Boolean =
        person.fullName.lowercase().contains(needle) ||
            "${person.lastName} ${person.firstName}".lowercase().contains(needle) ||
            (person.code?.lowercase()?.contains(needle) == true)
}

// ---------------------------------------------------------------------------
// Scheda persona (nuova / modifica)
// ---------------------------------------------------------------------------

data class PersonForm(
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: LocalDate? = null,
    val sex: String? = null,
    val code: String = "",
    val notes: String = "",
) {
    val isValid: Boolean get() = firstName.isNotBlank() && lastName.isNotBlank()
}

class PersonEditViewModel(container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val repository = container.repository
    val personId: String? = handle["personId"]
    val isEdit: Boolean get() = personId != null

    private var existing: PersonEntity? = null
    private val _form = MutableStateFlow(PersonForm())
    val form: StateFlow<PersonForm> = _form.asStateFlow()
    private val _loaded = MutableStateFlow(personId == null)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        personId?.let { id ->
            viewModelScope.launch {
                repository.getPerson(id)?.let { person ->
                    existing = person
                    _form.value = PersonForm(
                        firstName = person.firstName,
                        lastName = person.lastName,
                        birthDate = Dates.parse(person.birthDate),
                        sex = person.sex,
                        code = person.code.orEmpty(),
                        notes = person.notes.orEmpty(),
                    )
                }
                _loaded.value = true
            }
        }
    }

    fun update(transform: (PersonForm) -> PersonForm) {
        _form.update(transform)
    }

    fun save(onSaved: (String) -> Unit) {
        val form = _form.value
        if (!form.isValid) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = PersonEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                firstName = form.firstName.trim(),
                lastName = form.lastName.trim(),
                birthDate = form.birthDate?.let { Dates.toIso(it) },
                sex = form.sex,
                code = form.code.trim().ifBlank { null },
                notes = form.notes.trim().ifBlank { null },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
            repository.savePerson(entity)
            existing = entity
            onSaved(entity.id)
        }
    }
}

// ---------------------------------------------------------------------------
// Dettaglio persona
// ---------------------------------------------------------------------------

data class PersonDetailUiState(
    val person: PersonEntity? = null,
    val assessments: List<AssessmentEntity> = emptyList(),
    val loading: Boolean = true,
)

class PersonDetailViewModel(container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val repository = container.repository
    private val personId: String = checkNotNull(handle["personId"])

    val state: StateFlow<PersonDetailUiState> = combine(
        repository.observePerson(personId),
        repository.observeAssessmentsOf(personId),
    ) { person, assessments ->
        PersonDetailUiState(person = person, assessments = assessments, loading = false)
    }.stateWhileSubscribed(viewModelScope, PersonDetailUiState())

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deletePerson(personId)
            onDeleted()
        }
    }

    fun deleteAssessment(id: String) {
        viewModelScope.launch { repository.deleteAssessment(id) }
    }
}

// ---------------------------------------------------------------------------
// Compilazione valutazione (nuova / modifica)
// ---------------------------------------------------------------------------

data class AssessmentFormState(
    val tool: ToolDefinition? = null,
    val person: PersonEntity? = null,
    val date: LocalDate = Dates.today(),
    val evaluator: String = "",
    val setting: String = "",
    val answers: AssessmentAnswers = AssessmentAnswers(),
    /** id item principale -> true quando si sta usando l'item alternativo. */
    val alternativeModes: Map<String, Boolean> = emptyMap(),
    val notes: String = "",
    val loaded: Boolean = false,
    val notFound: Boolean = false,
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val isEdit: Boolean = false,
) {
    val score: ScoreResult? get() = tool?.score(answers)
    fun usesAlternative(primaryId: String): Boolean = alternativeModes[primaryId] ?: false
}

class AssessmentFormViewModel(container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val repository = container.repository
    private val settings = container.settings
    private val assessmentId: String? = handle["assessmentId"]
    private val toolIdArg: String? = handle["toolId"]
    private val personIdArg: String? = handle["personId"]

    private var existing: AssessmentEntity? = null
    private val _state = MutableStateFlow(AssessmentFormState())
    val state: StateFlow<AssessmentFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        if (assessmentId != null) {
            val data = repository.getAssessment(assessmentId)
            if (data == null) {
                _state.value = AssessmentFormState(loaded = true, notFound = true)
                return
            }
            val tool = ToolRegistry.byId(data.assessment.toolId)
            val answers = AnswersCodec.decode(data.assessment.answersJson)
            val modes = tool?.primaryItems.orEmpty()
                .mapNotNull { primary ->
                    tool?.alternativeFor(primary.id)?.let { alternative -> primary.id to answers.items.containsKey(alternative.id) }
                }
                .toMap()
            existing = data.assessment
            _state.value = AssessmentFormState(
                tool = tool,
                person = data.person,
                date = Dates.parse(data.assessment.date) ?: Dates.today(),
                evaluator = data.assessment.evaluator.orEmpty(),
                setting = data.assessment.setting.orEmpty(),
                answers = answers,
                alternativeModes = modes,
                notes = data.assessment.notes.orEmpty(),
                loaded = true,
                notFound = tool == null,
                isEdit = true,
            )
        } else {
            val tool = toolIdArg?.let { ToolRegistry.byId(it) }
            val person = personIdArg?.let { repository.getPerson(it) }
            _state.value = AssessmentFormState(
                tool = tool,
                person = person,
                evaluator = settings.profile.value.name,
                loaded = true,
                notFound = tool == null || person == null,
            )
        }
    }

    fun setDate(date: LocalDate) = _state.update { it.copy(date = date, dirty = true) }
    fun setEvaluator(value: String) = _state.update { it.copy(evaluator = value, dirty = true) }
    fun setSetting(value: String) = _state.update { it.copy(setting = value, dirty = true) }
    fun setNotes(value: String) = _state.update { it.copy(notes = value, dirty = true) }

    fun setField(fieldId: String, value: String) = _state.update { current ->
        current.copy(answers = current.answers.copy(fields = current.answers.fields + (fieldId to value)), dirty = true)
    }

    fun setItemNote(itemId: String, value: String) = _state.update { current ->
        current.copy(answers = current.answers.copy(itemNotes = current.answers.itemNotes + (itemId to value)), dirty = true)
    }

    /** Item a conteggio senza sotto-prove: imposta il numero di risposte corrette (null = non valutato). */
    fun setCount(itemId: String, count: Int?) {
        val item = _state.value.tool?.item(itemId) ?: return
        val max = item.countMax ?: return
        _state.update { current ->
            val items = current.answers.items.toMutableMap()
            if (count == null) items.remove(itemId) else items[itemId] = count.coerceIn(0, max)
            current.copy(answers = current.answers.copy(items = items), dirty = true)
        }
    }

    /**
     * Segna una sotto-prova come riuscita (true), non riuscita (false) o da valutare (null).
     * L'item risulta compilato, con punteggio pari alle sotto-prove riuscite, solo quando sono tutte segnate.
     */
    fun setSubtask(itemId: String, index: Int, correct: Boolean?) {
        val item = _state.value.tool?.item(itemId) ?: return
        if (!item.hasSubtasks) return
        _state.update { current ->
            val results = current.answers.subtaskResults[itemId].orEmpty().toMutableMap()
            if (correct == null) results.remove(index) else results[index] = correct
            val items = current.answers.items.toMutableMap()
            if (results.size == item.subtasks.size) {
                items[itemId] = results.values.count { it }
            } else {
                items.remove(itemId)
            }
            current.copy(
                answers = current.answers.copy(items = items, subtaskResults = current.answers.subtaskResults + (itemId to results)),
                dirty = true,
            )
        }
    }

    /**
     * Seleziona o deseleziona un'opzione di un item a scelta multipla (maschera di bit). Le opzioni
     * «non applicabile» e quelle di un gruppo diverso (es. un'altra superficie) escludono le altre.
     */
    fun toggleOption(itemId: String, optionIndex: Int) {
        val item = _state.value.tool?.item(itemId) ?: return
        if (!item.multiSelect) return
        val option = item.options.getOrNull(optionIndex) ?: return
        _state.update { current ->
            val items = current.answers.items.toMutableMap()
            var mask = item.toggled(items[itemId] ?: 0, optionIndex)
            if (mask and (1 shl optionIndex) != 0) {
                item.options.forEachIndexed { index, other ->
                    val exclusive = option.notApplicable || other.notApplicable || other.group != option.group
                    if (index != optionIndex && exclusive) mask = mask and (1 shl index).inv()
                }
            }
            if (mask == 0) items.remove(itemId) else items[itemId] = mask
            current.copy(answers = current.answers.copy(items = items), dirty = true)
        }
    }

    /** Azzera un item a conteggio (sotto-prove comprese): torna «non valutato». */
    fun clearItem(itemId: String) = _state.update { current ->
        current.copy(
            answers = current.answers.copy(
                items = current.answers.items - itemId,
                subtaskResults = current.answers.subtaskResults - itemId,
                decisionPaths = current.answers.decisionPaths - itemId,
            ),
            dirty = true,
        )
    }

    fun select(itemId: String, optionIndex: Int) = selectOption(itemId, optionIndex, decisionPath = null)

    /**
     * Applica il livello raggiunto con l'albero decisionale di un item: seleziona l'opzione con quel
     * punteggio e conserva le risposte date, così la scheda e il report possono indicarle.
     */
    fun applyDecision(itemId: String, path: List<Int>) {
        val item = _state.value.tool?.item(itemId) ?: return
        val outcome = item.decisionTree?.resolve(path) ?: return
        val optionIndex = item.optionIndexFor(outcome.score) ?: return
        selectOption(itemId, optionIndex, decisionPath = path)
    }

    private fun selectOption(itemId: String, optionIndex: Int, decisionPath: List<Int>?) {
        val tool = _state.value.tool ?: return
        val item = tool.item(itemId) ?: return
        _state.update { current ->
            val items = current.answers.items.toMutableMap()
            val modes = current.alternativeModes.toMutableMap()
            // Una scelta manuale del livello annulla il percorso dell'albero decisionale.
            val paths = current.answers.decisionPaths.toMutableMap()
            if (decisionPath == null) paths.remove(itemId) else paths[itemId] = decisionPath
            items[itemId] = optionIndex
            item.alternativeOf?.let { primaryId ->
                items.remove(primaryId)
                paths.remove(primaryId)
                modes[primaryId] = true
            }
            tool.alternativeFor(itemId)?.let { alternative ->
                items.remove(alternative.id)
                paths.remove(alternative.id)
                modes[itemId] = false
            }
            current.copy(answers = current.answers.copy(items = items, decisionPaths = paths), alternativeModes = modes, dirty = true)
        }
    }

    fun setUseAlternative(primaryId: String, useAlternative: Boolean) {
        val tool = _state.value.tool ?: return
        val alternative = tool.alternativeFor(primaryId) ?: return
        _state.update { current ->
            val items = current.answers.items.toMutableMap()
            if (useAlternative) items.remove(primaryId) else items.remove(alternative.id)
            current.copy(
                answers = current.answers.copy(items = items),
                alternativeModes = current.alternativeModes + (primaryId to useAlternative),
                dirty = true,
            )
        }
    }

    fun save(onSaved: (String) -> Unit) {
        val current = _state.value
        val tool = current.tool ?: return
        val person = current.person ?: return
        if (current.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val score = tool.score(current.answers)
            val now = System.currentTimeMillis()
            val entity = AssessmentEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                personId = person.id,
                toolId = tool.id,
                date = Dates.toIso(current.date),
                evaluator = current.evaluator.trim().ifBlank { null },
                setting = current.setting.trim().ifBlank { null },
                answersJson = AnswersCodec.encode(current.answers),
                totalScore = score.total,
                maxScore = score.max,
                isComplete = score.isComplete,
                notes = current.notes.trim().ifBlank { null },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
            repository.saveAssessment(entity)
            existing = entity
            _state.update { it.copy(saving = false, dirty = false) }
            onSaved(entity.id)
        }
    }
}

// ---------------------------------------------------------------------------
// Dettaglio valutazione
// ---------------------------------------------------------------------------

data class AssessmentDetailUiState(
    val data: AssessmentWithPerson? = null,
    val tool: ToolDefinition? = null,
    val answers: AssessmentAnswers = AssessmentAnswers(),
    val score: ScoreResult? = null,
    val loading: Boolean = true,
)

class AssessmentDetailViewModel(container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val repository = container.repository
    private val assessmentId: String = checkNotNull(handle["assessmentId"])

    val state: StateFlow<AssessmentDetailUiState> = repository.observeAssessment(assessmentId).map { data ->
        if (data == null) {
            AssessmentDetailUiState(loading = false)
        } else {
            val tool = ToolRegistry.byId(data.assessment.toolId)
            val answers = AnswersCodec.decode(data.assessment.answersJson)
            AssessmentDetailUiState(
                data = data,
                tool = tool,
                answers = answers,
                score = tool?.score(answers),
                loading = false,
            )
        }
    }.stateWhileSubscribed(viewModelScope, AssessmentDetailUiState())

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAssessment(assessmentId)
            onDeleted()
        }
    }
}

// ---------------------------------------------------------------------------
// Anteprima PDF
// ---------------------------------------------------------------------------

data class PdfPreviewState(
    val file: File? = null,
    val fileName: String = "",
    val subject: String = "",
    val pages: List<ImageBitmap> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class PdfPreviewViewModel(private val container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val assessmentId: String = checkNotNull(handle["assessmentId"])
    private val _state = MutableStateFlow(PdfPreviewState())
    val state: StateFlow<PdfPreviewState> = _state.asStateFlow()

    init {
        generate()
    }

    fun generate() {
        viewModelScope.launch {
            _state.value = PdfPreviewState(loading = true)
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val data = container.repository.getAssessment(assessmentId)
                        ?: error("Valutazione non trovata")
                    val tool = ToolRegistry.byId(data.assessment.toolId)
                        ?: error("Strumento non disponibile")
                    val exporter = AssessmentPdfExporter(container.pdfBackend)
                    val input = AssessmentPdfExporter.Input(
                        assessment = data.assessment,
                        person = data.person,
                        tool = tool,
                        profile = container.settings.profile.value,
                    )
                    val file = exporter.export(input, container.reportsDir)
                    val pages = renderPdfPages(file)
                    val subject = "${tool.name} – ${data.person.fullName} – ${Dates.formatIsoShort(data.assessment.date)}"
                    Triple(file, pages, subject)
                }
            }
            _state.value = result.fold(
                onSuccess = { (file, pages, subject) ->
                    PdfPreviewState(file = file, fileName = file.name, subject = subject, pages = pages, loading = false)
                },
                onFailure = { PdfPreviewState(loading = false, error = it.message ?: "Errore nella generazione del PDF") },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Impostazioni
// ---------------------------------------------------------------------------

class SettingsViewModel(container: AppContainer) : ViewModel() {
    private val settings = container.settings
    val profile: StateFlow<EvaluatorProfile> = settings.profile

    fun save(profile: EvaluatorProfile) = settings.save(profile)
}
