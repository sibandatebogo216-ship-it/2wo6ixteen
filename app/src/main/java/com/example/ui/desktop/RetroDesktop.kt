package com.example.ui.desktop

import android.os.SystemClock
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EmulatorConfig
import com.example.data.GameProfile
import com.example.data.GameStats
import com.example.ui.games.EmulatorKeyStates
import com.example.ui.games.RunRetroGame
import com.example.ui.editor.EditControllerLayoutScreen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

@Composable
fun RetroEmulatorConsoleDashboard(
    viewModel: EmulatorViewModel,
    modifier: Modifier = Modifier
) {
    val navState by viewModel.navigationState.collectAsState()
    val config by viewModel.emulatorConfig.collectAsState()
    val crtEnabled = config?.crtFilterEnabled ?: true

    Box(modifier = modifier.fillMaxSize()) {
        when (navState) {
            "BIOS" -> BiosBootScreen(onBootComplete = { viewModel.changeNavigationState("DOS") })
            "DOS" -> CliDosScreen(
                onVer = { viewModel.changeNavigationState("WIN") },
                onLaunchGame = { gameId -> viewModel.launchGame(gameId) }
            )
            "WIN" -> Windows98DesktopScreen(viewModel)
            "GAME" -> ActiveGameSessionView(viewModel)
            else -> {}
        }

        // Apply Global retro CRT aesthetic mesh overlay if active
        if (crtEnabled && navState != "GAME") {
            CrtPhosphorMeshEffect(intensity = config?.scanlineIntensity ?: 0.3f)
        }
    }
}

// ==========================================
// 1. BIOS VINTAGE BOOT SEQUENCE
// ==========================================
@Composable
fun BiosBootScreen(onBootComplete: () -> Unit) {
    var bootTextLines = remember { mutableStateListOf<String>() }
    var bootProgress by remember { mutableStateOf(0.0f) }

    LaunchedEffect(Unit) {
        val lines = listOf(
            "AMIBIOS (C) 1995-1998 Megatrends Inc.",
            "CPU: Intel Pentium MMX 233 MHz clocked at FSB 66MHz",
            "Speed Multiplier: 3.5x  -- FSB Frequency: 66 MHz",
            "System L1 Cache: 32KB  -- L2 Cache: 512KB Write-Back",
            "TESTING VOLTAGE PARAMETERS: Core VCC 3.28V [OK]",
            "TESTING MEMORY CHIPS: ",
            "32768KB OK",
            "65536KB OK",
            "131072KB RAM OK",
            "A: FLOPPY DRIVE MOUNTED (3.5\" MS-FDD)",
            "C: HARD DISK SEGMENT AT 0x3F6 IDE-0 PRIMARY CH 0 MASTER",
            "   SECTOR MAPPING ACTIVE -- BLOCK SIZE: 512B",
            "Detecting peripheral controllers...",
            "Standard keyboard/mouse connected at IRQ 1",
            "Capturing Gameport Controller interface. Controller mapping setup: OK",
            "SoundBlaster 16 Driver initialised successfully. Port 220h",
            "Executing MSCDEX CD-ROM Extensions... Version 2.23",
            "C:\\> AUTOEXEC.BAT executing..."
        )

        for (item in lines) {
            bootTextLines.add(item)
            bootProgress = bootTextLines.size.toFloat() / lines.size.toFloat()
            delay(150 + (0..200).random().toLong())
        }
        delay(600)
        onBootComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0A09))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Energetic AMIBIOS brand header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Megatrends BIOS Setup v2.95",
                        color = Color(0xFFF97316),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "MAY 1998",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    bootTextLines.forEach { line ->
                        Text(
                            text = "  $line",
                            color = Color(0xFFE7E5E4),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Progress Indicators
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LinearProgressIndicator(
                    progress = { bootProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color(0xFFEAB308),
                    trackColor = Color(0xFF292524)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BOOT SYSTEMS MOUNTING... ${(bootProgress * 100).toInt()}%",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Button(
                        onClick = onBootComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("FAST BOOT ⚡", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. RETRO MS-DOS EXECUTABLE CLI TERMINAL
// ==========================================
@Composable
fun CliDosScreen(
    onVer: () -> Unit,
    onLaunchGame: (String) -> Unit
) {
    var consoleHistory = remember {
        mutableStateListOf(
            "AMIBIOS Setup Successful.",
            "Starting MS-DOS 6.22...",
            "",
            "Microsoft(R) MS-DOS(R) Version 6.22",
            "  (C)Copyright Microsoft Corp 1981-1994.",
            "",
            "C:\\> Type 'help' to see list of retro games & OS commands.",
            "C:\\> "
        )
    }

    var textInput by remember { mutableStateOf("") }
    val listState = rememberScrollState()

    fun runCommand(cmd: String) {
        val cleanCmd = cmd.trim().lowercase()
        consoleHistory.add("C:\\> $cmd")

        when {
            cleanCmd == "help" -> {
                consoleHistory.add("Available commands:")
                consoleHistory.add("  dir            - List emulated directory items")
                consoleHistory.add("  cd games       - Enter Interactive Game folder and list launch commands")
                consoleHistory.add("  system / ver   - Check simulated PC stats / details")
                consoleHistory.add("  win / win98    - BOOT into Vintage Windows 98 desktop system")
                consoleHistory.add("  reboot         - Execute System reset BIOS sequency")
                consoleHistory.add("  cls            - Clean terminal screen logs")
            }
            cleanCmd == "dir" -> {
                consoleHistory.add(" Volume in drive C has no label.")
                consoleHistory.add(" Directory of C:\\")
                consoleHistory.add("")
                consoleHistory.add("GAMES          <DIR>        05-24-98   8:42a")
                consoleHistory.add("COMMANDS BAT          412   05-24-98   8:42a")
                consoleHistory.add("MEMTEST  EXE        51200   05-24-98   8:42a")
                consoleHistory.add("BIOS     INI          184   05-24-98   8:42a")
                consoleHistory.add("WIN98    <DIR>        05-24-98   8:51a")
                consoleHistory.add("        3 File(s)          51,796 bytes")
                consoleHistory.add("        2 Dir(s)      42,842,112 bytes free")
            }
            cleanCmd == "cd games" || cleanCmd == "games" -> {
                consoleHistory.add("C:\\GAMES>")
                consoleHistory.add("  Space Strike 3D         -> Type 'run raycaster'")
                consoleHistory.add("  Commander Code Platform -> Type 'run commander'")
                consoleHistory.add("  Retro RTS Tactics       -> Type 'run rts'")
            }
            cleanCmd == "system" || cleanCmd == "ver" -> {
                consoleHistory.add("SYSTEM MONITERS DETAILS:")
                consoleHistory.add("  Emulated OS: RetroDOS 6.22 / Windows 98 SE Core")
                consoleHistory.add("  Chipset Architecture: x86-Pentium MMX (66MHz)")
                consoleHistory.add("  Virtual Video controller: SVGA ISA 16-Bit Card")
                consoleHistory.add("  Sound Synthesizer: AdLib Gold / ISA SoundBlaster 16")
            }
            cleanCmd == "win" || cleanCmd == "win98" || cleanCmd == "win95" -> {
                consoleHistory.add("Loading GUI shell structures... Starting Windows 98...")
                onVer()
            }
            cleanCmd == "reboot" -> {
                consoleHistory.clear()
                consoleHistory.add("System reset requested...")
                consoleHistory.add("C:\\> ")
            }
            cleanCmd == "cls" -> {
                consoleHistory.clear()
                consoleHistory.add("C:\\> ")
            }
            cleanCmd.startsWith("run ") -> {
                val gameToRun = cleanCmd.replace("run ", "").trim()
                if (gameToRun == "raycaster") {
                    consoleHistory.add("Launching Space Strike 3D...")
                    onLaunchGame("raycaster")
                } else if (gameToRun == "commander") {
                    consoleHistory.add("Launching Commander Code...")
                    onLaunchGame("commander_code")
                } else if (gameToRun == "rts") {
                    consoleHistory.add("Launching Retro RTS Tactics...")
                    onLaunchGame("retro_rts")
                } else {
                    consoleHistory.add("Game ROM '$gameToRun' not found inside /C:/GAMES/ partition.")
                }
            }
            cleanCmd == "run" -> {
                consoleHistory.add("Usage: run [raycaster | commander | rts]")
            }
            else -> {
                consoleHistory.add("Bad command or file name: '$cmd'")
            }
        }
        consoleHistory.add("C:\\> ")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF022C22)) // Authentic toxic green tinted terminal
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF047857))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("C:\\DOS\\PROMPT", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text("x86 EMULATOR MODE", color = Color(0xFF6EE7B7), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrolling view logs
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(listState)
            ) {
                consoleHistory.forEach { line ->
                    Text(
                        text = line,
                        color = Color(0xFF34D399),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "C:\\> $textInput",
                        color = Color(34, 211, 238),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp, 16.dp)
                            .background(Color(34, 211, 238))
                    )
                }
            }

            // Quick Touch commands keyboard triggers for effortless user interactions!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("QUICK SHELL COMMANDS PRESETS (TAP TO EXECUTE):", color = Color(0xFF6EE7B7), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("help", "dir", "cd games", "system", "cls").forEach { cmdName ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF065F46), shape = RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF34D399), shape = RoundedCornerShape(4.dp))
                                .clickable { runCommand(cmdName) }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cmdName, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val gameTriggers = listOf(
                        Pair("Play Space 3D 🛸", "run raycaster"),
                        Pair("Play Commander 🧗", "run commander"),
                        Pair("Play RTS ⚔️", "run rts"),
                        Pair("START WIN98 🖥️", "win98")
                    )
                    gameTriggers.forEach { (label, command) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF047857), shape = RoundedCornerShape(4.dp))
                                .border(1.dp, Color(34, 211, 238), shape = RoundedCornerShape(4.dp))
                                .clickable { runCommand(command) }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. WINDOWS 98 RETRO DESKTOP GUI SYSTEM
// ==========================================
@Composable
fun Windows98DesktopScreen(viewModel: EmulatorViewModel) {
    var openWindowsMap = remember { mutableStateMapOf<String, Boolean>() }
    var activeStartMenu by remember { mutableStateOf(false) }

    val config by viewModel.emulatorConfig.collectAsState()
    val games by viewModel.gameProfiles.collectAsState()
    val stats by viewModel.gameStats.collectAsState()
    val logs by viewModel.controllerInputLogs.collectAsState()
    val rawKeyStates by viewModel.hardwareKeyStates.collectAsState()

    // Setup initial open configurations
    LaunchedEffect(Unit) {
        openWindowsMap["my_computer"] = true // Active on first boot
        openWindowsMap["game_explorer"] = false
        openWindowsMap["calibrator"] = false
        openWindowsMap["settings"] = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF008080)) // Standard Windows 95/98 Teal Background color
    ) {
        // Desktop icons Grid aligned to left
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DesktopShortcutWidget(
                label = "My Computer",
                iconResId = Icons.Default.Info,
                onClick = { openWindowsMap["my_computer"] = true }
            )
            DesktopShortcutWidget(
                label = "Game Explorer",
                iconResId = Icons.Default.PlayArrow,
                onClick = { openWindowsMap["game_explorer"] = true }
            )
            DesktopShortcutWidget(
                label = "Controller Calibration",
                iconResId = Icons.Default.Build,
                onClick = { openWindowsMap["calibrator"] = true }
            )
            DesktopShortcutWidget(
                label = "Emulator Settings",
                iconResId = Icons.Default.Settings,
                onClick = { openWindowsMap["settings"] = true }
            )
        }

        // 1. My Computer Window (System specs & monitors)
        if (openWindowsMap["my_computer"] == true) {
            Win98WindowWidget(
                title = "My Computer - Systems parameters",
                onClose = { openWindowsMap["my_computer"] = false },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.72f)
            ) {
                MyComputerSystemInfoPanel(config ?: EmulatorConfig())
            }
        }

        // 2. Game Explorer Window
        if (openWindowsMap["game_explorer"] == true) {
            Win98WindowWidget(
                title = "PC Game ROM Executables Explorer",
                onClose = { openWindowsMap["game_explorer"] = false },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 22.dp)
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.80f)
            ) {
                GameExplorerPanel(
                    games = games,
                    stats = stats,
                    onLaunch = { gameId -> viewModel.launchGame(gameId) }
                )
            }
        }

        // 3. Controller Calibration & Logs Window
        if (openWindowsMap["calibrator"] == true) {
            Win98WindowWidget(
                title = "Physical controller calibration & Log inputs",
                onClose = { openWindowsMap["calibrator"] = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.75f)
            ) {
                ControllerCalibrationPanel(
                    logs = logs,
                    rawKeyStates = rawKeyStates,
                    onRegisterSimulatorInput = { viewModel.addControllerLog("Simulated action: Button trigger.") }
                )
            }
        }

        // 4. Emulator Settings Window
        if (openWindowsMap["settings"] == true) {
            Win98WindowWidget(
                title = "PC Hardware Emulation Settings BIOS",
                onClose = { openWindowsMap["settings"] = false },
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(0.75f)
            ) {
                EmulatorSettingsFormPanel(
                    currentConfig = config ?: EmulatorConfig(),
                    onSave = { updated -> viewModel.saveHardwareSettings(updated) }
                )
            }
        }

        // Dynamic Windows 98 bottom Start taskbar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFFC0C0C0)) // Vintage grey look
                .border(2.dp, Color.White, shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .padding(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Start Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (activeStartMenu) Color(0xFF808080) else Color(0xFFC0C0C0),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .border(2.dp, if (activeStartMenu) Color.Black else Color.White)
                            .clickable { activeStartMenu = !activeStartMenu }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Windows logo", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Simple running app indicators
                    listOf("my_computer", "game_explorer", "calibrator", "settings").forEach { appKey ->
                        if (openWindowsMap[appKey] == true) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFB0B0B0), shape = RoundedCornerShape(2.dp))
                                    .border(1.dp, Color.White)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = appKey.replace("_", " ").uppercase(),
                                    fontSize = 9.sp,
                                    color = Color.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }

                // Right side: Clock info + simulated icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFA0A0A0))
                        .border(1.dp, Color.White)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = "Volume", tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    val systemTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    Text(text = systemTime, color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Popup Windows Start Drawer List
        if (activeStartMenu) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 4.dp, y = (-40).dp)
                    .width(180.dp)
                    .background(Color(0xFFC0C0C0))
                    .border(2.dp, Color.White)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color(0xFF000080), Color(0xFF1084D0))))
                            .padding(8.dp)
                    ) {
                        Text("Windows98 SE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    listOf(
                        Pair("Games App Explorer Launcher", "game_explorer"),
                        Pair("Hardware Specs Properties", "my_computer"),
                        Pair("External Pad Calibration", "calibrator"),
                        Pair("Emulator BIOS Form Setup", "settings")
                    ).forEach { (display, target) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    openWindowsMap[target] = true
                                    activeStartMenu = false
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Launch Icon", tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(display, color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Divider(color = Color.Gray)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.changeNavigationState("DOS")
                                activeStartMenu = false
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Build, contentDescription = "DOS prompt", tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exit to MS-DOS CLI", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// ==========================================
// DESKTOP INTERFACE WINDOW REUSABLE SHELL
// ==========================================
@Composable
fun Win98WindowWidget(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(Color(0xFFC0C0C0)) // Retro windows frame gray color
            .border(2.dp, Color.White)
            .border(4.dp, Color(0xFFC0C0C0))
            .border(5.dp, Color.Black)
            .clip(RoundedCornerShape(2.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Authentic title Bar Blue Gradient Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF000080), Color(0xFF1084D0))))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Classic Close Icon Button
                Box(
                    modifier = Modifier
                        .background(Color(0xFFC0C0C0))
                        .border(1.dp, Color.White)
                        .clickable { onClose() }
                        .size(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close window", tint = Color.Black, modifier = Modifier.size(12.dp))
                }
            }

            // Window Content View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFE5E5E5)) // light inner dashboard gray
                    .padding(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun DesktopShortcutWidget(
    label: String,
    iconResId: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
            .width(82.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                .size(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = iconResId, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            lineHeight = 12.sp
        )
    }
}

// ==========================================
// MY COMPUTER SYSTEM SPEC PANEL DIAGRAMS
// ==========================================
@Composable
fun MyComputerSystemInfoPanel(config: EmulatorConfig) {
    var rotationAngle by remember { mutableStateOf(0f) }

    // Spin fan motor infinitely
    val infiniteTransition = rememberInfiniteTransition(label = "FanRotation")
    val angleSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FanSpin"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SYSTEM EMULATION PROPERTIES",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontFamily = FontFamily.Monospace
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Visual Motherboard components diagram using canvas
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cw = size.width
                        val ch = size.height
                        
                        // Green Motherboard PCB board
                        drawRect(color = Color(0xFF155E75))
                        
                        // Gold traces
                        drawLine(Color(0xFFEAB308), Offset(10f, 10f), Offset(cw - 20f, 10f), strokeWidth = 1f)
                        drawLine(Color(0xFFEAB308), Offset(10f, 10f), Offset(10f, ch - 20f), strokeWidth = 1f)
                        drawLine(Color(0xFFEAB308), Offset(30f, ch / 2), Offset(cw - 40f, ch / 2), strokeWidth = 2f)

                        // CPU Socket Block Box
                        drawRect(
                            color = Color(0xFF334155),
                            topLeft = Offset(cw / 2f - 30f, ch / 2f - 30f),
                            size = Size(60f, 60f)
                        )
                        // Active cores silicon write
                        drawCircle(
                            color = Color.Cyan,
                            radius = 12f,
                            center = Offset(cw / 2f, ch / 2f)
                        )
                    }
                    Text("PENTIUM CHIPSET", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // CPU Fan spinning widget to prove system execution!
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Fan",
                        tint = Color.Cyan,
                        modifier = Modifier
                            .size(54.dp)
                            .rotate(angleSpin)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("ACTIVE COOLING FAN", color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("${config.fanSpeedRpm} RPM", color = Color.Green, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Hard system reports summary parameters
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.Gray)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SPECIFICATIONS LOGS:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                Divider()
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Emulated CPU MicroProcessor:", fontSize = 11.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                    Text(config.cpuModel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Allocated Memory Allocation:", fontSize = 11.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                    Text("${config.ramSizeMb} MB RAM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Digital Soundcard synthesis:", fontSize = 11.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                    Text(config.soundCard, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Graphics adapter standard:", fontSize = 11.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                    Text(config.videoMode, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Core Voltage Monitoring:", fontSize = 11.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                    Text("${config.coreVoltage} V", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// ==========================================
// GAME ROMS EXPLORER PANEL LISTS
// ==========================================
@Composable
fun GameExplorerPanel(
    games: List<GameProfile>,
    stats: List<GameStats>,
    onLaunch: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "DOUBLE-TAP SHORTCUT TO LAUNCH",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            games.forEach { game ->
                val gameStat = stats.firstOrNull { it.gameId == game.id }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Gray, shape = RoundedCornerShape(4.dp))
                        .clickable { onLaunch(game.id) },
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF3B82F6), shape = CircleShape)
                                        .size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = game.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(text = game.description, fontSize = 11.sp, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Genre: ${game.genre}", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Text("Year: ${game.releaseYear}", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Text("Memory Req: ${game.memoryRequired}MB", fontSize = 9.sp, color = Color.Red, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "HI-SCORE",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${gameStat?.highScore ?: 0}",
                                fontSize = 16.sp,
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { onLaunch(game.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text("RUN GAME", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// CONTROLLER CALIBRATION & HARD INPUT LOGS
// ==========================================
@Composable
fun ControllerCalibrationPanel(
    logs: List<String>,
    rawKeyStates: Map<Int, Boolean>,
    onRegisterSimulatorInput: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOG ENGINE MONITOR",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontFamily = FontFamily.Monospace
            )
            
            // Connection visual badge
            Box(
                modifier = Modifier
                    .background(Color(0xFF0F766E), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("EXTERNAL CONTROLLER LISTENING...", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Visual mapped gamepad widget to prove controller connectivity visual responses
            Card(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        val cw = size.width
                        val ch = size.height

                        // Draw classic gamepad bone skeleton
                        drawRoundRect(
                            color = Color(0xFF475569),
                            topLeft = Offset(cw * 0.15f, ch * 0.3f),
                            size = Size(cw * 0.7f, ch * 0.4f),
                            cornerRadius = CornerRadius(24f, 24f)
                        )

                        // Highlight Dpad Up (Andoid key 19)
                        val isUp = rawKeyStates[19] == true
                        drawCircle(
                            color = if (isUp) Color.Yellow else Color.DarkGray,
                            radius = 16f,
                            center = Offset(cw * 0.3f, ch * 0.42f)
                        )

                        // Highlight Dpad Left (Android key 21)
                        val isLeft = rawKeyStates[21] == true
                        drawCircle(
                            color = if (isLeft) Color.Yellow else Color.DarkGray,
                            radius = 16f,
                            center = Offset(cw * 0.23f, ch * 0.5f)
                        )

                        // Highlight Dp Right (Android key 22)
                        val isRight = rawKeyStates[22] == true
                        drawCircle(
                            color = if (isRight) Color.Yellow else Color.DarkGray,
                            radius = 16f,
                            center = Offset(cw * 0.37f, ch * 0.5f)
                        )

                        // Highlight Dp Down (Android key 20)
                        val isDown = rawKeyStates[20] == true
                        drawCircle(
                            color = if (isDown) Color.Yellow else Color.DarkGray,
                            radius = 16f,
                            center = Offset(cw * 0.3f, ch * 0.58f)
                        )

                        // Action button A (Android Key 62)
                        val isA = rawKeyStates[62] == true
                        drawCircle(
                            color = if (isA) Color.Green else Color(0xFFB91C1C),
                            radius = 18f,
                            center = Offset(cw * 0.74f, ch * 0.54f)
                        )

                        // Action button B (Android Key 66)
                        val isB = rawKeyStates[66] == true
                        drawCircle(
                            color = if (isB) Color.Green else Color(0xFF1E3A8A),
                            radius = 18f,
                            center = Offset(cw * 0.66f, ch * 0.44f)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("VISUAL GAMEPAD MAP", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("Press external hardware button", color = Color.Green, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Real-time Scrolling Input events Logger
            Card(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    logs.forEach { logLine ->
                        Text(
                            text = logLine,
                            color = if (logLine.contains("Event") || logLine.contains("Input")) Color.Yellow else Color(0xFF22C55E),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Divider(color = Color.White.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}

// ==========================================
// HARDWARE EMULATION CONFIG SETUP FORM
// ==========================================
@Composable
fun EmulatorSettingsFormPanel(
    currentConfig: EmulatorConfig,
    onSave: (EmulatorConfig) -> Unit
) {
    var editRam by remember { mutableStateOf(currentConfig.ramSizeMb) }
    var editCpu by remember { mutableStateOf(currentConfig.cpuModel) }
    var editSound by remember { mutableStateOf(currentConfig.soundCard) }
    var editCrt by remember { mutableStateOf(currentConfig.crtFilterEnabled) }
    var editIntensity by remember { mutableStateOf(currentConfig.scanlineIntensity) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("EMULATED BIOS HARWARE SETUPS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black, fontFamily = FontFamily.Monospace)

        // Config RAM slider
        Column {
            Text("Simulated Allocated RAM size: ${editRam}MB RAM", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Slider(
                value = editRam.toFloat(),
                onValueChange = { editRam = it.toInt() },
                valueRange = 16f..256f,
                steps = 7,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Config CPU speed properties selection
        Column {
            Text("CPU Intel Core Model Speed:", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            listOf("Pentium 133MHz", "Pentium MMX 233", "Pentium II 400MHz OverDrive").forEach { model ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { editCpu = model }
                ) {
                    RadioButton(selected = (editCpu == model), onClick = { editCpu = model })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(model, color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Config Sound Synthesis
        Column {
            Text("BIOS Sound Chip Synthesizer Synthe:", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            listOf("SoundBlaster 16", "AdLib Gold OPL3", "Gravis Ultrasound").forEach { sound ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { editSound = sound }
                ) {
                    RadioButton(selected = (editSound == sound), onClick = { editSound = sound })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(sound, color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Toggle Retro CRT Overlay
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(checked = editCrt, onCheckedChange = { editCrt = it })
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Enable Retro CRT Scanlines Display Filter", fontSize = 12.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                Text("Applies CRT phosphorous line arrays matching authentic vintage VGA monitors", fontSize = 10.sp, color = Color.DarkGray)
            }
        }

        if (editCrt) {
            Column {
                Text("Scanline Opacity Intensity: ${(editIntensity * 100).toInt()}%", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Slider(
                    value = editIntensity,
                    onValueChange = { editIntensity = it },
                    valueRange = 0.1f..0.8f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = {
                onSave(
                    currentConfig.copy(
                        ramSizeMb = editRam,
                        cpuModel = editCpu,
                        soundCard = editSound,
                        crtFilterEnabled = editCrt,
                        scanlineIntensity = editIntensity
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SAVE & UPDATE SYSTEM CONFIGURATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
        }
    }
}

// ==========================================
// VINTAGE RETRO MONITOR SHADER/FILTER
// ==========================================
@Composable
fun CrtPhosphorMeshEffect(intensity: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val h = size.height
        val w = size.width

        // Mesh scanlines drawing
        for (y in 0 until h.toInt() step 5) {
            drawLine(
                color = Color.Black.copy(alpha = intensity * 0.65f),
                start = Offset(0f, y.toFloat()),
                end = Offset(w, y.toFloat()),
                strokeWidth = 1.2f
            )
        }

        // Vignette shadows around screen rim creating a tube curvature feel
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)),
                center = Offset(w / 2f, h / 2f),
                radius = w * 0.72f
            )
        )
    }
}

// ==========================================
// ACTIVE PLAYING SESSION VIEW OVERLAY WITH GAMEPADS
// ==========================================
@Composable
fun ActiveGameSessionView(viewModel: EmulatorViewModel) {
    val activeGameId by viewModel.activeGameId.collectAsState()
    val controllerButtons by viewModel.controllerButtons.collectAsState()
    val hardwareKeys by viewModel.hardwareKeyStates.collectAsState()

    // Map unified raw state outputs
    val mappedKeyStates = remember(hardwareKeys) {
        derivedStateOf {
            // Evaluates if key triggers are active via touch button clicks or raw physical key codes received!
            val upActive = hardwareKeys[19] == true || hardwareKeys[51] == true // Arrow up or W
            val leftActive = hardwareKeys[21] == true || hardwareKeys[29] == true // Arrow left or A
            val rightActive = hardwareKeys[22] == true || hardwareKeys[32] == true // Arrow right or D
            val downActive = hardwareKeys[20] == true || hardwareKeys[47] == true // Arrow down or S
            val actAActive = hardwareKeys[62] == true // Space
            val actBActive = hardwareKeys[66] == true // Enter
            val actXActive = hardwareKeys[113] == true // Left-Control
            val actYActive = hardwareKeys[57] == true // Left-Alt

            EmulatorKeyStates(
                up = upActive,
                down = downActive,
                left = leftActive,
                right = rightActive,
                actA = actAActive,
                actB = actBActive,
                actX = actXActive,
                actY = actYActive
            )
        }
    }

    // Toggle custom Editor Overlay layout
    var isOverlayEditingMode by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        if (widthPx == 0f || heightPx == 0f) return@BoxWithConstraints

        // Render Game canvas
        RunRetroGame(
            gameId = activeGameId ?: "raycaster",
            keyStates = mappedKeyStates.value,
            onScoreSaved = { points -> viewModel.saveGameScore(activeGameId ?: "raycaster", points) },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Overlay buttons (Normal user playing mode)
        if (!isOverlayEditingMode) {
            controllerButtons.forEach { btn ->
                val px = btn.posX * widthPx
                val py = btn.posY * heightPx

                Box(
                    modifier = Modifier
                        .offset { IntOffset(px.roundToInt(), py.roundToInt()) }
                        .size((64 * btn.scale).dp)
                        .alpha(btn.opacity)
                        .background(Color.DarkGray.copy(alpha = 0.5f), shape = CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), shape = CircleShape)
                        .pointerInput(btn.id) {
                            // Touch trigger Down & Up key handlers
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    // Touch presses down
                                    if (event.changes.any { it.pressed }) {
                                        viewModel.onKeyEvent(btn.mappedScanCode, true)
                                    }
                                    // Touch lifts up
                                    if (event.changes.any { !it.pressed }) {
                                        viewModel.onKeyEvent(btn.mappedScanCode, false)
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = btn.btnLabel,
                        color = Color.White,
                        fontSize = (15 * btn.scale).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Small Floating Session configurations header
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1: Toggle Custom Key editor!
                Button(
                    onClick = { isOverlayEditingMode = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit gamepad", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EDIT TOUCH KEYPADS", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                }

                // Button 2: Return back safely to retro DesktopOS window
                Button(
                    onClick = { viewModel.changeNavigationState("WIN") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Exit to OS", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EXIT SESSION TO OS", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                }
            }
        } else {
            // Edit Overlay keypad positions mode screen!
            EditControllerLayoutScreen(
                currentButtons = controllerButtons,
                onSaveButton = { changes -> viewModel.updateControllerButton(changes) },
                onResetDefaults = { viewModel.resetControllerLayoutDefaults() },
                onCloseEditor = { isOverlayEditingMode = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
