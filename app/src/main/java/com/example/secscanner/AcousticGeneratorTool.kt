package com.example.secscanner

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AcousticGeneratorTool() {
    var frequency by remember { mutableStateOf(440f) }
    var isPlaying by remember { mutableStateOf(false) }

    val sampleRate = 44100
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(isPlaying) {
        var audioTrack: AudioTrack? = null
        var keepPlaying = isPlaying

        if (isPlaying) {
            coroutineScope.launch(Dispatchers.IO) {
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )

                audioTrack?.play()

                val buffer = ShortArray(bufferSize)
                var angle = 0.0

                try {
                    while (keepPlaying) {
                        val currentFreq = frequency.toDouble()
                        val angularVelocity = 2.0 * PI * currentFreq / sampleRate

                        for (i in buffer.indices) {
                            buffer[i] = (sin(angle) * Short.MAX_VALUE).toInt().toShort()
                            angle += angularVelocity
                        }
                        audioTrack?.write(buffer, 0, buffer.size)
                    }
                } finally {
                    audioTrack?.stop()
                    audioTrack?.release()
                }
            }
        }

        onDispose {
            keepPlaying = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Acoustic Tone Generator", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Frequency: ${frequency.toInt()} Hz")
        Slider(
            value = frequency,
            onValueChange = { frequency = it },
            valueRange = 200f..18000f,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { isPlaying = !isPlaying },
            colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        ) {
            Text(if (isPlaying) "Stop Tone" else "Play Tone")
        }

        Spacer(modifier = Modifier.height(32.dp))

        val animatedPhase = remember { androidx.compose.animation.core.Animatable(0f) }
        LaunchedEffect(isPlaying, frequency) {
            if (isPlaying) {
                while (true) {
                    animatedPhase.animateTo(
                        targetValue = animatedPhase.value + 2f * PI.toFloat(),
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(durationMillis = (1000f / (frequency / 100f)).toInt().coerceAtLeast(16), easing = androidx.compose.animation.core.LinearEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                        )
                    )
                }
            } else {
                animatedPhase.snapTo(0f)
            }
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2f

            val path = Path()
            val points = 200
            val waveAmplitude = canvasHeight / 3f

            drawLine(
                color = Color.DarkGray,
                start = Offset(0f, centerY),
                end = Offset(canvasWidth, centerY),
                strokeWidth = 2f
            )

            if (isPlaying) {
                val waveFrequency = frequency / 2000f
                for (i in 0..points) {
                    val x = (i.toFloat() / points) * canvasWidth
                    val y = centerY + sin((x / canvasWidth) * 2 * PI.toFloat() * waveFrequency + animatedPhase.value) * waveAmplitude

                    if (i == 0) path.moveTo(x, y.toFloat())
                    else path.lineTo(x, y.toFloat())
                }

                drawPath(
                    path = path,
                    color = Color.Green,
                    style = Stroke(width = 4f)
                )
            }
        }
    }
}
