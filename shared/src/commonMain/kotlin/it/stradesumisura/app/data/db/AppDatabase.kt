package it.stradesumisura.app.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [PersonEntity::class, AssessmentEntity::class],
    version = 1,
    exportSchema = false,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao

    abstract fun assessmentDao(): AssessmentDao

    companion object {
        /** Nome del file del database (uguale su Android e Windows). */
        const val NAME = "strade_su_misura.db"
    }
}

/** Implementazione generata da Room per ciascuna piattaforma. */
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
