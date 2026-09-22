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
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

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

                    items(reports) { report ->
                        AppReportItem(report)
                    }
                }
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
