package live.icenet.gymprogress.data

import androidx.room.*

@Dao
interface MachineDao {

    @Insert
    suspend fun insert(machine: Machine)

    @Update
    suspend fun update(machine: Machine)

    @Delete
    suspend fun delete(machine: Machine)

    @Query("SELECT * FROM Machine")
    suspend fun getAll(): List<Machine>
}
