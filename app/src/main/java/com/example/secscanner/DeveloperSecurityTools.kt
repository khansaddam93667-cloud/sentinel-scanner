package com.example.secscanner

import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection
import java.security.cert.X509Certificate

@Composable
fun JwtInspectorCard() {
    var jwtString by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "JWT Token Inspector", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = jwtString,
                onValueChange = { jwtString = it },
                label = { Text("Paste JWT here") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                try {
                    val parts = jwtString.split(".")
                    if (parts.size != 3) {
                        resultText = "Invalid JWT Format (Requires 3 segments)"
                        return@Button
                    }

                    val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
                    val json = JSONObject(payload)

                    val iss = json.optString("iss", "N/A")
                    val sub = json.optString("sub", "N/A")
                    val exp = json.optLong("exp", 0L)

                    val expStr = if (exp > 0) {
                        val isExpired = (System.currentTimeMillis() / 1000) > exp
                        val expiryStatus = if (isExpired) "Expired" else "Valid"
                        "$exp ($expiryStatus)"
                    } else {
                        "N/A"
                    }

                    resultText = "Issuer: $iss\nSubject: $sub\nExpiration: $expStr\n\nPayload JSON:\n${json.toString(2)}"
                } catch (e: Exception) {
                    resultText = "Error parsing JWT: ${e.message}"
                }
            }) {
                Text("Inspect Token")
            }
            if (resultText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = resultText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun SslCertAuditorCard() {
    var hostname by remember { mutableStateOf("") }
    var certInfo by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "SSL/TLS Certificate Auditor", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = hostname,
                onValueChange = { hostname = it },
                label = { Text("Hostname (e.g. github.com)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (hostname.isBlank()) return@Button
                certInfo = "Fetching certificate for $hostname..."
                coroutineScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        try {
                            val host = if (!hostname.startsWith("http")) "https://$hostname" else hostname
                            val url = URL(host)
                            val connection = url.openConnection() as HttpsURLConnection
                            connection.connectTimeout = 5000
                            connection.readTimeout = 5000
                            connection.connect()

                            val certs = connection.serverCertificates
                            if (certs.isNotEmpty() && certs[0] is X509Certificate) {
                                val cert = certs[0] as X509Certificate

                                val md = MessageDigest.getInstance("SHA-256")
                                val fingerprint = md.digest(cert.encoded).joinToString(":") { "%02X".format(it) }

                                """
                                Subject: ${cert.subjectDN}
                                Issuer: ${cert.issuerDN}
                                Valid From: ${cert.notBefore}
                                Valid Until: ${cert.notAfter}
                                SHA-256: $fingerprint
                                """.trimIndent()
                            } else {
                                "No X.509 certificate found."
                            }
                        } catch (e: Exception) {
                            "Error fetching cert: ${e.message}"
                        }
                    }
                    certInfo = result
                }
            }) {
                Text("Inspect Cert")
            }
            if (certInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = certInfo, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun HashGeneratorCard() {
    var input by remember { mutableStateOf("") }

    val hashes = remember(input) {
        if (input.isEmpty()) return@remember emptyMap<String, String>()
        val bytes = input.toByteArray()
        val md5 = MessageDigest.getInstance("MD5").digest(bytes).joinToString("") { "%02x".format(it) }
        val sha1 = MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }
        val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        mapOf("MD5" to md5, "SHA-1" to sha1, "SHA-256" to sha256)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Cryptographic Checksum & Hash Generator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Input text to hash") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (hashes.isNotEmpty()) {
                hashes.forEach { (algo, hash) ->
                    Text(text = "$algo: $hash", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}
