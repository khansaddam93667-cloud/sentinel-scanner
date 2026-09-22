package com.example.secscanner

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

@Composable
fun LanScannerTool() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var activeHosts by remember { mutableStateOf<List<String>>(emptyList()) }
    var statusMessage by remember { mutableStateOf("Ready to scan.") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                coroutineScope.launch {
                    isScanning = true
                    statusMessage = "Determining subnet..."
                    val baseIp = getSubnetBaseIp(context)
                    if (baseIp == null) {
                        statusMessage = "Could not determine local IPv4 subnet."
                        isScanning = false
                        return@launch
                    }

                    statusMessage = "Scanning $baseIp.1 to $baseIp.254..."
                    activeHosts = emptyList()

                    val foundHosts = mutableListOf<String>()
                    withContext(Dispatchers.IO) {
                        val deferreds = (1..254).map { i ->
                            async {
                                val targetIp = "$baseIp.$i"
                                var isActive = false
                                val ports = listOf(80, 443, 8080)
                                for (port in ports) {
                                    try {
                                        Socket().use { socket ->
                                            socket.connect(InetSocketAddress(targetIp, port), 150)
                                            isActive = true
                                        }
                                    } catch (e: Exception) {
                                    }
                                    if (isActive) break
                                }
                                if (isActive) targetIp else null
                            }
                        }
                        foundHosts.addAll(deferreds.awaitAll().filterNotNull())
                    }

                    activeHosts = foundHosts
                    statusMessage = "Scan complete. Found ${foundHosts.size} active hosts with open web ports."
                    isScanning = false
                }
            },
            enabled = !isScanning
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isScanning) "Scanning..." else "Scan Subnet")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        if (activeHosts.isNotEmpty()) {
            Text("Discovered Hosts", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeHosts) { ip ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = ip, fontWeight = FontWeight.SemiBold)
                            Button(onClick = {
                            }) {
                                Text("Probe Ports")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getSubnetBaseIp(context: Context): String? {
    try {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            val linkProperties = connectivityManager.getLinkProperties(network) ?: return null
            for (linkAddress in linkProperties.linkAddresses) {
                val address = linkAddress.address
                if (address is java.net.Inet4Address && !address.isLoopbackAddress) {
                    val ipBytes = address.address
                    return "${ipBytes[0].toInt() and 0xFF}.${ipBytes[1].toInt() and 0xFF}.${ipBytes[2].toInt() and 0xFF}"
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}
