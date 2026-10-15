package it.stradesumisura.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Profile of the evaluator, printed in the PDF header and signature block. */
data class EvaluatorProfile(
    val name: String = "",
    val qualification: String = "Terapista occupazionale",
    val organization: String = "",
    val place: String = "",
    val includeSignature: Boolean = true,
)

class SettingsStore(private val prefs: KeyValueStore) {

    private val _profile = MutableStateFlow(read())
    val profile: StateFlow<EvaluatorProfile> = _profile.asStateFlow()

    fun save(profile: EvaluatorProfile) {
        prefs.putAll(
            mapOf(
                KEY_NAME to profile.name,
                KEY_QUALIFICATION to profile.qualification,
                KEY_ORGANIZATION to profile.organization,
                KEY_PLACE to profile.place,
                KEY_SIGNATURE to profile.includeSignature,
            )
        )
        _profile.value = profile
    }

    private fun read(): EvaluatorProfile = EvaluatorProfile(
        name = prefs.getString(KEY_NAME, ""),
        qualification = prefs.getString(KEY_QUALIFICATION, "Terapista occupazionale"),
        organization = prefs.getString(KEY_ORGANIZATION, ""),
        place = prefs.getString(KEY_PLACE, ""),
        includeSignature = prefs.getBoolean(KEY_SIGNATURE, true),
    )

    private companion object {
        const val KEY_NAME = "evaluator_name"
        const val KEY_QUALIFICATION = "evaluator_qualification"
        const val KEY_ORGANIZATION = "organization"
        const val KEY_PLACE = "place"
        const val KEY_SIGNATURE = "include_signature"
    }
}
