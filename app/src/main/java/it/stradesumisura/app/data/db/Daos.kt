package it.stradesumisura.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Query(
        """
        SELECT people.*,
               (SELECT COUNT(*) FROM assessments WHERE assessments.personId = people.id) AS assessmentCount
        FROM people
        ORDER BY lastName COLLATE NOCASE ASC, firstName COLLATE NOCASE ASC
        """
    )
    fun observeAllWithCount(): Flow<List<PersonWithCount>>

    @Query("SELECT * FROM people WHERE id = :id")
    fun observeById(id: String): Flow<PersonEntity?>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun getById(id: String): PersonEntity?

    @Query("SELECT COUNT(*) FROM people")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun upsert(person: PersonEntity)

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface AssessmentDao {

    @Query("SELECT * FROM assessments WHERE personId = :personId ORDER BY date DESC, updatedAt DESC")
    fun observeByPerson(personId: String): Flow<List<AssessmentEntity>>

    @Transaction
    @Query("SELECT * FROM assessments ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AssessmentWithPerson>>

    @Transaction
    @Query("SELECT * FROM assessments WHERE id = :id")
    fun observeWithPerson(id: String): Flow<AssessmentWithPerson?>

    @Transaction
    @Query("SELECT * FROM assessments WHERE id = :id")
    suspend fun getWithPerson(id: String): AssessmentWithPerson?

    @Query("SELECT * FROM assessments WHERE id = :id")
    suspend fun getById(id: String): AssessmentEntity?

    @Query("SELECT COUNT(*) FROM assessments")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun upsert(assessment: AssessmentEntity)

    @Query("DELETE FROM assessments WHERE id = :id")
    suspend fun deleteById(id: String)
}
