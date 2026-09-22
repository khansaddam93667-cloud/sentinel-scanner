package com.example.secscanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HologramCubeWidget() {
    var rotationX by remember { mutableStateOf(0f) }
    var rotationY by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { time ->
                if (lastTime != 0L) {
                    val delta = (time - lastTime) / 1_000_000_000f
                    rotationX += delta * 0.5f
                    rotationY += delta * 0.5f
                }
                lastTime = time
            }
        }
    }

    val vertices = remember {
        arrayOf(
            floatArrayOf(-1f, -1f, -1f), floatArrayOf(1f, -1f, -1f), floatArrayOf(1f, 1f, -1f), floatArrayOf(-1f, 1f, -1f),
            floatArrayOf(-1f, -1f, 1f), floatArrayOf(1f, -1f, 1f), floatArrayOf(1f, 1f, 1f), floatArrayOf(-1f, 1f, 1f)
        )
    }

    val edges = remember {
        arrayOf(
            intArrayOf(0, 1), intArrayOf(1, 2), intArrayOf(2, 3), intArrayOf(3, 0),
            intArrayOf(4, 5), intArrayOf(5, 6), intArrayOf(6, 7), intArrayOf(7, 4),
            intArrayOf(0, 4), intArrayOf(1, 5), intArrayOf(2, 6), intArrayOf(3, 7)
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    rotationY += dragAmount.x * 0.01f
                    rotationX += dragAmount.y * 0.01f
                }
            }
    ) {
        val cx = size.width / 2
        val cy = size.height / 2
        val scale = size.width / 4
        val distance = 4f

        val projected = Array(8) { Offset.Zero }

        for (i in 0..7) {
            val v = vertices[i]
            var x = v[0]
            var y = v[1]
            var z = v[2]

            // Rotate X
            var tempY = y * cos(rotationX) - z * sin(rotationX)
            var tempZ = y * sin(rotationX) + z * cos(rotationX)
            y = tempY
            z = tempZ

            // Rotate Y
            var tempX = x * cos(rotationY) + z * sin(rotationY)
            tempZ = -x * sin(rotationY) + z * cos(rotationY)
            x = tempX
            z = tempZ

            z += distance

            val px = (x * distance) / z
            val py = (y * distance) / z

            projected[i] = Offset(cx + px * scale, cy + py * scale)
        }

        for (edge in edges) {
            drawLine(
                color = Color(0xFF00E676),
                start = projected[edge[0]],
                end = projected[edge[1]],
                strokeWidth = 3f
            )
        }
    }
}
