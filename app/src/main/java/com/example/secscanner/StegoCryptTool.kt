package com.example.secscanner

import android.util.Base64
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

@Composable
fun StegoCryptTool() {
    var message by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    CyberCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "StegoCrypt Engine (AES-GCM)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Secret Message") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    try {
                        result = encryptAesGcm(message, password)
                    } catch (e: Exception) {
                        result = "Encryption failed: ${e.message}"
                    }
                }, modifier = Modifier.weight(1f)) {
                    Text("Encrypt")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    try {
                        result = decryptAesGcm(message, password)
                    } catch (e: Exception) {
                        result = "Decryption failed: ${e.message}"
                    }
                }, modifier = Modifier.weight(1f)) {
                    Text("Decrypt")
                }
            }
            if (result.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = result,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Result") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

fun getSecretKeySpec(password: String): SecretKeySpec {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
    return SecretKeySpec(bytes, "AES")
}

fun encryptAesGcm(plaintext: String, password: String): String {
    val iv = ByteArray(12)
    SecureRandom().nextBytes(iv)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val gcmSpec = GCMParameterSpec(128, iv)
    val secretKeySpec = getSecretKeySpec(password)
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec)
    val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

    val combined = iv + ciphertext
    return Base64.encodeToString(combined, Base64.NO_WRAP)
}

fun decryptAesGcm(base64Ciphertext: String, password: String): String {
    val combined = Base64.decode(base64Ciphertext, Base64.NO_WRAP)
    val iv = combined.copyOfRange(0, 12)
    val ciphertext = combined.copyOfRange(12, combined.size)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val gcmSpec = GCMParameterSpec(128, iv)
    val secretKeySpec = getSecretKeySpec(password)
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec)

    val plaintext = cipher.doFinal(ciphertext)
    return String(plaintext, Charsets.UTF_8)
}
