package live.icenet.gymprogress.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val machineName: String,
    val weight: Int,
    val reps: Int,
    val sets: Int
)
