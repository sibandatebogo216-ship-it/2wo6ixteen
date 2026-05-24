package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ui.desktop.EmulatorViewModel
import com.example.ui.desktop.RetroEmulatorConsoleDashboard
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val viewModel: EmulatorViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          RetroEmulatorConsoleDashboard(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }

  // Intercept physical keyboards & external controller buttons!
  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    val processed = viewModel.onKeyEvent(keyCode, true)
    if (processed) return true
    return super.onKeyDown(keyCode, event)
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
    val processed = viewModel.onKeyEvent(keyCode, false)
    if (processed) return true
    return super.onKeyUp(keyCode, event)
  }

  // Intercept physical Gamepad/Joystick stick and HAT direction motions!
  override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
    // Check if the source is a gamepad or joystick
    if (event != null && (event.source and android.view.InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
      if (event.action == MotionEvent.ACTION_MOVE) {
        
        // Map D-pad relative coordinates (HAT Axes)
        val xVal = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val yVal = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        viewModel.addControllerLog("Generic Joystick axis hat shift: X=$xVal, Y=$yVal")

        // Map Axis Hat X directions
        when {
          xVal == -1.0f -> {
            viewModel.onKeyEvent(21, true)  // KEYCODE_DPAD_LEFT (21)
            viewModel.onKeyEvent(22, false) // Release right
          }
          xVal == 1.0f -> {
            viewModel.onKeyEvent(22, true)  // KEYCODE_DPAD_RIGHT (22)
            viewModel.onKeyEvent(21, false) // Release left
          }
          else -> {
            // Centered HAT check
            viewModel.onKeyEvent(21, false)
            viewModel.onKeyEvent(22, false)
          }
        }

        // Map Axis Hat Y directions
        when {
          yVal == -1.0f -> {
            viewModel.onKeyEvent(19, true)  // KEYCODE_DPAD_UP (19)
            viewModel.onKeyEvent(20, false) // Release down
          }
          yVal == 1.0f -> {
            viewModel.onKeyEvent(20, true)  // KEYCODE_DPAD_DOWN (20)
            viewModel.onKeyEvent(19, false) // Release up
          }
          else -> {
            // Centered HAT check
            viewModel.onKeyEvent(19, false)
            viewModel.onKeyEvent(20, false)
          }
        }

        // Map Analog Sticks (Left Stick) for easier games controls!
        val leftX = event.getAxisValue(MotionEvent.AXIS_X)
        val leftY = event.getAxisValue(MotionEvent.AXIS_Y)
        
        if (leftX in -0.2f..0.2f && leftY in -0.2f..0.2f) {
          // Deadzone clearing
          viewModel.onKeyEvent(19, false)
          viewModel.onKeyEvent(20, false)
          viewModel.onKeyEvent(21, false)
          viewModel.onKeyEvent(22, false)
        } else {
          // Evaluate left stick directions mapping
          if (leftX < -0.5f) viewModel.onKeyEvent(21, true)
          if (leftX > 0.5f) viewModel.onKeyEvent(22, true)
          if (leftY < -0.5f) viewModel.onKeyEvent(19, true)
          if (leftY > 0.5f) viewModel.onKeyEvent(20, true)
        }

        return true
      }
    }
    return super.onGenericMotionEvent(event)
  }
}
