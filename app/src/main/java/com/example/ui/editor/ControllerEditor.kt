package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ControllerButtonConfig
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditControllerLayoutScreen(
    currentButtons: List<ControllerButtonConfig>,
    onSaveButton: (ControllerButtonConfig) -> Unit,
    onResetDefaults: () -> Unit,
    onCloseEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Current active button being configured
    var selectedButtonForEdit by remember { mutableStateOf<ControllerButtonConfig?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        if (widthPx == 0f || heightPx == 0f) return@BoxWithConstraints

        // Visual grid pattern to aid design alignment
        Box(modifier = Modifier.fillMaxSize()) {
            // Draw grid in columns
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(8) {
                    Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.04f)))
                }
            }
            // Draw grid in rows
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                repeat(8) {
                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(alpha = 0.04f)))
                }
            }
        }

        // Overlay active buttons (Tappable and Draggable in edit mode)
        currentButtons.forEach { button ->
            // Calculate pixel density off offsets
            val btnX = button.posX * widthPx
            val btnY = button.posY * heightPx

            Box(
                modifier = Modifier
                    .offset { IntOffset(btnX.roundToInt(), btnY.roundToInt()) }
                    .pointerInput(button.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Calculate new relative offsets
                            val newX = ((btnX + dragAmount.x) / widthPx).coerceIn(0.01f, 0.95f)
                            val newY = ((btnY + dragAmount.y) / heightPx).coerceIn(0.05f, 0.92f)
                            onSaveButton(button.copy(posX = newX, posY = newY))
                        }
                    }
                    .size((64 * button.scale).dp)
                    .alpha(button.opacity)
                    .background(Color(0xFF1E293B), shape = CircleShape)
                    .border(2.dp, Color(0xFF60A5FA), shape = CircleShape)
                    .clickable {
                        selectedButtonForEdit = button
                        showEditDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = button.btnLabel,
                    color = Color.White,
                    fontSize = (15 * button.scale).sp,
                    style = MaterialTheme.typography.titleMedium
                )
                // Small indicator showing it can be dragged
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Green, shape = CircleShape)
                        .align(Alignment.TopEnd)
                )
            }
        }

        // Top Command Layout Actions Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(12.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "VIRTUAL TOUCH CONTROLLER DESIGNER",
                        color = Color.White,
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "DRAG buttons to move. TAP to customise size & key mappings.",
                        color = Color.LightGray,
                        fontSize = 10.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onResetDefaults,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Layout")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Codes", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onCloseEditor,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Save Layout")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save & Exit", fontSize = 11.sp)
                    }
                }
            }
        }

        // Detail mapping edit dialog
        if (showEditDialog && selectedButtonForEdit != null) {
            val config = selectedButtonForEdit!!
            var editLabel by remember(config.id) { mutableStateOf(config.btnLabel) }
            var editScale by remember(config.id) { mutableStateOf(config.scale) }
            var editOpacity by remember(config.id) { mutableStateOf(config.opacity) }
            var editScanCode by remember(config.id) { mutableStateOf(config.mappedScanCode) }

            // Common Virtual Key presets mapped to Android KeyCodes
            val keyPresets = listOf(
                Pair("SPACE (Jump/Fire)", 62),
                Pair("ENTER (Select)", 66),
                Pair("ARROW UP", 19),
                Pair("ARROW LEFT", 21),
                Pair("ARROW RIGHT", 22),
                Pair("ARROW DOWN", 20),
                Pair("ESCAPE (Exit)", 111),
                Pair("TAB (Map)", 61),
                Pair("L-CTRL (Sprints)", 113),
                Pair("L-ALT (Inventory)", 57),
                Pair("KEY W", 51),
                Pair("KEY A", 29),
                Pair("KEY S", 47),
                Pair("KEY D", 32)
            )

            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Configure [${config.id}] Button") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Button Label Input
                        TextField(
                            value = editLabel,
                            onValueChange = { editLabel = it },
                            label = { Text("Button Label Text / Icon String") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Button scale slider
                        Column {
                            Text("Button Scale Factor: ${(editScale * 10).roundToInt() / 10f}x")
                            Slider(
                                value = editScale,
                                onValueChange = { editScale = it },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Button Opacity slider
                        Column {
                            Text("Button Opacity: ${(editOpacity * 100).roundToInt()}%")
                            Slider(
                                value = editOpacity,
                                onValueChange = { editOpacity = it },
                                valueRange = 0.1f..1.0f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // KeyMapping Config Dropdown
                        Text("Simulated PC Keyboard Mapping Keys:", style = MaterialTheme.typography.titleSmall)
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            keyPresets.forEach { (name, code) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editScanCode = code }
                                        .background(
                                            if (editScanCode == code) Color(0xFF1E3A8A) else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(8.dp)
                                        .border(
                                            1.dp,
                                            if (editScanCode == code) Color.Cyan else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                ) {
                                    RadioButton(
                                        selected = (editScanCode == code),
                                        onClick = { editScanCode = code }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name, color = if (editScanCode == code) Color.Cyan else Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onSaveButton(
                            config.copy(
                                btnLabel = editLabel,
                                scale = editScale,
                                opacity = editOpacity,
                                mappedScanCode = editScanCode
                            )
                        )
                        showEditDialog = false
                    }) {
                        Text("Apply Configurations")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
