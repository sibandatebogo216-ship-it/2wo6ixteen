package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "controller_buttons")
data class ControllerButtonConfig(
    @PrimaryKey val id: String,
    val btnLabel: String,
    val posX: Float, // Relative X coordinate (0.0 to 1.0)
    val posY: Float, // Relative Y coordinate (0.0 to 1.0)
    val scale: Float = 1.0f,
    val opacity: Float = 0.7f,
    val mappedScanCode: Int, // Represents the key code or a predefined virtual key category
    val isVisible: Boolean = true
)

@Entity(tableName = "emulator_settings")
data class EmulatorConfig(
    @PrimaryKey val id: Int = 1, // Single row configuration
    val ramSizeMb: Int = 128,
    val cpuModel: String = "Pentium MMX 233",
    val soundCard: String = "SoundBlaster 16",
    val videoMode: String = "SVGA 640x480",
    val crtFilterEnabled: Boolean = true,
    val scanlineIntensity: Float = 0.35f,
    val simulatedFddSpeed: Int = 2, // 1=Slow, 2=Normal, 3=Turbo
    val coreVoltage: Float = 3.3f, // Hardware monitoring
    val fanSpeedRpm: Int = 3200
)

@Entity(tableName = "game_profiles")
data class GameProfile(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val genre: String,
    val releaseYear: String,
    val difficulty: String = "Normal",
    val memoryRequired: Int = 32, // MB
    val fileCount: Int = 42,
    val sizeKb: Int = 12400
)

@Entity(tableName = "game_stats")
data class GameStats(
    @PrimaryKey val gameId: String,
    val highScore: Int = 0,
    val timePlayedSeconds: Long = 0,
    val lastPlayedTimestamp: Long = 0
)
