package it.stradesumisura.app.domain

/**
 * Generic model of an assessment tool (scale). Every tool is a list of items, each with a set of
 * scored options; the sum of the selected options gives the total, interpreted through bands.
 * New tools are added by creating a [ToolDefinition] and registering it in [ToolRegistry].
 */
data class ToolOption(
    val score: Int,
    val title: String,
    val description: String,
)

data class ToolItem(
    val id: String,
    val title: String,
    val options: List<ToolOption>,
    /** Short guidance shown under the title. */
    val hint: String? = null,
    /** Id of the primary item this one replaces (mutually exclusive alternatives, e.g. wheelchair vs walking). */
    val alternativeOf: String? = null,
) {
    val maxScore: Int get() = options.maxOf { it.score }
}

data class ScoreBand(
    val min: Int,
    val max: Int,
    val label: String,
    val description: String,
    /** ARGB colour used for chips, bars and the PDF. */
    val color: Long,
) {
    fun contains(score: Int): Boolean = score in min..max
    val rangeLabel: String get() = if (min == max) "$min" else "$min–$max"
}

/** The option actually selected for a primary item, possibly through its alternative item. */
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
    /** Short citation shown next to the tool name, e.g. "Shah, Vanclay e Cooper, 1989". */
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
) {
    /** Items that count towards the total (alternatives are reached through their primary item). */
    val primaryItems: List<ToolItem> = items.filter { it.alternativeOf == null }

    val maxScore: Int = primaryItems.sumOf { it.maxScore }

    fun item(id: String): ToolItem? = items.firstOrNull { it.id == id }

    fun alternativeFor(primaryId: String): ToolItem? = items.firstOrNull { it.alternativeOf == primaryId }

    fun bandFor(total: Int): ScoreBand? = bands.firstOrNull { it.contains(total) }

    /** Resolves the selection for a primary item: its own answer first, then its alternative's. */
    fun selectionFor(primary: ToolItem, answers: Map<String, Int>): Selection? {
        answers[primary.id]?.let { index ->
            primary.options.getOrNull(index)?.let { return Selection(primary, index, it, isAlternative = false) }
        }
        val alternative = alternativeFor(primary.id) ?: return null
        val index = answers[alternative.id] ?: return null
        val option = alternative.options.getOrNull(index) ?: return null
        return Selection(alternative, index, option, isAlternative = true)
    }

    fun selections(answers: Map<String, Int>): List<Pair<ToolItem, Selection?>> =
        primaryItems.map { it to selectionFor(it, answers) }

    /** answers: itemId -> option index. */
    fun score(answers: Map<String, Int>): ScoreResult {
        val selected = primaryItems.mapNotNull { selectionFor(it, answers) }
        val total = selected.sumOf { it.option.score }
        val complete = selected.size == primaryItems.size
        return ScoreResult(
            total = total,
            max = maxScore,
            answered = selected.size,
            required = primaryItems.size,
            band = if (complete) bandFor(total) else null,
        )
    }
}
