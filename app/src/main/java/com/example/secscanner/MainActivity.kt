package com.example.secscanner

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.background

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings as AndroidSettings
import android.net.Uri
import com.example.secscanner.ui.theme.SecScannerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel.screenMonitor)

        setContent {

            val isSecureModeEnabled by viewModel.isSecureModeEnabled.collectAsState()

            LaunchedEffect(isSecureModeEnabled) {
                if (isSecureModeEnabled) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            SecScannerTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val isSecureModeEnabled by viewModel.isSecureModeEnabled.collectAsState()

    val isRecording by viewModel.isScreenRecording.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val reports by viewModel.appReports.collectAsState()
    val masvsReports by viewModel.masvsReports.collectAsState()

    val healthScore = if (masvsReports.isEmpty()) 100 else {
        val passed = masvsReports.count { it.result is MasvsResult.Pass }
        (passed.toFloat() / masvsReports.size * 100).toInt()
    }

    var currentTab by remember { mutableStateOf(0) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!isScanning) viewModel.scanDevice() },
                content = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(if (isScanning) "Scanning..." else "Run Scan")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Rounded.List, contentDescription = "Auditor") },
                    label = { Text("Auditor") }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Warning, contentDescription = "Device & RASP") },
                    label = { Text("Device & RASP") }
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (currentTab) {
                0 -> DashboardTab(healthScore, isRecording, isSecureModeEnabled, viewModel, isScanning, masvsReports)
                1 -> AuditorTab(reports)
                2 -> DeviceRaspTab()
            }
        }
    }
}

@Composable
fun DashboardTab(
    healthScore: Int,
    isRecording: Boolean,
    isSecureModeEnabled: Boolean,
    viewModel: MainViewModel,
    isScanning: Boolean,
    masvsReports: List<MasvsControlResult>
) {

            SecurityHealthScoreCard(score = healthScore)

            StatusCard(isRecording, isSecureModeEnabled)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Secure Mode (Block Screenshots)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = isSecureModeEnabled,
                    onCheckedChange = { viewModel.toggleSecureMode(it) }
                )
            }

            if (isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (masvsReports.isNotEmpty()) {
                        val groupedReports = masvsReports.groupBy { report ->
                            when {
                                report.controlName.contains("STORAGE", ignoreCase = true) -> "Storage"
                                report.controlName.contains("NETWORK", ignoreCase = true) -> "Network"
                                report.controlName.contains("RESILIENCE", ignoreCase = true) -> "Resilience"
                                report.controlName.contains("CODE", ignoreCase = true) -> "Platform"
                                else -> "Other Checks"
                            }
                        }

                        item {
                            Text(
                                text = "Baseline Security Controls",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }

                        groupedReports.forEach { (category, reports) ->
                            item {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(reports) { report ->
                                MasvsReportItem(report)
                            }
                        }

                        item {
                            Divider(modifier = Modifier.padding(vertical = 12.dp))
                            Text(
                                text = "App Risks",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }


@Composable
fun AuditorTab(reports: List<AppRiskReport>) {
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search apps...") },
            singleLine = true
        )

        val filteredReports = reports.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(filteredReports) { report ->
                AppReportItem(report)
            }
        }
    }
}

@Composable
fun DeviceRaspTab() {
    val context = LocalContext.current
    val view = LocalView.current
    val refreshRate = view.display?.refreshRate ?: 0f
    val isDebuggerConnected = android.os.Debug.isDebuggerConnected()
    val isRooted = java.io.File("/system/bin/su").exists()

    val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)

    val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    val isVpn = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

    val networkStatus = when {
        isVpn -> "VPN Active"
        isWifi -> "Wi-Fi"
        isCellular -> "Cellular"
        else -> "No Active Network"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Device & RASP Status", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Display", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Refresh Rate: ${refreshRate}Hz")
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = if (isDebuggerConnected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Debugger", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Status: ${if (isDebuggerConnected) "Connected (Danger)" else "Not Connected"}")
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = if (isRooted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Root Presence", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Status: ${if (isRooted) "Root Found (su binary exists)" else "Not Rooted"}")
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Network Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Active Network: $networkStatus")
            }
        }
    }
}

@Composable
fun StatusCard(isRecording: Boolean, isSecureModeEnabled: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "flash")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .alpha(alpha),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) MaterialTheme.colorScheme.errorContainer
                             else MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (isRecording) "Screen Recording: Active!" else "Screen Recording: Inactive",
                color = if (isRecording) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onTertiaryContainer,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isSecureModeEnabled) "App Security: Enforced" else "App Security: Not Enforced",
                color = if (isRecording) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun MasvsReportItem(report: MasvsControlResult) {
    val isPass = report.result is MasvsResult.Pass
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.controlName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Badge(
                    containerColor = if (isPass) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (isPass) "PASS" else "FAIL",
                        color = if (isPass) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    if (isPass) {
                         Text(
                            text = "Check passed successfully. No issues found.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = (report.result as MasvsResult.Fail).reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppReportItem(report: AppRiskReport) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = report.appName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(text = report.packageName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Badge(
                    containerColor = if (report.score > 0) Color.Red else Color.Green
                ) {
                    Text(
                        text = "Score: ${report.score}",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    if (report.risks.isEmpty()) {
                        Text(text = "No risks detected.", color = Color.Green, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(text = "Vulnerabilities Found:", fontWeight = FontWeight.SemiBold, color = Color.Red, style = MaterialTheme.typography.bodyMedium)
                        report.risks.forEach { risk ->
                            Text(text = "• $risk", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                        }
                    }

                    if (report.exportedComponents.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(text = "Exported Components:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                        report.exportedComponents.forEach { component ->
                            Text(text = "• $component", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                        }
                    }

                    if (report.dangerousPermissions.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(text = "Dangerous Permissions:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        report.dangerousPermissions.forEach { permission ->
                            Text(text = "• $permission", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", report.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Settings")
                    }
                }
            }
        }
    }
}
@Composable
fun SecurityHealthScoreCard(score: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Security Health",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Overall Device Score",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = score / 100f,
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 8.dp,
                    color = if (score >= 80) Color.Green else if (score >= 50) Color.Yellow else Color.Red,
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
                Text(
                    text = "${score}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
