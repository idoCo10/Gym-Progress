package live.icenet.gymprogress.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MachineDao {
    @Insert
    suspend fun insert(machine: Machine)

    @Query("SELECT * FROM Machine")
    suspend fun getAll(): List<Machine>
}
