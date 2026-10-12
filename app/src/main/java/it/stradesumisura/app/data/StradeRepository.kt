package it.stradesumisura.app.data

import it.stradesumisura.app.data.db.AppDatabase
import it.stradesumisura.app.data.db.AssessmentEntity
import it.stradesumisura.app.data.db.AssessmentWithPerson
import it.stradesumisura.app.data.db.PersonEntity
import it.stradesumisura.app.data.db.PersonWithCount
import kotlinx.coroutines.flow.Flow

/** Single entry point for all local persistence. */
class StradeRepository(private val db: AppDatabase) {

    // People -----------------------------------------------------------------

    val people: Flow<List<PersonWithCount>> = db.personDao().observeAllWithCount()

    val peopleCount: Flow<Int> = db.personDao().observeCount()

    fun observePerson(id: String): Flow<PersonEntity?> = db.personDao().observeById(id)

    suspend fun getPerson(id: String): PersonEntity? = db.personDao().getById(id)

    suspend fun savePerson(person: PersonEntity) = db.personDao().upsert(person)

    suspend fun deletePerson(id: String) = db.personDao().deleteById(id)

    // Assessments ------------------------------------------------------------

    val assessmentCount: Flow<Int> = db.assessmentDao().observeCount()

    fun observeAssessmentsOf(personId: String): Flow<List<AssessmentEntity>> =
        db.assessmentDao().observeByPerson(personId)

    fun observeRecentAssessments(limit: Int = 8): Flow<List<AssessmentWithPerson>> =
        db.assessmentDao().observeRecent(limit)

    fun observeAssessment(id: String): Flow<AssessmentWithPerson?> =
        db.assessmentDao().observeWithPerson(id)

    suspend fun getAssessment(id: String): AssessmentWithPerson? = db.assessmentDao().getWithPerson(id)

    suspend fun saveAssessment(assessment: AssessmentEntity) = db.assessmentDao().upsert(assessment)

    suspend fun deleteAssessment(id: String) = db.assessmentDao().deleteById(id)
}
