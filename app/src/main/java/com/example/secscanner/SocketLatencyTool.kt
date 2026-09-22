package com.example.secscanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

@Composable
fun SocketLatencyTool() {
    var host by remember { mutableStateOf("1.1.1.1") }
    var port by remember { mutableStateOf("53") }
    var resultText by remember { mutableStateOf("") }
    var latency by remember { mutableStateOf<Long?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val presets = listOf("Cloudflare (1.1.1.1)", "Google (8.8.8.8)", "Custom IP")
    val presetIps = listOf("1.1.1.1", "8.8.8.8", "")

    CyberCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Active Socket Prober", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Preset: ", style = MaterialTheme.typography.bodyMedium)
                Row(modifier = Modifier.clickable { expanded = true }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Select", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    presets.forEachIndexed { index, preset ->
                        DropdownMenuItem(
                            text = { Text(preset) },
                            onClick = {
                                if (presetIps[index].isNotEmpty()) {
                                    host = presetIps[index]
                                    port = "53"
                                } else {
                                    host = ""
                                    port = ""
                                }
                                expanded = false
                            }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host") },
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val p = port.toIntOrNull()
                    if (host.isNotBlank() && p != null) {
                        isTesting = true
                        resultText = "Probing..."
                        latency = null
                        coroutineScope.launch {
                            val (res, lat) = probeSocket(host, p)
                            resultText = res
                            latency = lat
                            isTesting = false
                        }
                    } else {
                        resultText = "Invalid Host or Port"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTesting
            ) {
                Text(if (isTesting) "Testing..." else "Probe Port")
            }

            if (resultText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Result: $resultText", style = MaterialTheme.typography.bodyMedium)
                    if (latency != null) {
                        val latColor = when {
                            latency!! < 50 -> NeonGreen
                            latency!! < 150 -> Color.Yellow
                            else -> Crimson
                        }
                        Badge(containerColor = latColor) {
                            Text("${latency}ms", color = Color.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

suspend fun probeSocket(host: String, port: Int): Pair<String, Long?> = withContext(Dispatchers.IO) {
    var lat: Long? = null
    var status = ""
    try {
        val socket = Socket()
        val time = measureTimeMillis {
            socket.connect(InetSocketAddress(host, port), 3000)
        }
        lat = time
        status = "OPEN"
        socket.close()
    } catch (e: java.net.SocketTimeoutException) {
        status = "TIMEOUT"
    } catch (e: Exception) {
        status = "CLOSED (${e.message})"
    }
    Pair(status, lat)
}
