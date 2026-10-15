package it.stradesumisura.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** A person followed by the therapist. Everything is stored locally on the device. */
@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String,
    val firstName: String,
    val lastName: String,
    /** ISO-8601 date (yyyy-MM-dd) or null. */
    val birthDate: String? = null,
    /** "M", "F", "X" or null. */
    val sex: String? = null,
    /** Free identifier (e.g. clinical record number). */
    val code: String? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val fullName: String
        get() = "$firstName $lastName".trim()

    val initials: String
        get() = listOf(firstName, lastName)
            .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
}

/** One compiled assessment of a tool for a person. Answers are kept as a JSON map itemId -> optionIndex. */
@Entity(
    tableName = "assessments",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("personId"), Index("toolId"), Index("date")],
)
data class AssessmentEntity(
    @PrimaryKey val id: String,
    val personId: String,
    val toolId: String,
    /** ISO-8601 date of the evaluation. */
    val date: String,
    val evaluator: String? = null,
    /** Context of the evaluation (home, ward, outpatient clinic...). */
    val setting: String? = null,
    val answersJson: String,
    val totalScore: Int,
    val maxScore: Int,
    val isComplete: Boolean,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

data class AssessmentWithPerson(
    @Embedded val assessment: AssessmentEntity,
    @Relation(parentColumn = "personId", entityColumn = "id")
    val person: PersonEntity,
)

data class PersonWithCount(
    @Embedded val person: PersonEntity,
    val assessmentCount: Int,
)
