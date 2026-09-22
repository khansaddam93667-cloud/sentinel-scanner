package com.example.secscanner

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

val NeonGreen = Color(0xFF00E676)
val Crimson = Color(0xFFFF1744)
val Cyan = Color(0xFF00E5FF)

@Composable
fun rememberGyroscopeTilt(enabled: Boolean = true): Pair<Float, Float> {
    val context = LocalContext.current
    var pitchTilt by remember { mutableStateOf(0f) }
    var rollTilt by remember { mutableStateOf(0f) }

    DisposableEffect(context, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                    pitchTilt = orientationAngles[1]
                    rollTilt = orientationAngles[2]
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                     val x = event.values[0]
                     val y = event.values[1]

                     pitchTilt = -y / 9.8f
                     rollTilt = x / 9.8f
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return Pair(pitchTilt, rollTilt)
}

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color = NeonGreen,
    tiltEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val (pitchTilt, rollTilt) = rememberGyroscopeTilt(enabled = tiltEnabled)
    val density = LocalDensity.current.density

    Card(
        modifier = modifier
            .graphicsLayer {
                if (tiltEnabled) {
                    rotationX = pitchTilt * 12f
                    rotationY = -rollTilt * 12f
                    cameraDistance = 16 * density
                }
            },
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        content()
    }
}
