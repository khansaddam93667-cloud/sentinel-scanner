package com.example.secscanner

import android.util.Base64
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PayloadSanitizerTool() {
    var payload by remember { mutableStateOf("") }

    CyberCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Payload Sanitizer & Defanger", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = payload,
                onValueChange = { payload = it },
                label = { Text("URL / Payload") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = {
                    payload = payload.replace("http://", "hxxp://").replace("https://", "hxxps://").replace(".", "[.]")
                }, modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)) {
                    Text("Defang URL")
                }
                Button(onClick = {
                    try {
                        payload = Base64.encodeToString(payload.toByteArray(), Base64.NO_WRAP)
                    } catch (e: Exception) {}
                }, modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)) {
                    Text("Base64 Encode")
                }
                Button(onClick = {
                    try {
                        payload = String(Base64.decode(payload, Base64.NO_WRAP))
                    } catch (e: Exception) {}
                }, modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)) {
                    Text("Base64 Decode")
                }
                Button(onClick = {
                    // Simple Regex to strip UTM trackers (and other common trackers)
                    val regex = Regex("([?&])(utm_[^&#=]+|gclid|fbclid)=[^&#]*")
                    var clean = payload
                    while (regex.containsMatchIn(clean)) {
                        clean = regex.replace(clean, "$1")
                    }
                    clean = clean.replace(Regex("([?&])&"), "$1")
                    clean = clean.replace(Regex("[?&]$"), "")
                    payload = clean
                }, modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)) {
                    Text("Strip Trackers")
                }
            }
        }
    }
}
