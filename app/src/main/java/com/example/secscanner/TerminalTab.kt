package com.example.secscanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

@Composable
fun TerminalTab() {
    val obsidianBlack = Color(0xFF0D1117)
    val neonGreen = Color(0xFF00FF66)

    var outputLines by remember { mutableStateOf(listOf("SentinelLab Interactive Shell Initialized...")) }
    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(outputLines.size) {
        if (outputLines.isNotEmpty()) {
            listState.animateScrollToItem(outputLines.size - 1)
        }
    }

    val executeCommand = { cmd: String ->
        if (cmd.isNotBlank()) {
            outputLines = outputLines + "> $cmd"
            when (cmd.trim()) {
                "clear" -> outputLines = listOf()
                "help" -> outputLines = outputLines + listOf(
                    "Available internal commands:",
                    "  help       - List internal shortcuts",
                    "  clear      - Clear terminal output",
                    "  sysinfo    - Print Kernel, OS build, and CPU ABI",
                    "  top-apps   - List installed 3rd party apps"
                )
                "sysinfo" -> {
                    coroutineScope.launch {
                        val kernel = withContext(Dispatchers.IO) {
                            try {
                                val pb = ProcessBuilder("uname", "-a")
                                val p = pb.start()
                                InputStreamReader(p.inputStream).readText().trim()
                            } catch(e: Exception) { "Unknown" }
                        }
                        val build = android.os.Build.DISPLAY
                        val abi = android.os.Build.SUPPORTED_ABIS.joinToString(", ")
                        outputLines = outputLines + listOf(
                            "Kernel: $kernel",
                            "OS Build: $build",
                            "CPU ABI: $abi"
                        )
                    }
                }
                "top-apps" -> {
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            try {
                                val pb = ProcessBuilder("pm", "list", "packages", "-3")
                                val p = pb.start()
                                InputStreamReader(p.inputStream).readLines()
                            } catch(e: Exception) { listOf("Error fetching apps: ${e.message}") }
                        }
                        outputLines = outputLines + result
                    }
                }
                else -> {
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            try {
                                val pb = ProcessBuilder("/system/bin/sh", "-c", cmd)
                                pb.redirectErrorStream(true)
                                val p = pb.start()
                                val output = InputStreamReader(p.inputStream).readLines()
                                p.waitFor()
                                if (output.isEmpty()) listOf("[Success]") else output
                            } catch (e: Exception) {
                                listOf("Error: ${e.message}")
                            }
                        }
                        outputLines = outputLines + result
                    }
                }
            }
            inputCommand = ""
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(obsidianBlack).padding(16.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(outputLines) { line ->
                Text(
                    text = line,
                    color = neonGreen,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("clear", "help", "sysinfo", "ifconfig", "uname").forEach { shortcut ->
                Button(
                    onClick = { executeCommand(shortcut) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(shortcut, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 120.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputCommand,
                onValueChange = { inputCommand = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter command...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = neonGreen,
                    unfocusedTextColor = neonGreen,
                    focusedBorderColor = neonGreen,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = neonGreen
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { executeCommand(inputCommand) },
                colors = ButtonDefaults.buttonColors(containerColor = neonGreen, contentColor = obsidianBlack)
            ) {
                Text("Send", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}
