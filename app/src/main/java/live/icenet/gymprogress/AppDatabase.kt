package live.icenet.gymprogress.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Machine::class, Session::class, Exercise::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun machineDao(): MachineDao
    abstract fun sessionDao(): SessionDao
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gym_progress_db"
                )
                    .fallbackToDestructiveMigration() // Important for development
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
