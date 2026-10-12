package it.stradesumisura.app.domain

import org.json.JSONObject

/**
 * Serializza le risposte di una valutazione nel JSON salvato nel database.
 * Formato: {"items": {itemId: indice}, "itemNotes": {itemId: testo}, "fields": {fieldId: testo}}.
 * Il formato legacy (oggetto piatto itemId -> indice) viene letto in modo trasparente.
 */
object AnswersCodec {

    private const val KEY_ITEMS = "items"
    private const val KEY_ITEM_NOTES = "itemNotes"
    private const val KEY_FIELDS = "fields"

    fun encode(answers: AssessmentAnswers): String {
        val json = JSONObject()
        json.put(KEY_ITEMS, JSONObject().also { obj -> answers.items.toSortedMap().forEach { (k, v) -> obj.put(k, v) } })
        json.put(KEY_ITEM_NOTES, JSONObject().also { obj -> answers.itemNotes.toSortedMap().forEach { (k, v) -> if (v.isNotBlank()) obj.put(k, v) } })
        json.put(KEY_FIELDS, JSONObject().also { obj -> answers.fields.toSortedMap().forEach { (k, v) -> if (v.isNotBlank()) obj.put(k, v) } })
        return json.toString()
    }

    fun decode(raw: String?): AssessmentAnswers {
        if (raw.isNullOrBlank()) return AssessmentAnswers()
        return runCatching {
            val json = JSONObject(raw)
            if (json.has(KEY_ITEMS)) {
                AssessmentAnswers(
                    items = intMap(json.optJSONObject(KEY_ITEMS)),
                    itemNotes = stringMap(json.optJSONObject(KEY_ITEM_NOTES)),
                    fields = stringMap(json.optJSONObject(KEY_FIELDS)),
                )
            } else {
                AssessmentAnswers(items = intMap(json))
            }
        }.getOrDefault(AssessmentAnswers())
    }

    private fun intMap(obj: JSONObject?): Map<String, Int> {
        if (obj == null) return emptyMap()
        return buildMap {
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = obj.opt(key)
                if (value is Number) put(key, value.toInt())
            }
        }
    }

    private fun stringMap(obj: JSONObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        return buildMap {
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = obj.optString(key, "")
                if (value.isNotBlank()) put(key, value)
            }
        }
    }
}
