package com.example.secscanner

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ExifStripperTool() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var exifData by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var statusMessage by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    val data = mutableMapOf<String, String>()

                    exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)?.let { lat ->
                        data["GPS Latitude"] = lat
                    }
                    exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)?.let { lon ->
                        data["GPS Longitude"] = lon
                    }
                    exif.getAttribute(ExifInterface.TAG_MODEL)?.let { model ->
                        data["Camera Model"] = model
                    }
                    exif.getAttribute(ExifInterface.TAG_DATETIME)?.let { dt ->
                        data["DateTime"] = dt
                    }

                    if (data.isEmpty()) {
                        data["Status"] = "No standard GPS/Camera tags found."
                    }
                    exifData = data
                }
            } catch (e: Exception) {
                exifData = mapOf("Error" to (e.message ?: "Failed to read EXIF"))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { launcher.launch("image/jpeg") }) {
            Text("Import Photo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (imageUri != null) {
            Text("Image Selected: ${imageUri?.lastPathSegment}", fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Extracted Metadata", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (exifData.isEmpty()) {
                        Text("No EXIF data found or not extracted yet.")
                    } else {
                        exifData.forEach { (key, value) ->
                            Text("$key: $value", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                coroutineScope.launch {
                    try {
                        statusMessage = "Sanitizing..."
                        context.contentResolver.openInputStream(imageUri!!)?.use { inputStream ->
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            if (bitmap != null) {
                                val outDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                                val outFile = File(outDir, "sanitized_${System.currentTimeMillis()}.jpg")
                                FileOutputStream(outFile).use { outStream ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
                                }
                                statusMessage = "Saved clean copy to: ${outFile.absolutePath}"
                            } else {
                                statusMessage = "Failed to decode image."
                            }
                        }
                    } catch (e: Exception) {
                        statusMessage = "Error: ${e.message}"
                    }
                }
            }) {
                Text("Sanitize & Save Copy")
            }
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(statusMessage, color = MaterialTheme.colorScheme.primary)
        }
    }
}
