package com.example.ui.games

import android.os.SystemClock
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

// Direct Key Inputs
data class EmulatorKeyStates(
    val up: Boolean = false,
    val down: Boolean = false,
    val left: Boolean = false,
    val right: Boolean = false,
    val actA: Boolean = false, // Space / Fire
    val actB: Boolean = false, // Enter / Action
    val actX: Boolean = false, // Ctrl / Run
    val actY: Boolean = false  // Alt / Inventory
)

@Composable
fun RunRetroGame(
    gameId: String,
    keyStates: EmulatorKeyStates,
    onScoreSaved: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (gameId) {
            "raycaster" -> RaycasterGameScreen(keyStates, onScoreSaved)
            "commander_code" -> PlatformerGameScreen(keyStates, onScoreSaved)
            "retro_rts" -> RtsGameScreen(keyStates, onScoreSaved)
            else -> {
                Text("Error: Unknown game ROM structure", color = Color.Red, fontSize = 18.sp)
            }
        }
    }
}

// ==========================================
// 1. SPACE STRIKE 3D (RAYCASTER ENGINE)
// ==========================================

@Composable
fun RaycasterGameScreen(
    keyStates: EmulatorKeyStates,
    onScoreSaved: (Int) -> Unit
) {
    // Player position
    var posX by remember { mutableStateOf(3.5f) }
    var posY by remember { mutableStateOf(3.5f) }
    var dirAngle by remember { mutableStateOf(0.0f) } // in radians
    
    // Weapon trigger & visual flash
    var weaponFiringState by remember { mutableStateOf(0) } // 0=idle, 1=firing, 2=recovering
    var weaponBob by remember { mutableStateOf(0f) }
    var walkTimer by remember { mutableStateOf(0f) }
    
    // Game variables
    var score by remember { mutableStateOf(0) }
    var health by remember { mutableStateOf(100) }
    var message by remember { mutableStateOf("MISSION: ELIMINATE THE ALIEN CORES") }
    var livesLeft by remember { mutableStateOf(3) }
    
    // Alien sprites
    class Guard(var x: Float, var y: Float, val type: Int, var active: Boolean = true, var health: Int = 1)
    val guards = remember {
        mutableStateListOf(
            Guard(1.5f, 1.5f, 1),
            Guard(1.5f, 8.5f, 2),
            Guard(8.5f, 1.5f, 1),
            Guard(8.5f, 8.5f, 2),
            Guard(5.5f, 5.5f, 3) // Alien Boss Core
        )
    }

    // Raycast Maze Map (10x10)
    val map = arrayOf(
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 1, 1, 0, 0, 1, 1, 0, 1),
        intArrayOf(1, 0, 1, 0, 0, 0, 0, 1, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 1, 0, 0, 0, 0, 1, 0, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 1, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
    )

    // Main Engine Game Loop
    LaunchedEffect(keyStates, weaponFiringState) {
        val speed = 0.12f
        val rotSpeed = 0.08f
        
        while (true) {
            // Weapon Fire State Updates
            if (weaponFiringState == 1) {
                delay(80)
                weaponFiringState = 2
            } else if (weaponFiringState == 2) {
                delay(120)
                weaponFiringState = 0
            }

            // Input handlers
            var walked = false
            if (keyStates.left) {
                dirAngle -= rotSpeed
                if (dirAngle < 0) dirAngle += (2 * PI).toFloat()
            }
            if (keyStates.right) {
                dirAngle += rotSpeed
                if (dirAngle > 2 * PI) dirAngle -= (2 * PI).toFloat()
            }
            if (keyStates.up) {
                val nextX = posX + cos(dirAngle) * speed
                val nextY = posY + sin(dirAngle) * speed
                // Basic Wall Collision Check
                if (map.getOrNull(nextY.toInt())?.getOrNull(posX.toInt()) == 0) {
                    posY = nextY
                }
                if (map.getOrNull(posY.toInt())?.getOrNull(nextX.toInt()) == 0) {
                    posX = nextX
                }
                walked = true
            }
            if (keyStates.down) {
                val nextX = posX - cos(dirAngle) * speed
                val nextY = posY - sin(dirAngle) * speed
                if (map.getOrNull(nextY.toInt())?.getOrNull(posX.toInt()) == 0) {
                    posY = nextY
                }
                if (map.getOrNull(posY.toInt())?.getOrNull(nextX.toInt()) == 0) {
                    posX = nextX
                }
                walked = true
            }

            if (walked) {
                walkTimer += 0.35f
                weaponBob = sin(walkTimer) * 12f
            } else {
                weaponBob = 0f
            }

            // Fire input
            if (keyStates.actA && weaponFiringState == 0) {
                weaponFiringState = 1
                // Raycast target check (simple line of sight test to kill guards)
                var hitTarget = false
                guards.forEach { guard ->
                    if (guard.active) {
                        // Angle from player to guard
                        val dx = guard.x - posX
                        val dy = guard.y - posY
                        val angleToGuard = atan2(dy, dx)
                        // Difference between looking angle and guard angle
                        var angleDiff = abs(dirAngle - angleToGuard)
                        if (angleDiff > PI) angleDiff = (2 * PI).toFloat() - angleDiff
                        
                        // Distance
                        val dist = sqrt(dx * dx + dy * dy)
                        // If guard is nearby and within a narrow cone (~12 degrees)
                        if (dist < 8f && angleDiff < 0.22f) {
                            guard.health -= 1
                            if (guard.health <= 0) {
                                guard.active = false
                                score += if (guard.type == 3) 500 else 100
                                onScoreSaved(score)
                                if (guard.type == 3) {
                                    message = "VICTORY! CORE ELIMINATED!"
                                } else {
                                    message = "SECURED THE CORRIDOR! +100 PTS"
                                }
                            } else {
                                message = "TARGET DAMAGED!"
                            }
                            hitTarget = true
                        }
                    }
                }
                if (!hitTarget) {
                    message = "MISSED! ROTATE GUN RETICLE"
                }
            }

            // Periodic Guard Attack Actions
            if (SystemClock.uptimeMillis() % 150 < 15) {
                guards.forEach { guard ->
                    if (guard.active) {
                        val distance = sqrt((guard.x - posX).pow(2) + (guard.y - posY).pow(2))
                        if (distance < 1.5f) {
                            health -= 5
                            message = "WARNING: CORE INTERACTION SHIELD DEPLETED!"
                            if (health <= 0) {
                                if (livesLeft > 1) {
                                    livesLeft--
                                    health = 100
                                    posX = 3.5f
                                    posY = 3.5f
                                    message = "RESPAWNING RESCUE PROBE..."
                                } else {
                                    message = "GAME OVER. SYSTEM PURGED."
                                }
                            }
                        }
                    }
                }
            }

            delay(33) // ~30 fps update clock
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // 1. Draw solid Retro Sky (Upper half) & Floor (Lower half)
            drawRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(0f, 0f),
                size = Size(w, h / 2f)
            )
            drawRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(0f, h / 2f),
                size = Size(w, h / 2f)
            )

            // 2. Draw Retro Pseudo-3D Walls (Raycaster columns)
            val numRays = 130
            val rayWidth = w / numRays
            val fov = 1.04f // 60 degrees

            for (i in 0 until numRays) {
                // Calculate ray angle
                val rayAngle = (dirAngle - fov / 2f) + (i.toFloat() / numRays) * fov
                
                // Set casting parameters
                var curX = posX
                var curY = posY
                val stepSize = 0.05f
                val maxCamDist = 12.0f
                var dist = 0.0f
                var hit = false
                var wallType = 1

                val cosRay = cos(rayAngle)
                val sinRay = sin(rayAngle)

                while (dist < maxCamDist && !hit) {
                    dist += stepSize
                    curX += cosRay * stepSize
                    curY += sinRay * stepSize
                    
                    val mapX = curX.toInt()
                    val mapY = curY.toInt()

                    if (mapX >= 0 && mapY >= 0 && mapY < map.size && mapX < map[0].size) {
                        if (map[mapY][mapX] > 0) {
                            hit = true
                            wallType = map[mapY][mapX]
                        }
                    }
                }

                // Standard Fish-eye correction math
                val correctedDist = dist * cos(rayAngle - dirAngle)
                
                // Draw Wall Slice Column
                val wallHeight = (h / (correctedDist + 0.1f)).coerceAtMost(h)
                val topY = (h - wallHeight) / 2f
                
                // Shading based on distance (Atmosphere effect!)
                val baseColVal = (255 - (correctedDist * 20).toInt()).coerceIn(40, 240)
                // Color based on wall types inside map
                val wallColor = if (wallType == 1) {
                    Color(0, baseColVal / 2, baseColVal, 255)
                } else {
                    Color(baseColVal, baseColVal / 2, 0, 255)
                }

                drawRect(
                    color = wallColor,
                    topLeft = Offset(i * rayWidth, topY),
                    size = Size(rayWidth + 1f, wallHeight)
                )

                // Render Retro Scanlines inside column
                for (sy in topY.toInt() until (topY + wallHeight).toInt() step 6) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.15f),
                        start = Offset(i * rayWidth, sy.toFloat()),
                        end = Offset((i + 1) * rayWidth, sy.toFloat()),
                        strokeWidth = 2f
                    )
                }
            }

            // 3. Draw Guards/Sprites overlay (Basic projection)
            guards.forEach { guard ->
                if (guard.active) {
                    val dx = guard.x - posX
                    val dy = guard.y - posY
                    
                    // Transformation/Angle math
                    val angleToSprite = atan2(dy, dx)
                    var spriteDiff = angleToSprite - dirAngle
                    while (spriteDiff < -PI) spriteDiff += (2 * PI).toFloat()
                    while (spriteDiff > PI) spriteDiff -= (2 * PI).toFloat()
                    
                    val spriteDist = sqrt(dx * dx + dy * dy)
                    
                    // Calculate visual column on screen
                    if (abs(spriteDiff) < (fov / 2f) && spriteDist > 0.4f && spriteDist < 10f) {
                        val screenX = (w / 2) + (spriteDiff / (fov / 2)) * (w / 2)
                        val spriteHeight = (h / spriteDist).coerceAtMost(h * 0.9f)
                        val spriteTopY = (h - spriteHeight) / 2
                        
                        // Draw Retro sprite representations
                        val spriteColor = when(guard.type) {
                            1 -> Color(0xFFEF4444) // Red Guard alien
                            2 -> Color(0xFF22C55E) // Green Toxic turret
                            else -> Color(0xFFEAB308) // Boss Golden Core glowing yellow
                        }

                        // Alien Body
                        drawRect(
                            color = spriteColor,
                            topLeft = Offset(screenX.toFloat() - spriteHeight / 4, spriteTopY),
                            size = Size(spriteHeight / 2, spriteHeight)
                        )
                        // Alien Glowing Reticle Core
                        drawCircle(
                            color = Color.Cyan,
                            radius = spriteHeight / 8,
                            center = Offset(screenX.toFloat(), spriteTopY + spriteHeight / 5)
                        )
                        // Shield lines
                        drawCircle(
                            color = Color.White.copy(alpha = 0.6f),
                            radius = spriteHeight / 3,
                            center = Offset(screenX.toFloat(), spriteTopY + spriteHeight / 2),
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }

            // 4. Draw Crosshair
            drawCircle(
                color = Color.Green,
                radius = 12f,
                center = Offset(w / 2, h / 2),
                style = Stroke(width = 2f)
            )
            drawRect(
                color = Color.Green,
                topLeft = Offset(w / 2 - 2, h / 2 - 8),
                size = Size(4f, 16f)
            )
            drawRect(
                color = Color.Green,
                topLeft = Offset(w / 2 - 8, h / 2 - 2),
                size = Size(16f, 4f)
            )

            // 5. Draw MiniMap in Top-Right
            val pad = 24f
            val gridSz = 12f
            map.forEachIndexed { my, row ->
                row.forEachIndexed { mx, valAt ->
                    val colorMap = when {
                        mx == posX.toInt() && my == posY.toInt() -> Color.Green
                        valAt == 1 -> Color(0xFF475569)
                        valAt == 2 -> Color(0xFFF97316)
                        else -> Color(0xFF0F172A).copy(alpha = 0.8f)
                    }
                    drawRect(
                        color = colorMap,
                        topLeft = Offset(w - (10 * gridSz) - pad + (mx * gridSz), pad + (my * gridSz)),
                        size = Size(gridSz - 1, gridSz - 1)
                    )
                }
            }
            // Draw small line for direction in minimap view
            val mapPlayerX = w - (10 * gridSz) - pad + (posX * gridSz)
            val mapPlayerY = pad + (posY * gridSz)
            drawLine(
                color = Color.Green,
                start = Offset(mapPlayerX, mapPlayerY),
                end = Offset(mapPlayerX + cos(dirAngle) * 16f, mapPlayerY + sin(dirAngle) * 16f),
                strokeWidth = 3f
            )
        }

        // 6. Draw Simulated Doom Heavy Armament Weapon Sprite Bottom Centered
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = weaponBob.dp)
                .fillMaxWidth(0.42f)
                .fillMaxHeight(0.35f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cw = size.width
                val ch = size.height
                
                // Weapon Base
                drawRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(cw * 0.35f, ch * 0.4f),
                    size = Size(cw * 0.3f, ch * 0.6f)
                )
                // Left Double Rail barrel
                drawRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(cw * 0.38f, ch * 0.1f),
                    size = Size(cw * 0.08f, ch * 0.4f)
                )
                // Right Double Rail barrel
                drawRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(cw * 0.54f, ch * 0.1f),
                    size = Size(cw * 0.08f, ch * 0.4f)
                )
                // Laser scope glow
                drawCircle(
                    color = if (weaponFiringState == 1) Color.Red else Color(0xFFFF5555),
                    radius = 8f,
                    center = Offset(cw * 0.5f, ch * 0.35f)
                )

                if (weaponFiringState == 1) {
                    // Muzzle flash particle beams
                    drawCircle(
                        color = Color.Yellow,
                        radius = 42f,
                        center = Offset(cw * 0.5f, ch * 0.08f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 24f,
                        center = Offset(cw * 0.5f, ch * 0.08f)
                    )
                    // Flash lines extending outwards
                    drawLine(
                        color = Color.Cyan,
                        start = Offset(cw * 0.5f, ch * 0.08f),
                        end = Offset(cw * 0.1f, -ch * 0.2f),
                        strokeWidth = 6f
                    )
                    drawLine(
                        color = Color.Cyan,
                        start = Offset(cw * 0.5f, ch * 0.08f),
                        end = Offset(cw * 0.9f, -ch * 0.2f),
                        strokeWidth = 6f
                    )
                }
            }
        }

        // 7. Render bottom Retro OS DOS gaming status HUD bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SCORE: $score",
                    color = Color(0xFFEAB308),
                    fontSize = 15.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "HP: $health%",
                    color = if (health > 30) Color.Green else Color.Red,
                    fontSize = 15.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "PROBES: $livesLeft",
                    color = Color.Cyan,
                    fontSize = 15.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "LOG: $message",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

// ==========================================
// 2. COMMANDER CODE (2D ARCADE PLATFORMER)
// ==========================================

@Composable
fun PlatformerGameScreen(
    keyStates: EmulatorKeyStates,
    onScoreSaved: (Int) -> Unit
) {
    var plyX by remember { mutableStateOf(100f) }
    var plyY by remember { mutableStateOf(300f) }
    var velY by remember { mutableStateOf(0f) }
    var isJumping by remember { mutableStateOf(false) }
    
    // Level & gameplay configurations
    var collectedCount by remember { mutableStateOf(0) }
    var progressMessage by remember { mutableStateOf("SYSTEM PROBE STAGE 1: GRAB CHIPS") }
    var hazardState by remember { mutableStateOf(0f) }
    
    // Default blocks (Platformer structures)
    data class Block(val x: Float, val y: Float, val w: Float, val h: Float, val isHazard: Boolean = false)
    val platforms = remember {
        listOf(
            Block(0f, 450f, 1500f, 100f), // Ground base
            Block(250f, 320f, 200f, 30f),  // High layer
            Block(550f, 220f, 200f, 30f),  // Top layer
            Block(850f, 320f, 250f, 30f),  // Bridge
            Block(1200f, 240f, 150f, 30f), // Platform before boss
            Block(450f, 420f, 80f, 30f, isHazard = true), // Spikes bottom
            Block(950f, 420f, 80f, 30f, isHazard = true)  // Spikes bottom 2
        )
    }

    // Interactive chips
    class Chip(val x: Float, val y: Float, var collected: Boolean = false)
    val items = remember {
        mutableStateListOf(
            Chip(350f, 270f),
            Chip(650f, 170f),
            Chip(900f, 275f),
            Chip(1280f, 180f),
            Chip(1400f, 400f)
        )
    }

    // Main Game Loops
    LaunchedEffect(keyStates) {
        val speed = 9f
        val grav = 1.4f
        val jumpImpulse = -22f
        
        while (true) {
            hazardState = (hazardState + 0.1f) % (2 * PI).toFloat()
            
            // Movement Updates
            if (keyStates.left) {
                plyX = (plyX - speed).coerceIn(10f, 1400f)
            }
            if (keyStates.right) {
                plyX = (plyX + speed).coerceIn(10f, 1400f)
            }
            if (keyStates.actY) { // Hold Y for sprinting!
                plyX = if (keyStates.left) (plyX - 4f).coerceIn(10f, 1400f) else if (keyStates.right) (plyX + 4f).coerceIn(10f, 1400f) else plyX
            }

            // Jump command
            if (keyStates.actA && !isJumping) {
                velY = jumpImpulse
                isJumping = true
                progressMessage = "GRAVITY MODULATOR ACTIVE: IN FLIGHT"
            }

            // Apply gravity
            velY += grav
            plyY += velY

            // Collision checks against platforms
            var onSuf = false
            platforms.forEach { b ->
                if (plyX + 32 > b.x && plyX < b.x + b.w) {
                    if (plyY + 48 >= b.y && plyY < b.y + b.h) {
                        if (b.isHazard) {
                            // Hit Spikes! Respawn
                            plyX = 100f
                            plyY = 300f
                            velY = 0f
                            progressMessage = "STACK OVERFLOW DETECTED! CORE RESTARTED"
                        } else {
                            // Normal platform land
                            plyY = b.y - 48
                            velY = 0f
                            onSuf = true
                            isJumping = false
                        }
                    }
                }
            }
            
            if (!onSuf && plyY > 402f) { // Ground clamping
                plyY = 402f
                velY = 0f
                isJumping = false
            }

            // Collect items collision checks
            items.forEach { chip ->
                if (!chip.collected && abs(plyX - chip.x) < 40f && abs(plyY - chip.y) < 40f) {
                    chip.collected = true
                    collectedCount++
                    onScoreSaved(collectedCount * 250)
                    progressMessage = "LOADED MEMORY SEGMENT: 0x${(collectedCount * 12).toString(16).uppercase()}"
                }
            }

            delay(33) // ~30 fps updates
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Retro Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF020617))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("COMMANDER CODE", color = Color(0xFF60A5FA), fontSize = 16.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Text("DATA: $collectedCount / 5", color = Color.Green, fontSize = 14.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Text("SCORE: ${collectedCount * 250}", color = Color.Yellow, fontSize = 14.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cw = size.width
                val ch = size.height

                // Draw falling binary terminal code in backgroung (aesthetic)
                for (bcol in 0 until (cw.toInt() / 80)) {
                    val dropY = ((SystemClock.uptimeMillis() / 20) + (bcol * 70)) % (ch.toInt() + 100) - 50f
                    drawRect(
                        color = Color(34, 197, 94, 25), // Ultra transparent green
                        topLeft = Offset(bcol * 80f, dropY),
                        size = Size(40f, 60f)
                    )
                }

                // Scrolling view offset depending on player
                val viewOffset = (cw / 3f - plyX).coerceAtMost(0f)

                // 1. Draw Platforms
                platforms.forEach { b ->
                    val blockColor = if (b.isHazard) {
                        Color(239, 68, 68, 255) // Spikes Red
                    } else {
                        Color(59, 130, 246, 255) // Cyber Platform Blue
                    }
                    
                    drawRect(
                        color = blockColor,
                        topLeft = Offset(b.x + viewOffset, b.y),
                        size = Size(b.w, b.h)
                    )

                    // Draw grid segments on blocks to give a digital PC motherboard feel
                    for (gx in b.x.toInt() until (b.x + b.w).toInt() step 50) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.4f),
                            start = Offset(gx + viewOffset, b.y),
                            end = Offset(gx + viewOffset, b.y + b.h),
                            strokeWidth = 2f
                        )
                    }
                }

                // 2. Draw Collectible Gold/Green Chips
                items.forEach { chip ->
                    if (!chip.collected) {
                        // Floating bounce offset
                        val floatOff = sin(hazardState * 3f + chip.y) * 8f
                        
                        // Outer neon border
                        drawCircle(
                            color = Color(0xFF22C55E),
                            radius = 16f,
                            center = Offset(chip.x + viewOffset, chip.y + floatOff),
                            style = Stroke(width = 3f)
                        )
                        // Inner processor gold core
                        drawRect(
                            color = Color(0xFFEAB308),
                            topLeft = Offset(chip.x + viewOffset - 6, chip.y + floatOff - 6),
                            size = Size(12f, 12f)
                        )
                    }
                }

                // 3. Draw Player Sprite (Simulated Pixel Probe robot)
                val playAngle = if (isJumping) 15f else sin(hazardState * 5f) * 10f
                val playerDrawX = plyX + viewOffset
                
                // Draw Robotic body
                drawRoundRect(
                    color = Color.Green,
                    topLeft = Offset(playerDrawX - 20, plyY - 30),
                    size = Size(40f, 44f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                // Jetpack booster fire if jumping
                if (velY < 0) {
                    drawCircle(
                        color = Color.Red,
                        radius = 10f,
                        center = Offset(playerDrawX - 10, plyY + 18)
                    )
                    drawCircle(
                        color = Color.Yellow,
                        radius = 6f,
                        center = Offset(playerDrawX - 10, plyY + 18)
                    )
                }

                // Big LED lens head
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = 12f,
                    center = Offset(playerDrawX, plyY - 14)
                )
                drawCircle(
                    color = Color.Cyan,
                    radius = 6f,
                    center = Offset(playerDrawX + 3, plyY - 14) // Eyeball center shifting right
                )
            }
        }

        // Retro Stage Tip Line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(8.dp)
        ) {
            Text(
                text = "IO_STATUS: $progressMessage",
                color = Color(0xFF10B981),
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

// ==========================================
// 3. RETRO COMMANDER RTS (REAL-TIME STRATEGY)
// ==========================================

@Composable
fun RtsGameScreen(
    keyStates: EmulatorKeyStates,
    onScoreSaved: (Int) -> Unit
) {
    // RTS Logic - Simple battlefield grids
    var selectedUnitId by remember { mutableStateOf(-1) }
    var resources by remember { mutableStateOf(1000) }
    var baseHealth by remember { mutableStateOf(100) }
    var enemyCommandCoreHealth by remember { mutableStateOf(100) }
    var actionMessage by remember { mutableStateOf("TAP BATTLEFIELD TO DEPLOY TANKS") }
    
    // Custom entities
    class RtsUnit(val id: Int, val isEnemy: Boolean, var x: Float, var y: Float, var targetX: Float, var targetY: Float, var hp: Int = 50, val type: String = "Medium Tank")
    val units = remember {
        mutableStateListOf(
            RtsUnit(1, false, 150f, 350f, 150f, 350f, 80, "Heavy Armor"),
            RtsUnit(2, false, 250f, 420f, 250f, 420f, 40, "Fast Miner"),
            RtsUnit(101, true, 850f, 180f, 850f, 180f, 60, "Enemy Tank"),
            RtsUnit(102, true, 920f, 300f, 920f, 300f, 60, "Enemy Tank")
        )
    }

    // Ore patches coordinates
    val ores = remember {
        listOf(
            Offset(450f, 200f),
            Offset(520f, 250f),
            Offset(480f, 380f)
        )
    }

    // Attack bullets
    class Bullet(var current: Offset, val target: Offset, val isEnemy: Boolean)
    val activeBullets = remember { mutableStateListOf<Bullet>() }

    // Rts Game Loops
    LaunchedEffect(keyStates) {
        var idCounter = 3
        while (true) {
            // Unit deployment triggers on mapping inputs!
            if (keyStates.actA && resources >= 400) {
                // Deploy military armor at Player base
                units.add(RtsUnit(idCounter++, false, 120f, 280f + (idCounter % 3) * 40f, 500f, 300f, 75, "Heavy Armor"))
                resources -= 400
                actionMessage = "HEAVY METALS CHASSIS EN ROUTE SYSTEM ORDERED (-400 CREDITS)"
                delay(300)
            }
            if (keyStates.actB && resources >= 250) {
                // Deploy Harvester
                units.add(RtsUnit(idCounter++, false, 100f, 320f, 480f, 220f, 40, "Fast Miner"))
                resources -= 250
                actionMessage = "COALITION MINING UNIT DEPLOYED"
                delay(300)
            }

            // Move custom models step-by-step
            units.forEach { unit ->
                val dx = unit.targetX - unit.x
                val dy = unit.targetY - unit.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > 8f) {
                    unit.x += (dx / dist) * 4f
                    unit.y += (dy / dist) * 4f
                } else if (unit.type == "Fast Miner" && !unit.isEnemy) {
                    // Mining Ore loop! Auto travel back
                    val nearestOre = ores.minBy { Offset(unit.x, unit.y).minus(it).getDistance() }
                    val distToOre = Offset(unit.x, unit.y).minus(nearestOre).getDistance()
                    
                    if (distToOre < 40f) {
                        resources += 15
                        unit.targetX = 120f // Travel to base depot
                        unit.targetY = 320f
                    } else if (abs(unit.x - 120f) < 20f && abs(unit.y - 320f) < 20f) {
                        unit.targetX = nearestOre.x
                        unit.targetY = nearestOre.y
                    }
                }
            }

            // Periodic combats: bullet firing between targets
            units.firstOrNull { !it.isEnemy && it.type == "Heavy Armor" }?.let { ally ->
                units.firstOrNull { it.isEnemy && it.hp > 0 }?.let { enemy ->
                    val dist = sqrt((ally.x - enemy.x).pow(2) + (ally.y - enemy.y).pow(2))
                    if (dist < 350f) {
                        activeBullets.add(Bullet(Offset(ally.x, ally.y), Offset(enemy.x, enemy.y), false))
                        enemy.hp -= 8
                        if (enemy.hp <= 0) {
                            units.remove(enemy)
                            resources += 200
                            onScoreSaved(resources)
                            actionMessage = "ENEMY INTERRUPTING AGENT NEUTRALIZED! +200"
                        }
                    }
                }
            }

            units.firstOrNull { it.isEnemy }?.let { enemy ->
                units.firstOrNull { !it.isEnemy }?.let { ally ->
                    val dist = sqrt((enemy.x - ally.x).pow(2) + (enemy.y - ally.y).pow(2))
                    if (dist < 320f) {
                        activeBullets.add(Bullet(Offset(enemy.x, enemy.y), Offset(ally.x, ally.y), true))
                        ally.hp -= 4
                        if (ally.hp <= 0) {
                            units.remove(ally)
                            actionMessage = "DEFENSE WARNING: TANK SQUADRON TAKEN DOWN"
                        }
                    }
                }
            }

            // Automated Enemy Spawning
            if (SystemClock.uptimeMillis() % 6500 < 60) {
                units.add(RtsUnit(idCounter++, true, 950f, 250f, 150f, 350f, 50, "Enemy Tank"))
            }

            // Clean bullets or glide them
            if (activeBullets.isNotEmpty()) {
                delay(100)
                activeBullets.clear()
            }

            delay(50)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // RTS Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("WARROOM TACTICAL: LEVEL 1", color = Color(0xFFEF4444), fontSize = 15.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Text("CREDITS: $$resources", color = Color.Green, fontSize = 15.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("DEPOT SYS: $baseHealth%", color = Color.Cyan, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Text("ACTION BAR: HOLD TOUCH TO STEER", color = Color.Yellow, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }

        // Tap Canvas to direct Ally Tanks
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Set targets for selected or all friendly tanks!
                        units.forEach { u ->
                            if (!u.isEnemy) {
                                u.targetX = offset.x
                                u.targetY = offset.y
                            }
                        }
                        actionMessage = "ALL INDEPENDENT FORCES REDIRECTED TO TARGET VECTOR"
                    }
                }
        ) {
            val h = size.height
            val w = size.width

            // Ground base battle grid
            drawRect(color = Color(0xFF1E293B))

            // Grass accents / tactical grid coordinates
            for (gx in 0 until w.toInt() step 90) {
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(gx.toFloat(), 0f),
                    end = Offset(gx.toFloat(), h)
                )
            }
            for (gy in 0 until h.toInt() step 90) {
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, gy.toFloat()),
                    end = Offset(w, gy.toFloat())
                )
            }

            // 1. Draw Player Depot base (Left side)
            drawRect(
                color = Color(34, 211, 238, 200),
                topLeft = Offset(10f, 250f),
                size = Size(100f, 150f)
            )
            drawRect(
                color = Color.Cyan,
                topLeft = Offset(10f, 250f),
                size = Size(100f, 150f),
                style = Stroke(width = 4f)
            )

            // 2. Draw Enemy Reactor Core (Right side)
            drawRect(
                color = Color(248, 113, 113, 200),
                topLeft = Offset(w - 110f, 200f),
                size = Size(100f, 170f)
            )
            drawRect(
                color = Color.Red,
                topLeft = Offset(w - 110f, 200f),
                size = Size(100f, 170f),
                style = Stroke(width = 4f)
            )

            // 3. Draw Ore tiberium fields (Green neon crystals)
            ores.forEach { o ->
                drawCircle(
                    color = Color(0xFF22C55E),
                    radius = 25f,
                    center = o,
                    style = Stroke(width = 6f)
                )
                drawCircle(
                    color = Color.Green,
                    radius = 12f,
                    center = o
                )
            }

            // 4. Draw Units on screen
            units.forEach { u ->
                val primaryColor = if (u.isEnemy) Color(0xFFEF4444) else Color(0xFF3B82F6)
                
                // Draw unit shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = 24f,
                    center = Offset(u.x + 2f, u.y + 4f)
                )
                // Draw unit body body (Heavy core or harvester)
                if (u.type == "Fast Miner") {
                    drawRect(
                        color = Color.Yellow,
                        topLeft = Offset(u.x - 16f, u.y - 12f),
                        size = Size(32f, 24f)
                    )
                    // Miner cage outline
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(u.x - 16f, u.y - 12f),
                        size = Size(32f, 24f),
                        style = Stroke(width = 2f)
                    )
                } else {
                    drawCircle(
                        color = primaryColor,
                        radius = 20f,
                        center = Offset(u.x, u.y)
                    )
                    // Tank turret gun pointing forward to target X
                    val gunDx = u.targetX - u.x
                    val gunDy = u.targetY - u.y
                    val gunDist = sqrt(gunDx * gunDx + gunDy * gunDy)
                    val cosG = if (gunDist > 0) gunDx / gunDist else 1.0f
                    val sinG = if (gunDist > 0) gunDy / gunDist else 0.0f
                    
                    drawLine(
                        color = Color.White,
                        start = Offset(u.x, u.y),
                        end = Offset(u.x + cosG * 30f, u.y + sinG * 30f),
                        strokeWidth = 6f
                    )
                    
                    // Health indicators bar
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(u.x - 15f, u.y - 30f),
                        size = Size(30f, 5f)
                    )
                    drawRect(
                        color = if (u.isEnemy) Color.Red else Color.Green,
                        topLeft = Offset(u.x - 15f, u.y - 30f),
                        size = Size(30f * (u.hp / 80f).coerceIn(0f, 1f), 5f)
                    )
                }
            }

            // 5. Draw active bullet tracers laser beams
            activeBullets.forEach { b ->
                drawLine(
                    color = if (b.isEnemy) Color.Red else Color.Cyan,
                    start = b.current,
                    end = b.target,
                    strokeWidth = 4f
                )
            }
        }

        // RTS bottom tip bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF020617))
                .padding(10.dp)
        ) {
            Text(
                text = "SYSTEM REPORTS: $actionMessage",
                color = Color(0xFFFF5555),
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
