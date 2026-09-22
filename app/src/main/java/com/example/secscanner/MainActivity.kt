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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Terminal
import android.content.pm.PackageInfo
import androidx.compose.runtime.DisposableEffect
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
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Tools") },
                    label = { Text("Tools") }
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (currentTab) {
                0 -> DashboardTab(healthScore, isRecording, isSecureModeEnabled, viewModel, isScanning, masvsReports)
                1 -> AuditorTab(reports, isScanning, { viewModel.scanDevice() })
                2 -> DeviceRaspTab()
                3 -> ToolsTab(viewModel)
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

fun AuditorTab(reports: List<AppRiskReport>, isScanning: Boolean, onScanDevice: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                placeholder = { Text("Search apps...") },
                singleLine = true
            )
            Button(onClick = { if (!isScanning) onScanDevice() }) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isScanning) "Scanning..." else "Run Scan")
            }
        }
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

    val refreshRate = try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            context.display?.mode?.refreshRate ?: 60f
        } else {
            60f
        }
    } catch (e: Throwable) { 60f }

    val isDebuggerConnected = try {
        android.os.Debug.isDebuggerConnected()
    } catch (e: Throwable) { false }

    val isRooted = try {
        val paths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/app/Superuser.apk")
        paths.any { path -> try { java.io.File(path).exists() } catch (e: Throwable) { false } }
    } catch (e: Throwable) { false }

    val isDeveloperModeEnabled = try {
        android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
    } catch (e: Throwable) { false }

    val networkStatus = try {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNet)

        val isVpn = try { caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true } catch (e: Throwable) { false }
        val isWifi = try { caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true } catch (e: Throwable) { false }
        val isCellular = try { caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true } catch (e: Throwable) { false }

        when {
            isVpn -> "VPN Active"
            isWifi -> "Wi-Fi"
            isCellular -> "Cellular"
            else -> "No Active Network"
        }
    } catch (e: Throwable) {
        "Unavailable"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 100.dp)
            .verticalScroll(rememberScrollState())
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

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = if (isDeveloperModeEnabled) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Developer Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Status: ${if (isDeveloperModeEnabled) "Enabled (Danger)" else "Disabled"}")
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

@Composable
fun ToolsTab(viewModel: MainViewModel) {
    val overlayApps by viewModel.overlayApps.collectAsState()
    val appReports by viewModel.appReports.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    androidx.compose.material3.Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Security & Telemetry Tools",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item {
                PrivacyWipeTool(viewModel, snackbarHostState)
            }
            item {
                SocketLatencyTool()
            }
            item {
                CameraDiagnosticCard()
            }
            item {
                IpcSandboxCard(appReports)
            }
            item {
                StegoCryptTool()
            }
            item {
                ParticleBenchmarkTool()
            }
            item {
                PayloadSanitizerTool()
            }
            item {
                BatteryTelemetryCard()
            }
            item {
                JwtInspectorCard()
            }
            item {
                SslCertAuditorCard()
            }
            item {
                HashGeneratorCard()
            }
            item {
                OverlaySentryCard(overlayApps)
            }
        }
    }
}
@Composable
fun BatteryTelemetryCard() {
    val context = LocalContext.current
    var voltage by remember { mutableStateOf(0) }
    var temperature by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf(-1) }
    var plugType by remember { mutableStateOf(-1) }
    var level by remember { mutableStateOf(0) }
    var scale by remember { mutableStateOf(100) }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == android.content.Intent.ACTION_BATTERY_CHANGED) {
                    voltage = intent.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0)
                    temperature = intent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0)
                    status = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                    plugType = intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1)
                    level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                    scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                }
            }
        }
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val vStr = String.format("%.2f V", voltage / 1000f)
    val tStr = String.format("%.1f °C", temperature / 10f)
    val chargingStatus = when (status) {
        android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        android.os.BatteryManager.BATTERY_STATUS_FULL -> "Full"
        android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
        else -> "Unknown"
    }
    val plugStatus = when (plugType) {
        android.os.BatteryManager.BATTERY_PLUGGED_AC -> "AC"
        android.os.BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        else -> "Unplugged"
    }
    val batteryPct = if (scale > 0) level * 100 / scale.toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "SuperVOOC & Thermal Telemetry Scope", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Voltage: $vStr")
            Text(text = "Temperature: $tStr")
            Text(text = "Status: $chargingStatus ($plugStatus)")
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { batteryPct / 100f }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun OverlaySentryCard(overlayApps: List<PackageInfo>) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Overlay & Tapjacking Sentry", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (overlayApps.isEmpty()) {
                Text(text = "No apps with SYSTEM_ALERT_WINDOW permission found.")
            } else {
                overlayApps.forEach { pkg ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = pkg.packageName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Button(onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${pkg.packageName}"))
                            context.startActivity(intent)
                        }) {
                            Text("Manage")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IpcSandboxCard(appReports: List<AppRiskReport>) {
    val context = LocalContext.current
    var selectedReport by remember { mutableStateOf<AppRiskReport?>(null) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Safe Intent & IPC Sandbox", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val appsWithExported = appReports.filter { it.exportedComponents.isNotEmpty() }
            if (appsWithExported.isEmpty()) {
                 Text("No apps with exported components found.")
            } else {
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(appsWithExported) { report ->
                        Text(
                            text = report.packageName,
                            modifier = Modifier.fillMaxWidth().clickable { selectedReport = report }.padding(8.dp),
                            color = if (selectedReport == report) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                selectedReport?.let { report ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Selected: ${report.packageName}", fontWeight = FontWeight.SemiBold)
                    report.exportedComponents.forEach { component ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = component, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Button(onClick = {
                                try {
                                    val intent = android.content.Intent()
                                    if (component.startsWith("Activity: ")) {
                                        intent.setClassName(report.packageName, component.removePrefix("Activity: "))
                                        context.startActivity(intent)
                                    }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Launch failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text("Test Launch")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraDiagnosticCard() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var cameraIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var isViewfinderActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val cameraManager = context.getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        try {
            cameraIds = cameraManager.cameraIdList.toList()
        } catch (e: Exception) {
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Hardware Camera Diagnostic", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Available Camera IDs: ${cameraIds.joinToString(", ")}")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { isViewfinderActive = !isViewfinderActive }) {
                Text(if (isViewfinderActive) "Close Viewfinder" else "Launch Viewfinder")
            }
            if (isViewfinderActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            androidx.camera.view.PreviewView(ctx).apply {
                                val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = androidx.camera.core.Preview.Builder().build().also {
                                        it.setSurfaceProvider(surfaceProvider)
                                    }
                                    val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                                    } catch(exc: Exception) {
                                    }
                                }, androidx.core.content.ContextCompat.getMainExecutor(ctx))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(color = Color.Red, start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f), end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height), strokeWidth = 2f)
                        drawLine(color = Color.Red, start = androidx.compose.ui.geometry.Offset(0f, size.height / 2), end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2), strokeWidth = 2f)
                    }
                }
            }
        }
    }
}
