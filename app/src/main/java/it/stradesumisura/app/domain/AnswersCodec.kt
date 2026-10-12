package it.stradesumisura.app.domain

import org.json.JSONObject

/** Serialises the answers map (itemId -> option index) to the JSON stored in the database. */
object AnswersCodec {

    fun encode(answers: Map<String, Int>): String {
        val json = JSONObject()
        answers.toSortedMap().forEach { (key, value) -> json.put(key, value) }
        return json.toString()
    }

    fun decode(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, json.getInt(key))
                }
            }
        }.getOrDefault(emptyMap())
    }
}
