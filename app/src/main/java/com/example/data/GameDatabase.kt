package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 1,
    val currentLevel: Int = 1,
    val totalDeaths: Int = 0,
    val currentLevelDeaths: Int = 0,
    val isMuted: Boolean = false,
    val playerName: String = "Player 1"
)

@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val levelsCompleted: Int,
    val totalDeaths: Int,
    val isRealPlayer: Boolean = false
)

@Dao
interface GameDao {
    @Query("SELECT * FROM user_progress WHERE id = 1")
    fun getUserProgressFlow(): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE id = 1")
    suspend fun getUserProgress(): UserProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProgress(progress: UserProgress)

    @Query("SELECT * FROM leaderboard ORDER BY levelsCompleted DESC, totalDeaths ASC")
    fun getLeaderboardFlow(): Flow<List<LeaderboardEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntry(entry: LeaderboardEntry)

    @Query("DELETE FROM leaderboard WHERE isRealPlayer = 1")
    suspend fun clearRealPlayerEntries()
}

@Database(entities = [UserProgress::class, LeaderboardEntry::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "trust_nobody_database"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate Room DB with funny default high scores and shamers
                        scope.launch(Dispatchers.IO) {
                            val dao = getDatabase(context, scope).gameDao()
                            
                            // Seed default user progress
                            dao.saveUserProgress(UserProgress())

                            // Seed funny hall of shame / fame entrants
                            val seeds = listOf(
                                LeaderboardEntry(name = "GamerGod_99", levelsCompleted = 50, totalDeaths = 14, isRealPlayer = false),
                                LeaderboardEntry(name = "KeyboardSmasher", levelsCompleted = 50, totalDeaths = 2841, isRealPlayer = false),
                                LeaderboardEntry(name = "SpikeMagnet", levelsCompleted = 35, totalDeaths = 1840, isRealPlayer = false),
                                LeaderboardEntry(name = "WreckItRalph", levelsCompleted = 24, totalDeaths = 920, isRealPlayer = false),
                                LeaderboardEntry(name = "NoobLord", levelsCompleted = 3, totalDeaths = 788, isRealPlayer = false),
                                LeaderboardEntry(name = "SpeedRunnerPro", levelsCompleted = 50, totalDeaths = 49, isRealPlayer = false),
                                LeaderboardEntry(name = "YourMom_404", levelsCompleted = 15, totalDeaths = 666, isRealPlayer = false)
                            )
                            for (s in seeds) {
                                dao.insertLeaderboardEntry(s)
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
