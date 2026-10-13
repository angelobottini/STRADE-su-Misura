package it.stradesumisura.app.domain

import it.stradesumisura.app.domain.tools.AdlObservation
import it.stradesumisura.app.domain.tools.BarthelModifiedIndex
import it.stradesumisura.app.domain.tools.PerformanceQualityRatingScale

/**
 * Catalogo degli strumenti disponibili nell'app.
 * Per aggiungere una scala: creare la sua [ToolDefinition] in `domain/tools` ed elencarla qui.
 */
object ToolRegistry {

    val tools: List<ToolDefinition> = listOf(
        BarthelModifiedIndex.definition,
        AdlObservation.definition,
        PerformanceQualityRatingScale.definition,
    )

    fun byId(id: String): ToolDefinition? = tools.firstOrNull { it.id == id }

    fun require(id: String): ToolDefinition =
        byId(id) ?: error("Strumento non disponibile: $id")
}
