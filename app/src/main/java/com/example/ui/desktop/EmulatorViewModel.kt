package com.example.ui.desktop

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EmulatorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = EmulatorDatabase.getDatabase(application)
    private val repository = EmulatorRepository(db.emulatorDao())

    // UI exposed configurations
    val controllerButtons: StateFlow<List<ControllerButtonConfig>> = repository.controllerButtons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emulatorConfig: StateFlow<EmulatorConfig?> = repository.emulatorConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val gameProfiles: StateFlow<List<GameProfile>> = repository.gameProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gameStats: StateFlow<List<GameStats>> = repository.gameStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined active input states (captures physical + touch controls unified key states!)
    private val _hardwareKeyStates = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val hardwareKeyStates: StateFlow<Map<Int, Boolean>> = _hardwareKeyStates.asStateFlow()

    // Last external controller inputs logs for Calibration panel
    private val _controllerInputLogs = MutableStateFlow<List<String>>(listOf("System calibrator initialised. Awaiting external inputs..."))
    val controllerInputLogs: StateFlow<List<String>> = _controllerInputLogs.asStateFlow()

    // Screen navigation layout: BIOS, DOS, DESKTOP, GAME, EDITOR
    private val _navigationState = MutableStateFlow("BIOS") // BIOS -> DOS -> DESKTOP -> GAME -> EDITOR
    val navigationState: StateFlow<String> = _navigationState.asStateFlow()

    private val _activeGameId = MutableStateFlow<String?>(null)
    val activeGameId: StateFlow<String?> = _activeGameId.asStateFlow()

    init {
        // Run database initializer asynchronously
        viewModelScope.launch {
            repository.initializeIfNeeded()
        }
    }

    // Capture key change events (for virtual overlays + physical keyboard / controller buttons)
    fun onKeyEvent(keyCode: Int, isPressed: Boolean): Boolean {
        _hardwareKeyStates.update { current ->
            val updated = current.toMutableMap()
            updated[keyCode] = isPressed
            updated
        }

        // Write custom calibration log
        val actionText = if (isPressed) "Pressed" else "Released"
        val logLine = "Input: KeyCode [$keyCode] -> $actionText (Unified mapped event)"
        addControllerLog(logLine)

        // Handled if it belongs to mapped buttons to avoid system back button intervention
        val handledCodes = listOf(19, 21, 22, 20, 62, 66, 111, 113, 57, 61, 29, 32, 47, 51)
        return handledCodes.contains(keyCode)
    }

    fun addControllerLog(message: String) {
        _controllerInputLogs.update { current ->
            (listOf(message) + current).take(22) // Keep last 22 logs
        }
    }

    fun changeNavigationState(state: String) {
        _navigationState.value = state
    }

    fun launchGame(gameId: String) {
        _activeGameId.value = gameId
        _navigationState.value = "GAME"
        addControllerLog("Launched PC game module: [$gameId]")
    }

    // DB updates - Save custom touch button coordinates
    fun updateControllerButton(button: ControllerButtonConfig) {
        viewModelScope.launch {
            repository.updateControllerButton(button)
        }
    }

    // Reset touch key settings
    fun resetControllerLayoutDefaults() {
        viewModelScope.launch {
            val defaultButtons = listOf(
                ControllerButtonConfig(id = "dpad_up", btnLabel = "▲", posX = 0.12f, posY = 0.65f, scale = 1.0f, opacity = 0.7f, mappedScanCode = 19),
                ControllerButtonConfig(id = "dpad_left", btnLabel = "◀", posX = 0.05f, posY = 0.74f, scale = 1.0f, opacity = 0.7f, mappedScanCode = 21),
                ControllerButtonConfig(id = "dpad_right", btnLabel = "▶", posX = 0.19f, posY = 0.74f, scale = 1.0f, opacity = 0.7f, mappedScanCode = 22),
                ControllerButtonConfig(id = "dpad_down", btnLabel = "▼", posX = 0.12f, posY = 0.83f, scale = 1.0f, opacity = 0.7f, mappedScanCode = 20),
                ControllerButtonConfig(id = "btn_a", btnLabel = "A", posX = 0.90f, posY = 0.72f, scale = 1.1f, opacity = 0.8f, mappedScanCode = 62),
                ControllerButtonConfig(id = "btn_b", btnLabel = "B", posX = 0.80f, posY = 0.82f, scale = 1.1f, opacity = 0.8f, mappedScanCode = 66),
                ControllerButtonConfig(id = "btn_x", btnLabel = "X", posX = 0.82f, posY = 0.62f, scale = 1.0f, opacity = 0.7f, mappedScanCode = 113),
                ControllerButtonConfig(id = "btn_y", btnLabel = "Y", posX = 0.71f, posY = 0.72f, scale = 1.0f, opacity = 0.7f, mappedScanCode = 57),
                ControllerButtonConfig(id = "btn_esc", btnLabel = "ESC", posX = 0.06f, posY = 0.14f, scale = 0.8f, opacity = 0.5f, mappedScanCode = 111),
                ControllerButtonConfig(id = "btn_tab", btnLabel = "TAB", posX = 0.06f, posY = 0.26f, scale = 0.8f, opacity = 0.5f, mappedScanCode = 61),
                ControllerButtonConfig(id = "btn_space", btnLabel = "SPACE BAR", posX = 0.50f, posY = 0.88f, scale = 1.3f, opacity = 0.6f, mappedScanCode = 62)
            )
            defaultButtons.forEach { btn ->
                repository.updateControllerButton(btn)
            }
        }
    }

    // Update system settings (Crt filter, ram size allocations, virtual clocks)
    fun saveHardwareSettings(config: EmulatorConfig) {
        viewModelScope.launch {
            repository.insertEmulatorConfig(config)
        }
    }

    // Save game highScore stats securely
    fun saveGameScore(gameId: String, score: Int) {
        viewModelScope.launch {
            val statsFlow = repository.getStatsForGame(gameId)
            val currentStats = statsFlow.firstOrNull() ?: GameStats(gameId)
            if (score > currentStats.highScore) {
                repository.insertGameStats(
                    currentStats.copy(
                        highScore = score,
                        lastPlayedTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
