package it.stradesumisura.app.domain

import org.jetbrains.compose.resources.DrawableResource
import kotlin.math.abs
import kotlin.math.max

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
    /**
     * Opzione «non applicabile» (es. «la carrozzina non ha braccioli»): se selezionata l'item è
     * compilato ma non entra nel punteggio né nel massimo.
     */
    val notApplicable: Boolean = false,
    /**
     * Punteggio per variante (es. carrozzina elettrica nella TAI): valore della variante -> punteggio,
     * null = non applicabile in quella variante. La variante è il valore del campo
     * [ToolDefinition.variantFieldId]; [score] vale per le varianti non elencate.
     */
    val variantScores: Map<String, Int?> = emptyMap(),
    /** Immagine che illustra l'opzione (es. le posizioni della mano nella TAI). */
    val imageRes: DrawableResource? = null,
    /** Gruppo dell'opzione negli item a scelta multipla raggruppata (es. la superficie di arrivo). */
    val group: String? = null,
) {
    /** L'opzione con il punteggio della variante indicata (es. carrozzina elettrica). */
    fun resolved(variant: String?): ToolOption {
        if (variant == null || !variantScores.containsKey(variant)) return this
        val value = variantScores[variant]
        return if (value == null) copy(score = 0, notApplicable = true) else copy(score = value)
    }

    /** Punteggio più alto ottenibile con l'opzione in una qualsiasi variante. */
    val bestScore: Int get() = (listOf(score.takeIf { !notApplicable }) + variantScores.values).filterNotNull().maxOrNull() ?: 0

    /** Punteggio più basso ottenibile con l'opzione in una qualsiasi variante. */
    val worstScore: Int get() = (listOf(score.takeIf { !notApplicable }) + variantScores.values).filterNotNull().minOrNull() ?: 0
}

/** Regola di calcolo automatico del punteggio di un item a partire da altri item. */
sealed interface ComputedScore {
    val itemIds: List<String>

    /** Il punteggio è il massimo (cioè il peggiore) tra quelli degli item indicati. */
    data class MaxOf(override val itemIds: List<String>) : ComputedScore

    /** Il punteggio è la media aritmetica degli item indicati, arrotondata per difetto. */
    data class FloorMeanOf(override val itemIds: List<String>) : ComputedScore
}

/**
 * Albero decisionale che guida il valutatore al livello di un item con una sequenza di domande
 * (es. gli alberi decisionali della FIM). Ogni risposta porta a un'altra domanda o a un risultato.
 */
sealed interface DecisionNode {
    /** Domanda con le risposte possibili (di solito «Sì» e «No»). */
    data class Question(val text: String, val answers: List<DecisionAnswer>) : DecisionNode

    /** Livello raggiunto; [label] è il nome del livello nell'albero, se diverso da quello dell'opzione. */
    data class Result(val score: Int, val label: String? = null) : DecisionNode

    /**
     * Più alberi da percorrere in sequenza: il livello dell'item è il più basso tra i risultati
     * (es. Vescica e Alvo della FIM: livello di assistenza e frequenza degli episodi di incontinenza).
     */
    data class LowerOf(val parts: List<DecisionPart>) : DecisionNode

    companion object {
        fun level(score: Int, label: String? = null): DecisionNode = Result(score, label)

        /** Domanda a cui si risponde «Sì» o «No». */
        fun yesNo(text: String, yes: DecisionNode, no: DecisionNode): DecisionNode =
            Question(text, listOf(DecisionAnswer("Sì", yes), DecisionAnswer("No", no)))

        /** Domanda con più risposte possibili. */
        fun choice(text: String, vararg answers: Pair<String, DecisionNode>): DecisionNode =
            Question(text, answers.map { DecisionAnswer(it.first, it.second) })
    }
}

data class DecisionAnswer(val label: String, val next: DecisionNode)

/** Una parte di un albero composto ([DecisionNode.LowerOf]), con il proprio titolo. */
data class DecisionPart(val title: String, val root: DecisionNode)

/** Esito di un percorso nell'albero decisionale di un item. */
data class DecisionOutcome(
    /** Livello finale. */
    val score: Int,
    /** Nome del livello, se l'albero ne dichiara uno. */
    val label: String?,
    /** Per gli alberi composti, il livello raggiunto in ciascuna parte (titolo -> livello). */
    val partScores: List<Pair<String, Int>>,
)

/**
 * Percorre l'albero con le risposte date (indici delle risposte, nell'ordine). Restituisce l'esito
 * se il percorso è completo, altrimenti null.
 */
fun DecisionNode.resolve(path: List<Int>): DecisionOutcome? {
    var cursor = 0

    fun walk(node: DecisionNode): DecisionOutcome? = when (node) {
        is DecisionNode.Result -> DecisionOutcome(node.score, node.label, emptyList())
        is DecisionNode.Question -> {
            val answer = path.getOrNull(cursor)?.let { node.answers.getOrNull(it) } ?: return null
            cursor++
            walk(answer.next)
        }
        is DecisionNode.LowerOf -> {
            val outcomes = node.parts.map { part -> part.title to (walk(part.root) ?: return null) }
            val lowest = outcomes.minBy { it.second.score }
            DecisionOutcome(
                score = lowest.second.score,
                label = lowest.second.label,
                partScores = outcomes.map { it.first to it.second.score },
            )
        }
    }
    return walk(this)?.takeIf { cursor == path.size }
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
    /** Titolo breve per chip e riquadri compatti (es. "Qualità" per "Qualità di performance o del prodotto"). */
    val shortTitle: String? = null,
    /**
     * Se valorizzato, l'item è a conteggio: il punteggio è il numero di risposte corrette da 0 a
     * questo massimo (le opzioni sono generate da [count]) invece della scelta di un livello.
     */
    val countMax: Int? = null,
    /** Unità del conteggio, es. "risposte corrette"; usata nei titoli delle opzioni generate. */
    val countUnit: String = "risposte corrette",
    /**
     * Sotto-prove di un item a conteggio: l'operatore segna ciascuna come riuscita o non riuscita
     * e il punteggio è il numero di quelle riuscite. L'item è compilato quando sono tutte segnate.
     */
    val subtasks: List<String> = emptyList(),
    /**
     * Item facoltativo (es. una sezione somministrata solo ad alcune persone): se non compilato
     * non conta né come mancante né nel punteggio massimo della valutazione.
     */
    val optional: Boolean = false,
    /** Numero mostrato accanto all'item (es. "7a") al posto della numerazione progressiva. */
    val number: String? = null,
    /**
     * Albero decisionale che guida alla scelta del livello (es. FIM): il valutatore risponde alle
     * domande e il livello raggiunto viene selezionato tra le [options] con lo stesso punteggio.
     */
    val decisionTree: DecisionNode? = null,
    /**
     * Item a scelta multipla («segnare tutte le voci che si applicano»): la risposta è l'insieme
     * delle opzioni selezionate (salvato come maschera di bit) e il punteggio è la somma dei loro
     * punteggi, mai sotto zero.
     */
    val multiSelect: Boolean = false,
    /** Per gli item a scelta multipla: il punteggio è la media dei punteggi selezionati (es. A/B nella TAI). */
    val multiSelectMean: Boolean = false,
    /** Etichetta del gruppo di opzioni da scegliere prima (es. «Superficie di arrivo»), per le opzioni con [ToolOption.group]. */
    val optionGroupLabel: String? = null,
    /** Immagine mostrata sotto il titolo dell'item (es. lo schema degli angoli nella TAI). */
    val imageRes: DrawableResource? = null,
    /** Titolo di sezione mostrato prima dell'item (es. «Fase I – Preparazione della carrozzina»). */
    val section: String? = null,
) {
    val compactTitle: String get() = shortTitle ?: title

    /** Punteggio massimo dell'item in qualunque variante (per la scelta multipla: somma dei punteggi positivi). */
    val maxScore: Int
        get() = when {
            multiSelect && !multiSelectMean -> options.sumOf { it.bestScore.coerceAtLeast(0) }
            else -> options.maxOf { it.bestScore }
        }

    /** Punteggio minimo dell'item (zero per la scelta multipla, che non scende sotto zero). */
    val minScore: Int get() = if (multiSelect) 0 else options.minOf { it.worstScore }
    val isComputed: Boolean get() = computed != null
    val isCount: Boolean get() = countMax != null
    val hasSubtasks: Boolean get() = subtasks.isNotEmpty()
    val hasDecisionTree: Boolean get() = decisionTree != null
    val hasOptionGroups: Boolean get() = options.any { it.group != null }

    /** Gruppi delle opzioni nell'ordine di prima comparsa (es. le superfici). */
    val optionGroups: List<String> get() = options.mapNotNull { it.group }.distinct()

    /** Indici delle opzioni selezionate in un item a scelta multipla, dalla maschera di bit salvata. */
    fun selectedIndices(mask: Int): List<Int> = options.indices.filter { mask and (1 shl it) != 0 }

    /** Maschera di bit con l'opzione [index] selezionata oppure no. */
    fun toggled(mask: Int, index: Int): Int = mask xor (1 shl index)

    /** Indice dell'opzione con il punteggio indicato, oppure null. */
    fun optionIndexFor(score: Int): Int? = options.indexOfFirst { it.score == score }.takeIf { it >= 0 }

    /** Intervallo dei punteggi dell'item, es. "0–3" oppure "1–10". */
    val scoreRangeLabel: String get() = "$minScore–$maxScore"

    companion object {
        /** Item a conteggio: punteggio = numero di risposte corrette su [max]. */
        fun count(
            id: String,
            title: String,
            max: Int,
            hint: String? = null,
            description: String? = null,
            subtasks: List<String> = emptyList(),
            noteLabel: String? = null,
            optional: Boolean = false,
            number: String? = null,
            unit: String = "risposte corrette",
        ): ToolItem {
            require(subtasks.isEmpty() || subtasks.size == max) { "Le sotto-prove di $id devono essere $max" }
            return ToolItem(
                id = id,
                title = title,
                options = (0..max).map { n -> ToolOption(n, "$n $unit su $max", "") },
                hint = hint,
                description = description,
                noteLabel = noteLabel,
                countMax = max,
                countUnit = unit,
                subtasks = subtasks,
                optional = optional,
                number = number,
            )
        }
    }
}

/** Campo di testo raccolto prima degli item (es. compito osservato, diagnosi). */
data class ToolField(
    val id: String,
    val label: String,
    val placeholder: String? = null,
    val multiline: Boolean = false,
    /** Etichetta compatta per il PDF (es. "Compito" invece di "Compito osservato"). */
    val shortLabel: String? = null,
    /** Se non vuoto, il campo è una scelta tra questi valori (pulsanti) invece di un testo libero. */
    val choices: List<String> = emptyList(),
) {
    val pdfLabel: String get() = shortLabel ?: label
    val isChoice: Boolean get() = choices.isNotEmpty()
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

/**
 * Area di uno strumento (es. area motoria e area cognitiva della FIM): un sottoinsieme di item con
 * un proprio subtotale, mostrato nei riepiloghi e nel report.
 */
data class ToolGroup(
    val id: String,
    val title: String,
    val itemIds: List<String>,
    /** Titolo breve per chip e riquadri compatti. */
    val shortTitle: String? = null,
    /** Riga descrittiva sotto il titolo (es. le sotto-aree comprese). */
    val subtitle: String? = null,
    /**
     * Area facoltativa (es. il secondo trasferimento della TAI): le sue voci sono richieste solo
     * se almeno una è stata compilata; altrimenti non contano né come mancanti né nel massimo.
     */
    val optional: Boolean = false,
) {
    val compactTitle: String get() = shortTitle ?: title
}

/** Tutte le risposte di una valutazione. */
data class AssessmentAnswers(
    /** itemId -> indice dell'opzione selezionata. */
    val items: Map<String, Int> = emptyMap(),
    /** itemId -> nota libera. */
    val itemNotes: Map<String, String> = emptyMap(),
    /** fieldId -> valore del campo di testo. */
    val fields: Map<String, String> = emptyMap(),
    /** itemId -> (indice sotto-prova -> riuscita) per gli item a conteggio con sotto-prove. */
    val subtaskResults: Map<String, Map<Int, Boolean>> = emptyMap(),
    /** itemId -> risposte date nell'albero decisionale (indici), quando il livello è stato scelto così. */
    val decisionPaths: Map<String, List<Int>> = emptyMap(),
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
    /** Item facoltativi non somministrati (esclusi da [required] e da [max]). */
    val skipped: Int = 0,
    /** Punteggio più basso ottenibile (0 per i conteggi, 18 per la FIM). */
    val min: Int = 0,
    /** Item compilati come «non applicabile» (esclusi dal massimo). */
    val notApplicable: Int = 0,
    /** Frazione calcolata con una regola propria dello strumento (es. media delle percentuali dei trasferimenti nella TAI). */
    val fractionOverride: Float? = null,
) {
    val isComplete: Boolean get() = answered == required

    /** Posizione del punteggio tra [min] e [max], da 0 a 1. */
    val fraction: Float
        get() = fractionOverride?.coerceIn(0f, 1f)
            ?: if (max <= min) 0f else ((total - min).toFloat() / (max - min)).coerceIn(0f, 1f)
    val percent: Int get() = (fraction * 100f + 0.5f).toInt()
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
    /**
     * Item da evidenziare accanto al punteggio principale (es. i due valori inseriti dal valutatore
     * da cui viene calcolato il punteggio). Vuoto per gli strumenti che non lo prevedono.
     */
    val highlightedItemIds: List<String> = emptyList(),
    /** Aree con subtotale (es. motoria e cognitiva nella FIM); vuoto per gli strumenti senza aree. */
    val groups: List<ToolGroup> = emptyList(),
    /**
     * Divisore dei punteggi: i punteggi sono salvati come interi e mostrati divisi per questo valore
     * (100 per la TAI, che assegna 0,5 e −0,5 punti). Usare [formatScore] per mostrarli.
     */
    val scoreDivisor: Int = 1,
    /** Campo a scelta il cui valore seleziona i punteggi per variante delle opzioni ([ToolOption.variantScores]). */
    val variantFieldId: String? = null,
    /** True quando il punteggio principale si legge come percentuale del massimo applicabile (es. TAI). */
    val percentHeadline: Boolean = false,
    /** Se valorizzato, la percentuale complessiva è la media delle percentuali delle aree compilate (es. i due trasferimenti della TAI). */
    val groupPercentMean: Boolean = false,
    /** Se valorizzato, il totale non scende sotto questo valore (es. 0 per la TAI). */
    val totalFloor: Int? = null,
    /** Testo che spiega il calcolo del punteggio nella scheda dello strumento, se diverso da quello generato. */
    val scoringNote: String? = null,
    /** Etichetta del numero di item nelle schede (es. «18 voci per trasferimento»), se diversa dal conteggio. */
    val itemsCountLabel: String? = null,
) {
    /** Item mostrati nei riepiloghi (le alternative si raggiungono dal loro item principale). */
    val primaryItems: List<ToolItem> = items.filter { it.alternativeOf == null }

    /** Item che il valutatore compila (esclusi quelli calcolati automaticamente). */
    val answerableItems: List<ToolItem> = primaryItems.filter { !it.isComputed }

    /** True quando il punteggio è un conteggio di risposte corrette (tutti gli item a conteggio). */
    val countsAnswers: Boolean = answerableItems.isNotEmpty() && answerableItems.all { it.isCount }

    /** True quando il punteggio principale va letto come percentuale (conteggi e strumenti con [percentHeadline]). */
    val showsPercent: Boolean get() = countsAnswers || percentHeadline

    /** Testo che segue la percentuale, es. «di risposte corrette» oppure «del punteggio massimo applicabile». */
    val percentNoun: String get() = if (countsAnswers) "di risposte corrette" else "del punteggio massimo applicabile"

    /** Punteggio formattato per la visualizzazione, es. "7" oppure "0,5" quando [scoreDivisor] è 100. */
    fun formatScore(value: Int): String {
        if (scoreDivisor == 1) return "$value"
        val negative = value < 0
        val abs = kotlin.math.abs(value)
        val whole = abs / scoreDivisor
        var rest = abs % scoreDivisor
        var digits = scoreDivisor.toString().length - 1
        while (rest > 0 && rest % 10 == 0) {
            rest /= 10
            digits--
        }
        val text = if (rest == 0) "$whole" else "$whole,${rest.toString().padStart(digits, '0')}"
        return if (negative) "−$text" else text
    }

    /** Intervallo formattato, es. "0–19" oppure "0–100%" per gli strumenti letti in percentuale. */
    fun formatRange(min: Int, max: Int): String = "${formatScore(min)}–${formatScore(max)}"

    /** Intervallo dei punteggi di un item, formattato (es. "0–1" oppure "0–2"). */
    fun rangeLabelOf(item: ToolItem): String = formatRange(item.minScore, item.maxScore)

    /** Etichetta del numero di item, es. "18 voci" oppure quella dichiarata dallo strumento. */
    val itemsCountText: String get() = itemsCountLabel ?: "${answerableItems.size} $itemNounPlural"

    /** Valore della variante scelta (es. il tipo di carrozzina), se lo strumento la prevede. */
    fun variantOf(answers: AssessmentAnswers): String? = variantFieldId?.let { answers.fields[it] }

    /** Le opzioni di un item con i punteggi della variante scelta. */
    fun optionsFor(item: ToolItem, answers: AssessmentAnswers): List<ToolOption> {
        val variant = variantOf(answers)
        return item.options.map { it.resolved(variant) }
    }

    /** True quando almeno un item prevede l'albero decisionale. */
    val hasDecisionTrees: Boolean = items.any { it.hasDecisionTree }

    val hasGroups: Boolean get() = groups.isNotEmpty()

    /** Item principali di un'area, nell'ordine dello strumento. */
    fun itemsOf(group: ToolGroup): List<ToolItem> = primaryItems.filter { it.id in group.itemIds }

    /** Area a cui appartiene un item, se lo strumento prevede aree. */
    fun groupOf(item: ToolItem): ToolGroup? = groups.firstOrNull { item.id in it.itemIds }

    /** True quando il punteggio principale è la somma degli item compilati. */
    val sumsItems: Boolean get() = headlineItemId == null

    val headlineItem: ToolItem? = headlineItemId?.let { id -> items.firstOrNull { it.id == id } }

    val maxScore: Int = headlineItem?.maxScore ?: answerableItems.sumOf { it.maxScore }

    /** Punteggio più basso ottenibile (0 per la maggior parte delle scale, 1 per la PQRS). */
    val minScore: Int = (headlineItem?.minScore ?: answerableItems.sumOf { it.minScore }).let { min ->
        totalFloor?.let { floor -> max(min, floor) } ?: min
    }

    /** Intervallo del punteggio principale, es. "0–100" oppure "1–10" (in percentuale per gli strumenti letti così). */
    val scoreRangeLabel: String get() = if (percentHeadline) "0–100%" else formatRange(minScore, maxScore)

    /** Item evidenziati accanto al punteggio principale, nell'ordine dichiarato. */
    val highlightedItems: List<ToolItem> = highlightedItemIds.mapNotNull { id -> items.firstOrNull { it.id == id } }

    fun item(id: String): ToolItem? = items.firstOrNull { it.id == id }

    fun field(id: String): ToolField? = fields.firstOrNull { it.id == id }

    fun alternativeFor(primaryId: String): ToolItem? = items.firstOrNull { it.alternativeOf == primaryId }

    fun bandFor(total: Int): ScoreBand? = bands.firstOrNull { it.contains(total) }

    /** Punteggi degli item di origine di una regola, oppure null se manca anche uno solo di essi. */
    private fun sourceScores(rule: ComputedScore, answers: Map<String, Int>): List<Int>? {
        val scores = rule.itemIds.map { id -> item(id)?.let { source -> selectionFor(source, answers)?.option?.score } }
        return if (scores.isEmpty() || scores.any { it == null }) null else scores.filterNotNull()
    }

    /** Valore di un item calcolato, oppure null se manca anche uno solo degli item di origine. */
    fun computedValue(item: ToolItem, answers: Map<String, Int>): Int? {
        val rule = item.computed ?: return null
        val scores = sourceScores(rule, answers) ?: return null
        return when (rule) {
            is ComputedScore.MaxOf -> scores.max()
            // Divisione intera: per punteggi non negativi coincide con l'arrotondamento per difetto.
            is ComputedScore.FloorMeanOf -> scores.sum() / scores.size
        }
    }

    /**
     * Spiegazione leggibile del calcolo di un item calcolato, es. "(6 + 7) ÷ 2 = 6,5 → 6",
     * oppure null se il valore non è ancora calcolabile.
     */
    fun computedExplanation(item: ToolItem, answers: Map<String, Int>): String? {
        val rule = item.computed ?: return null
        val scores = sourceScores(rule, answers) ?: return null
        val value = computedValue(item, answers) ?: return null
        return when (rule) {
            is ComputedScore.MaxOf -> "livello più alto tra ${scores.joinToString(", ")} → $value"
            is ComputedScore.FloorMeanOf -> {
                val sum = scores.sum()
                val exact = if (sum % scores.size == 0) "$value" else "%.1f".format(java.util.Locale.ITALIAN, sum.toDouble() / scores.size)
                val rounding = if (sum % scores.size == 0) "" else ", arrotondata per difetto → $value"
                "media (${scores.joinToString(" + ")}) ÷ ${scores.size} = $exact$rounding"
            }
        }
    }

    fun computedExplanation(item: ToolItem, answers: AssessmentAnswers): String? = computedExplanation(item, answers.items)

    /**
     * Risolve la selezione di un item principale: per gli item calcolati l'opzione con il punteggio
     * derivato; per gli altri la risposta dell'item stesso, poi quella della sua alternativa.
     */
    fun selectionFor(primary: ToolItem, answers: Map<String, Int>, variant: String? = null): Selection? {
        if (primary.isComputed) {
            val value = computedValue(primary, answers) ?: return null
            val index = primary.options.indices.minByOrNull { abs(primary.options[it].score - value) } ?: return null
            return Selection(primary, index, primary.options[index], isAlternative = false)
        }
        answers[primary.id]?.let { index ->
            if (primary.multiSelect) {
                multiSelection(primary, index, variant)?.let { return it }
            } else {
                primary.options.getOrNull(index)?.let { return Selection(primary, index, it.resolved(variant), isAlternative = false) }
            }
        }
        val alternative = alternativeFor(primary.id) ?: return null
        val index = answers[alternative.id] ?: return null
        val option = alternative.options.getOrNull(index) ?: return null
        return Selection(alternative, index, option.resolved(variant), isAlternative = true)
    }

    fun selectionFor(primary: ToolItem, answers: AssessmentAnswers): Selection? =
        selectionFor(primary, answers.items, variantOf(answers))

    /**
     * Selezione di un item a scelta multipla: un'opzione «riassunto» con i titoli delle opzioni
     * selezionate e il punteggio dell'item (somma non negativa oppure media, secondo l'item).
     * L'item è «non applicabile» quando tutte le opzioni selezionate lo sono.
     */
    private fun multiSelection(item: ToolItem, mask: Int, variant: String?): Selection? {
        val indices = item.selectedIndices(mask)
        if (indices.isEmpty()) return null
        val chosen = indices.map { item.options[it].resolved(variant) }
        val applicable = chosen.filter { !it.notApplicable }
        val score = when {
            applicable.isEmpty() -> 0
            item.multiSelectMean -> Math.round(applicable.sumOf { it.score }.toDouble() / applicable.size).toInt()
            else -> applicable.sumOf { it.score }.coerceAtLeast(0)
        }
        val group = chosen.firstOrNull()?.group
        val title = buildString {
            if (group != null) append("$group: ")
            append(chosen.joinToString(" · ") { if (group != null) it.title.replaceFirstChar(Char::lowercase) else it.title })
        }
        val description = if (item.multiSelectMean && applicable.size > 1) {
            "Media di ${applicable.size} posizioni: (${applicable.joinToString(" + ") { formatScore(it.score) }}) ÷ ${applicable.size}"
        } else {
            ""
        }
        val summary = ToolOption(score, title, description, notApplicable = applicable.isEmpty(), group = group)
        return Selection(item, mask, summary, isAlternative = false)
    }

    /** True quando un item di un'area facoltativa va compilato: l'area è stata iniziata. */
    private fun groupStarted(group: ToolGroup, answers: AssessmentAnswers): Boolean =
        itemsOf(group).any { !it.isComputed && selectionFor(it, answers) != null }

    /**
     * Punteggio di un insieme di item: gli item facoltativi (o delle aree facoltative non iniziate)
     * non compilati non contano né come mancanti né nel massimo; gli item «non applicabili» sono
     * compilati ma esclusi dal massimo.
     */
    private fun scoreOf(candidates: List<ToolItem>, answers: AssessmentAnswers): ScoreResult {
        val startedGroups = groups.filter { !it.optional || groupStarted(it, answers) }.map { it.id }.toSet()
        val relevant = candidates.filter { item ->
            val selected = selectionFor(item, answers) != null
            val optionalGroup = groupOf(item)?.let { it.optional && it.id !in startedGroups } ?: false
            selected || (!item.optional && !optionalGroup)
        }
        val selected = relevant.mapNotNull { selectionFor(it, answers) }
        val notApplicable = selected.count { it.option.notApplicable }
        val applicable = relevant.filter { item -> selectionFor(item, answers)?.option?.notApplicable != true }
        val rawTotal = selected.filter { !it.option.notApplicable }.sumOf { it.option.score }
        val total = totalFloor?.let { max(rawTotal, it) } ?: rawTotal
        val min = applicable.sumOf { it.minScore }.let { m -> totalFloor?.let { max(m, it) } ?: m }
        return ScoreResult(
            total = total,
            max = applicable.sumOf { it.maxScore },
            answered = selected.size,
            required = relevant.size,
            band = null,
            // Le aree facoltative non iniziate non sono «prove non somministrate»: contano solo gli item facoltativi.
            skipped = candidates.count { it.optional && it !in relevant },
            min = min,
            notApplicable = notApplicable,
        )
    }

    /** answers: itemId -> indice opzione (senza campi: le varianti e le aree facoltative usano i valori di base). */
    fun score(answers: Map<String, Int>): ScoreResult = score(AssessmentAnswers(items = answers))

    fun score(answers: AssessmentAnswers): ScoreResult {
        val headline = headlineItem
        if (headline != null) {
            val relevant = answerableItems.filter { !it.optional || selectionFor(it, answers) != null }
            val selected = relevant.mapNotNull { selectionFor(it, answers) }
            val complete = selected.size == relevant.size
            val total = selectionFor(headline, answers)?.option?.score ?: 0
            return ScoreResult(
                total = total,
                max = headline.maxScore,
                answered = selected.size,
                required = relevant.size,
                band = if (complete) bandFor(total) else null,
                skipped = answerableItems.size - relevant.size,
                min = headline.minScore,
            )
        }
        val result = scoreOf(answerableItems, answers)
        val fractionOverride = if (groupPercentMean && groups.isNotEmpty()) {
            val started = groups.filter { !it.optional || groupStarted(it, answers) }.map { groupScore(it, answers) }
            if (started.isEmpty()) 0f else started.map { it.fraction }.average().toFloat()
        } else {
            null
        }
        return result.copy(
            band = if (result.isComplete) bandFor(result.total) else null,
            fractionOverride = fractionOverride,
        )
    }

    /** Subtotale di un'area: somma dei livelli dei suoi item compilati. */
    fun groupScore(group: ToolGroup, answers: AssessmentAnswers): ScoreResult =
        scoreOf(itemsOf(group).filter { !it.isComputed }, answers)

    /** Esito dell'albero decisionale di un item, se il livello è stato scelto con la guida. */
    fun decisionOutcome(item: ToolItem, answers: AssessmentAnswers): DecisionOutcome? {
        val tree = item.decisionTree ?: return null
        val path = answers.decisionPaths[item.id] ?: return null
        val outcome = tree.resolve(path) ?: return null
        // Il percorso vale solo se corrisponde ancora al livello selezionato.
        val selected = answers.items[item.id]?.let { item.options.getOrNull(it)?.score } ?: return null
        return outcome.takeIf { it.score == selected }
    }

    /**
     * Testo che riassume come è stato scelto il livello con l'albero decisionale, es.
     * "Livello di assistenza 6 · Frequenza degli episodi 4 → il più basso: 4", oppure null.
     */
    fun decisionSummary(item: ToolItem, answers: AssessmentAnswers): String? {
        val outcome = decisionOutcome(item, answers) ?: return null
        return if (outcome.partScores.size > 1) {
            "Albero decisionale: " + outcome.partScores.joinToString(" · ") { (title, score) -> "${title.lowercase()} $score" } +
                " → il più basso: ${outcome.score}"
        } else {
            "Livello scelto con l'albero decisionale" + (outcome.label?.let { ": $it" } ?: "")
        }
    }

    /** True se lo strumento prevede fasce interpretative; altrimenti il punteggio si legge in percentuale. */
    val hasBands: Boolean get() = bands.isNotEmpty()

    /** Numero da mostrare accanto a un item: quello dichiarato oppure la posizione progressiva. */
    fun numberOf(item: ToolItem): String? =
        item.number ?: answerableItems.indexOf(item).takeIf { it >= 0 }?.let { "${it + 1}" }

    /** Sotto-prove non riuscite di un item a conteggio, nell'ordine, come "3) testo". */
    fun failedSubtasks(item: ToolItem, answers: AssessmentAnswers): List<String> {
        val results = answers.subtaskResults[item.id] ?: return emptyList()
        return item.subtasks.mapIndexedNotNull { index, text -> if (results[index] == false) "${index + 1}) $text" else null }
    }

    /** Testo che riassume la valutazione negli elenchi, se lo strumento lo prevede. */
    fun summaryOf(answers: AssessmentAnswers): String? =
        summaryFieldId?.let { id -> answers.fields[id]?.trim()?.takeIf { it.isNotEmpty() } }
}
