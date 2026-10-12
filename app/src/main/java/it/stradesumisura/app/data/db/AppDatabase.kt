package it.stradesumisura.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PersonEntity::class, AssessmentEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao

    abstract fun assessmentDao(): AssessmentDao

    companion object {
        private const val NAME = "strade_su_misura.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .build()
    }
}
