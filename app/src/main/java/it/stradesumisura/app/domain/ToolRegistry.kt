package it.stradesumisura.app.domain

import it.stradesumisura.app.domain.tools.BarthelModifiedIndex

/**
 * Catalogue of the tools available in the app.
 * To add a new scale: create its [ToolDefinition] under `domain/tools` and list it here.
 */
object ToolRegistry {

    val tools: List<ToolDefinition> = listOf(
        BarthelModifiedIndex.definition,
    )

    fun byId(id: String): ToolDefinition? = tools.firstOrNull { it.id == id }

    fun require(id: String): ToolDefinition =
        byId(id) ?: error("Strumento non disponibile: $id")
}
