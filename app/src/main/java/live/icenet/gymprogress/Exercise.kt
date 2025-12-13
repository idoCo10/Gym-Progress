package live.icenet.gymprogress.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val machineName: String,
    val weight: Double,
    val reps: Double,
    val sets: Double
)

