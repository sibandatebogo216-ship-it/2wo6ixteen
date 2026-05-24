package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.File

class EmulatorRepository(private val dao: EmulatorDao) {

    val controllerButtons: Flow<List<ControllerButtonConfig>> = dao.getControllerButtons()
    val emulatorConfig: Flow<EmulatorConfig?> = dao.getEmulatorConfig()
    val gameProfiles: Flow<List<GameProfile>> = dao.getGameProfiles()
    val gameStats: Flow<List<GameStats>> = dao.getGameStats()

    fun getStatsForGame(gameId: String): Flow<GameStats?> = dao.getStatsForGame(gameId)

    suspend fun insertControllerButton(button: ControllerButtonConfig) {
        dao.insertControllerButton(button)
    }

    suspend fun updateControllerButton(button: ControllerButtonConfig) {
        dao.updateControllerButton(button)
    }

    suspend fun deleteControllerButton(id: String) {
        dao.deleteControllerButton(id)
    }

    suspend fun insertEmulatorConfig(config: EmulatorConfig) {
        dao.insertEmulatorConfig(config)
    }

    suspend fun insertGameProfile(profile: GameProfile) {
        dao.insertGameProfile(profile)
    }

    suspend fun insertGameStats(stats: GameStats) {
        dao.insertGameStats(stats)
    }

    suspend fun initializeIfNeeded() {
        val config = dao.getEmulatorConfig().firstOrNull()
        if (config == null) {
            // Populate default hardware settings
            dao.insertEmulatorConfig(
                EmulatorConfig(
                    id = 1,
                    ramSizeMb = 128,
                    cpuModel = "Pentium MMX 233",
                    soundCard = "SoundBlaster 16",
                    videoMode = "SVGA 640x480",
                    crtFilterEnabled = true,
                    scanlineIntensity = 0.35f,
                    simulatedFddSpeed = 2,
                    coreVoltage = 3.28f,
                    fanSpeedRpm = 3120
                )
            )

            // Populate default game library
            val defaultGames = listOf(
                GameProfile(
                    id = "raycaster",
                    title = "Space Strike 3D",
                    description = "An immersive 3D pseudo-raycaster raycaster game in the spirit of Doom. Defend the Martian outputs against alien guards using high speed movement and strafing.",
                    genre = "3D Raycaster Shooters",
                    releaseYear = "1994",
                    difficulty = "Hard",
                    memoryRequired = 16
                ),
                GameProfile(
                    id = "commander_code",
                    title = "Commander Code",
                    description = "Classic 90s DOS-style side-scrolling platformer. Control the coding hero across binary platforms, collecting chips and bypassing bugs.",
                    genre = "2D Arcade Platformers",
                    releaseYear = "1992",
                    difficulty = "Medium",
                    memoryRequired = 8
                ),
                GameProfile(
                    id = "retro_rts",
                    title = "Retro Commander RTS",
                    description = "Real-time strategy game inspired by Command & Conquer. Construct refineries, deploy automated harvesters, command heavy armor tanks, and level the enemy tactical command center.",
                    genre = "Real-Time Strategy",
                    releaseYear = "1996",
                    difficulty = "Medium",
                    memoryRequired = 64
                )
            )
            dao.insertGameProfiles(defaultGames)

            // Insert initial default touch-button overlays
            // We use standard layout placement in relative percentage coordinates (Float 0f..1f)
            val defaultButtons = listOf(
                ControllerButtonConfig(id = "dpad_up", btnLabel = "▲", posX = 0.12f, posY = 0.65f, scale = 1.0f, opacity = 0.7f, mappedScanCode = 19), // Arrow Up (Android key code 19)
                ControllerButtonConfig(id = "dpad_left", btnLabel = "◀", posX = 0.05f, posY = 0.74f, scale = 1.0f, opacity = 0.7f, mappedScanCode = 21), // Arrow Left (Android key code 21)
                ControllerButtonConfig(id = "dpad_right", btnLabel = "▶", posX = 0.19f, posY = 0.74f, scale = 1.0f, opacity = 0.7f, mappedScanCode = 22), // Arrow Right
                ControllerButtonConfig(id = "dpad_down", btnLabel = "▼", posX = 0.12f, posY = 0.83f, scale = 1.0f, opacity = 0.7f, mappedScanCode = 20), // Arrow Down
                
                ControllerButtonConfig(id = "btn_a", btnLabel = "A", posX = 0.90f, posY = 0.72f, scale = 1.1f, opacity = 0.8f, mappedScanCode = 62), // Space (Android key code 62) - Jump/Fire
                ControllerButtonConfig(id = "btn_b", btnLabel = "B", posX = 0.80f, posY = 0.82f, scale = 1.1f, opacity = 0.8f, mappedScanCode = 66), // Enter (Android key code 66) - Interact/Select
                ControllerButtonConfig(id = "btn_x", btnLabel = "X", posX = 0.82f, posY = 0.62f, scale = 1.0f, opacity = 0.7f, mappedScanCode = 113), // Ctrl (Left) - Run/Action 2
                ControllerButtonConfig(id = "btn_y", btnLabel = "Y", posX = 0.71f, posY = 0.72f, scale = 1.0f, opacity = 0.7f, mappedScanCode = 57), // Alt (Left) - Map/Alt Fire
                
                ControllerButtonConfig(id = "btn_esc", btnLabel = "ESC", posX = 0.06f, posY = 0.14f, scale = 0.8f, opacity = 0.5f, mappedScanCode = 111), // Escape
                ControllerButtonConfig(id = "btn_tab", btnLabel = "TAB", posX = 0.06f, posY = 0.26f, scale = 0.8f, opacity = 0.5f, mappedScanCode = 61), // Tab (Map overlay)
                ControllerButtonConfig(id = "btn_space", btnLabel = "SPACE BAR", posX = 0.50f, posY = 0.88f, scale = 1.3f, opacity = 0.6f, mappedScanCode = 62) // Space bar center bottom
            )
            dao.insertControllerButtons(defaultButtons)

            // Insert initial empty stats for each game
            defaultGames.forEach { game ->
                dao.insertGameStats(
                    GameStats(
                        gameId = game.id,
                        highScore = 0,
                        timePlayedSeconds = 0,
                        lastPlayedTimestamp = 0
                    )
                )
            }
        }
    }
}
