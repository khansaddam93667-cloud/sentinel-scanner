package com.example.secscanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin

class Particle {
    var x: Float = (Random.nextFloat() - 0.5f) * 2f
    var y: Float = (Random.nextFloat() - 0.5f) * 2f
    var z: Float = Random.nextFloat() * 2f
}

@Composable
fun ParticleBenchmarkTool() {
    var particleCount by remember { mutableStateOf(500f) }
    var fps by remember { mutableStateOf(0) }
    var time by remember { mutableStateOf(0f) }

    val particles = remember { Array(1500) { Particle() } }

    LaunchedEffect(particleCount) {
        var lastTime = 0L
        var frames = 0
        var lastFpsTime = 0L

        while (true) {
            withFrameNanos { frameTime ->
                if (lastTime != 0L) {
                    val delta = (frameTime - lastTime) / 1_000_000_000f
                    time += delta
                    frames++

                    if (frameTime - lastFpsTime > 1_000_000_000L) {
                        fps = frames
                        frames = 0
                        lastFpsTime = frameTime
                    }
                } else {
                    lastFpsTime = frameTime
                }
                lastTime = frameTime
            }
        }
    }

    CyberCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Adreno 120Hz 3D Particle Benchmark", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Particles: ${particleCount.toInt()}")
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = particleCount,
                    onValueChange = { particleCount = it },
                    valueRange = 100f..1500f,
                    modifier = Modifier.weight(1f)
                )
            }
            Text("FPS: $fps", color = if (fps >= 60) NeonGreen else if (fps >= 30) Color.Yellow else Crimson)

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                val currentCount = particleCount.toInt()
                for (i in 0 until currentCount) {
                    val p = particles[i]

                    p.z -= 0.05f
                    if (p.z <= 0f) {
                        p.z = 2f
                        p.x = (Random.nextFloat() - 0.5f) * 2f
                        p.y = (Random.nextFloat() - 0.5f) * 2f
                    }

                    val angle = time * 2f
                    val rx = p.x * cos(angle) - p.y * sin(angle)
                    val ry = p.x * sin(angle) + p.y * cos(angle)

                    val px = cx + (rx / p.z) * cx
                    val py = cy + (ry / p.z) * cy

                    if (px in 0f..size.width && py in 0f..size.height) {
                        val alpha = (1f - p.z / 2f).coerceIn(0.1f, 1f)
                        drawCircle(
                            color = Cyan.copy(alpha = alpha),
                            radius = (1f - p.z/2f) * 4f + 1f,
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }
    }
}
