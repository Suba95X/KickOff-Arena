package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

@Entity(tableName = "high_scores")
data class HighScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerName: String,
    val points: Int,
    val scoreText: String,
    val result: String, // "W", "L", "D"
    val mode: String,   // "CPU" or "P2P"
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface HighScoreDao {
    @Query("SELECT * FROM high_scores ORDER BY points DESC, timestamp DESC LIMIT 10")
    fun getTopScores(): Flow<List<HighScoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: HighScoreEntity): Long
}

@Database(entities = [HighScoreEntity::class], version = 1, exportSchema = false)
abstract class KickoffDatabase : RoomDatabase() {
    abstract fun highScoreDao(): HighScoreDao

    companion object {
        @Volatile
        private var INSTANCE: KickoffDatabase? = null

        fun getInstance(context: Context): KickoffDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KickoffDatabase::class.java,
                    "kickoff_arena.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

data class UserArenaSettings(
    val nickname: String = "Striker",
    val joystickFloating: Boolean = true,
    val autoRotateEnabled: Boolean = true,
    val cpuDifficulty: String = "MEDIUM",
    val masterVolume: Int = 80,
    val sfxVolume: Int = 80,
    val crowdLoopVolume: Int = 65,
    val whistleVolume: Int = 70,
    val muted: Boolean = false,
    val sensitivity: Float = 1.0f,
    val jerseyNumber: String = "10",
    val kitColorHex: String = "#3aa7ff"
)

class KickoffRepository(
    private val highScoreDao: HighScoreDao,
    private val prefs: SharedPreferences
) {
    val topScores: Flow<List<HighScoreEntity>> = highScoreDao.getTopScores()

    fun loadSettings(): UserArenaSettings {
        return UserArenaSettings(
            nickname = prefs.getString("nickname", "Striker") ?: "Striker",
            joystickFloating = prefs.getBoolean("joystick_floating", true),
            autoRotateEnabled = prefs.getBoolean("auto_rotate_enabled", true),
            cpuDifficulty = prefs.getString("cpu_difficulty", "MEDIUM") ?: "MEDIUM",
            masterVolume = prefs.getInt("vol_master", 80).coerceIn(0, 100),
            sfxVolume = prefs.getInt("vol_sfx", 80).coerceIn(0, 100),
            crowdLoopVolume = prefs.getInt("vol_crowd_loop", 65).coerceIn(0, 100),
            whistleVolume = prefs.getInt("vol_whistle", 70).coerceIn(0, 100),
            muted = prefs.getBoolean("muted", false),
            sensitivity = prefs.getFloat("sensitivity", 1.0f),
            jerseyNumber = prefs.getString("jersey_number", "10") ?: "10",
            kitColorHex = prefs.getString("kit_color", "#3aa7ff") ?: "#3aa7ff"
        )
    }

    fun saveSettings(settings: UserArenaSettings) {
        prefs.edit()
            .putString("nickname", settings.nickname)
            .putBoolean("joystick_floating", settings.joystickFloating)
            .putBoolean("auto_rotate_enabled", settings.autoRotateEnabled)
            .putString("cpu_difficulty", settings.cpuDifficulty)
            .putInt("vol_master", settings.masterVolume)
            .putInt("vol_sfx", settings.sfxVolume)
            .putInt("vol_crowd_loop", settings.crowdLoopVolume)
            .putInt("vol_whistle", settings.whistleVolume)
            .putBoolean("muted", settings.muted)
            .putFloat("sensitivity", settings.sensitivity)
            .putString("jersey_number", settings.jerseyNumber)
            .putString("kit_color", settings.kitColorHex)
            .apply()
    }

    suspend fun recordMatchResult(
        playerName: String,
        myGoals: Int,
        opGoals: Int,
        result: String,
        mode: String,
        pointsMultiplier: Float = 1.0f
    ): Pair<Int, Long> {
        val pts = calculatePoints(myGoals, opGoals, result, pointsMultiplier)
        val entity = HighScoreEntity(
            playerName = playerName.ifBlank { "Striker" }.take(10),
            points = pts,
            scoreText = "$myGoals-$opGoals",
            result = result,
            mode = mode
        )
        val id = highScoreDao.insertScore(entity)
        return pts to id
    }

    companion object {
        fun calculatePoints(
            my: Int,
            op: Int,
            r: String,
            multiplier: Float = 1.0f
        ): Int {
            val winBonus = when (r) {
                "W" -> 250
                "D" -> 80
                else -> 0
            }
            val cleanSheet = if (op == 0) 100 else 0
            val raw = max(0, my * 100 + winBonus + cleanSheet - op * 25)
            return (raw * multiplier).toInt()
        }
    }
}
