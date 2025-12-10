package live.icenet.gymprogress.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: Session): Long

    @Query("SELECT * FROM Session")
    suspend fun getAll(): List<Session>

    @Delete
    suspend fun delete(session: Session)
}
