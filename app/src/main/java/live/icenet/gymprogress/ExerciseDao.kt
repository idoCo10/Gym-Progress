package live.icenet.gymprogress.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ExerciseDao {
    @Insert
    suspend fun insert(exercise: Exercise)

    @Query("SELECT * FROM Exercise WHERE sessionId = :sessionId")
    suspend fun getBySession(sessionId: Int): List<Exercise>
}
