package it.stradesumisura.app.domain

import kotlin.math.abs

/**
 * Modello generico di uno strumento di valutazione (scala o scheda di osservazione).
 * Ogni strumento è una lista di item con opzioni a punteggio; opzionalmente raccoglie campi di
 * testo aggiuntivi (es. il compito osservato) e una nota libera per ciascun item. Un item può
 * essere calcolato automaticamente a partire da altri item (vedi [ComputedScore]).
 * Per aggiungere uno strumento: creare una [ToolDefinition] e registrarla in [ToolRegistry].
 */
data class ToolOption(
    val score: Int,
    val title: String,
    val description: String,
)

/** Regola di calcolo automatico del punteggio di un item a partire da altri item. */
sealed interface ComputedScore {
    /** Il punteggio è il massimo (cioè il peggiore) tra quelli degli item indicati. */
    data class MaxOf(val itemIds: List<String>) : ComputedScore
}

data class ToolItem(
    val id: String,
    val title: String,
    val options: List<ToolOption>,
    /** Breve indicazione mostrata sotto il titolo. */
    val hint: String? = null,
    /** Definizione estesa di ciò che si osserva; consultabile nella scheda e nel modulo. */
    val description: String? = null,
    /** Id dell'item principale che questo sostituisce (alternative mutuamente esclusive). */
    val alternativeOf: String? = null,
    /** Se valorizzato, per l'item viene raccolta una nota libera con questa etichetta. */
    val noteLabel: String? = null,
    /** Se valorizzato, l'item non viene compilato ma calcolato automaticamente. */
    val computed: ComputedScore? = null,
) {
    val maxScore: Int get() = options.maxOf { it.score }
    val isComputed: Boolean get() = computed != null
}

/** Campo di testo raccolto prima degli item (es. compito osservato, diagnosi). */
data class ToolField(
    val id: String,
    val label: String,
    val placeholder: String? = null,
    val multiline: Boolean = false,
    /** Etichetta compatta per il PDF (es. "Compito" invece di "Compito osservato"). */
    val shortLabel: String? = null,
) {
    val pdfLabel: String get() = shortLabel ?: label
}

data class ScoreBand(
    val min: Int,
    val max: Int,
    val label: String,
    val description: String,
    /** Colore ARGB usato per chip, barre e PDF. */
    val color: Long,
) {
    fun contains(score: Int): Boolean = score in min..max
    val rangeLabel: String get() = if (min == max) "$min" else "$min–$max"
}

/** Tutte le risposte di una valutazione. */
data class AssessmentAnswers(
    /** itemId -> indice dell'opzione selezionata. */
    val items: Map<String, Int> = emptyMap(),
    /** itemId -> nota libera. */
    val itemNotes: Map<String, String> = emptyMap(),
    /** fieldId -> valore del campo di testo. */
    val fields: Map<String, String> = emptyMap(),
)

/** L'opzione effettivamente selezionata (o calcolata) per un item principale. */
data class Selection(
    val item: ToolItem,
    val optionIndex: Int,
    val option: ToolOption,
    val isAlternative: Boolean,
)

data class ScoreResult(
    val total: Int,
    val max: Int,
    val answered: Int,
    val required: Int,
    val band: ScoreBand?,
) {
    val isComplete: Boolean get() = answered == required
    val fraction: Float get() = if (max == 0) 0f else total.toFloat() / max
}

data class ToolDefinition(
    val id: String,
    val name: String,
    val shortName: String,
    /** Citazione breve mostrata accanto al nome, es. "Shah, Vanclay e Cooper, 1989". */
    val citation: String,
    val subtitle: String,
    val domain: String,
    val duration: String,
    val description: String,
    val instructions: List<String>,
    val reference: String,
    val items: List<ToolItem>,
    val bands: List<ScoreBand>,
    val version: String = "1.0",
    /** Campi di testo aggiuntivi raccolti nella sezione dati della valutazione. */
    val fields: List<ToolField> = emptyList(),
    /** Se valorizzato, il punteggio principale è quello di questo item e non la somma degli item. */
    val headlineItemId: String? = null,
    val totalLabel: String = "Punteggio totale",
    val itemNounSingular: String = "attività",
    val itemNounPlural: String = "attività",
    val itemsSectionTitle: String = "Attività valutate",
    val notesLabel: String = "Osservazioni",
    val notesHint: String? = null,
    /** Campo il cui valore riassume la valutazione negli elenchi (es. il compito osservato). */
    val summaryFieldId: String? = null,
) {
    /** Item mostrati nei riepiloghi (le alternative si raggiungono dal loro item principale). */
    val primaryItems: List<ToolItem> = items.filter { it.alternativeOf == null }

    /** Item che il valutatore compila (esclusi quelli calcolati automaticamente). */
    val answerableItems: List<ToolItem> = primaryItems.filter { !it.isComputed }

    /** True quando il punteggio principale è la somma degli item compilati. */
    val sumsItems: Boolean get() = headlineItemId == null

    val headlineItem: ToolItem? = headlineItemId?.let { id -> items.firstOrNull { it.id == id } }

    val maxScore: Int = headlineItem?.maxScore ?: answerableItems.sumOf { it.maxScore }

    fun item(id: String): ToolItem? = items.firstOrNull { it.id == id }

    fun field(id: String): ToolField? = fields.firstOrNull { it.id == id }

    fun alternativeFor(primaryId: String): ToolItem? = items.firstOrNull { it.alternativeOf == primaryId }

    fun bandFor(total: Int): ScoreBand? = bands.firstOrNull { it.contains(total) }

    /** Valore di un item calcolato, oppure null se manca anche uno solo degli item di origine. */
    fun computedValue(item: ToolItem, answers: Map<String, Int>): Int? = when (val rule = item.computed) {
        null -> null
        is ComputedScore.MaxOf -> {
            val scores = rule.itemIds.map { id -> item(id)?.let { source -> selectionFor(source, answers)?.option?.score } }
            if (scores.any { it == null }) null else scores.filterNotNull().maxOrNull()
        }
    }

    /**
     * Risolve la selezione di un item principale: per gli item calcolati l'opzione con il punteggio
     * derivato; per gli altri la risposta dell'item stesso, poi quella della sua alternativa.
     */
    fun selectionFor(primary: ToolItem, answers: Map<String, Int>): Selection? {
        if (primary.isComputed) {
            val value = computedValue(primary, answers) ?: return null
            val index = primary.options.indices.minByOrNull { abs(primary.options[it].score - value) } ?: return null
            return Selection(primary, index, primary.options[index], isAlternative = false)
        }
        answers[primary.id]?.let { index ->
            primary.options.getOrNull(index)?.let { return Selection(primary, index, it, isAlternative = false) }
        }
        val alternative = alternativeFor(primary.id) ?: return null
        val index = answers[alternative.id] ?: return null
        val option = alternative.options.getOrNull(index) ?: return null
        return Selection(alternative, index, option, isAlternative = true)
    }

    fun selectionFor(primary: ToolItem, answers: AssessmentAnswers): Selection? = selectionFor(primary, answers.items)

    /** answers: itemId -> indice opzione. */
    fun score(answers: Map<String, Int>): ScoreResult {
        val selected = answerableItems.mapNotNull { selectionFor(it, answers) }
        val complete = selected.size == answerableItems.size
        val headline = headlineItem
        val total = if (headline != null) {
            selectionFor(headline, answers)?.option?.score ?: 0
        } else {
            selected.sumOf { it.option.score }
        }
        return ScoreResult(
            total = total,
            max = maxScore,
            answered = selected.size,
            required = answerableItems.size,
            band = if (complete) bandFor(total) else null,
        )
    }

    fun score(answers: AssessmentAnswers): ScoreResult = score(answers.items)

    /** Testo che riassume la valutazione negli elenchi, se lo strumento lo prevede. */
    fun summaryOf(answers: AssessmentAnswers): String? =
        summaryFieldId?.let { id -> answers.fields[id]?.trim()?.takeIf { it.isNotEmpty() } }
}
