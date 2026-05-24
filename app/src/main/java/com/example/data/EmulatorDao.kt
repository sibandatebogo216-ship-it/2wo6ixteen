package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EmulatorDao {

    // Controller configuration
    @Query("SELECT * FROM controller_buttons")
    fun getControllerButtons(): Flow<List<ControllerButtonConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertControllerButton(button: ControllerButtonConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertControllerButtons(buttons: List<ControllerButtonConfig>)

    @Update
    suspend fun updateControllerButton(button: ControllerButtonConfig)

    @Query("DELETE FROM controller_buttons WHERE id = :id")
    suspend fun deleteControllerButton(id: String)

    // Emulator system settings
    @Query("SELECT * FROM emulator_settings WHERE id = 1")
    fun getEmulatorConfig(): Flow<EmulatorConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmulatorConfig(config: EmulatorConfig)

    // Game profiles
    @Query("SELECT * FROM game_profiles")
    fun getGameProfiles(): Flow<List<GameProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameProfile(gameProfile: GameProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameProfiles(profiles: List<GameProfile>)

    // Game stats (high score, play time)
    @Query("SELECT * FROM game_stats")
    fun getGameStats(): Flow<List<GameStats>>

    @Query("SELECT * FROM game_stats WHERE gameId = :gameId")
    fun getStatsForGame(gameId: String): Flow<GameStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameStats(stats: GameStats)
}
