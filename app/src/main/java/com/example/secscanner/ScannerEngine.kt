package com.example.secscanner

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppRiskReport(
    val packageName: String,
    val appName: String,
    val score: Int,
    val risks: List<String>,
    val exportedComponents: List<String>,
    val dangerousPermissions: List<String>
)

class ScannerEngine(private val packageManager: PackageManager) {

    suspend fun getOverlayApps(): List<PackageInfo> = withContext(Dispatchers.IO) {
        val installedPackages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        installedPackages.filter { pkg ->
            pkg.requestedPermissions?.contains("android.permission.SYSTEM_ALERT_WINDOW") == true
        }
    }

    suspend fun scanApps(): List<AppRiskReport> = withContext(Dispatchers.IO) {
        val installedPackages = packageManager.getInstalledPackages(
            PackageManager.GET_PERMISSIONS or
            PackageManager.GET_ACTIVITIES or
            PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS
        )

        installedPackages
            .filter { (it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) == 0 }
            .map { pkgInfo ->
                analyzePackage(pkgInfo)
            }
            .sortedByDescending { it.score }
    }

    private fun analyzePackage(packageInfo: PackageInfo): AppRiskReport {
        val risks = mutableListOf<String>()
        var score = 0
        val appInfo = packageInfo.applicationInfo
        val exportedComponents = mutableListOf<String>()

        // 1. Exported components
        var hasExported = false
        packageInfo.activities?.forEach { if (it.exported) { hasExported = true; exportedComponents.add("Activity: ${it.name}") } }
        packageInfo.services?.forEach { if (it.exported) { hasExported = true; exportedComponents.add("Service: ${it.name}") } }
        packageInfo.receivers?.forEach { if (it.exported) { hasExported = true; exportedComponents.add("Receiver: ${it.name}") } }
        if (hasExported) {
            risks.add("Exported Components Found")
            score += 2
        }

        // 2. Cleartext HTTP
        if (appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0) {
            risks.add("Cleartext HTTP Traffic Allowed")
            score += 3
        }

        // 3. App backup
        if (appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0) {
            risks.add("App Backup Enabled")
            score += 1
        }

        // 4. High-risk permissions
        val highRiskPerms = listOf(
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.RECORD_AUDIO",
            "android.permission.SYSTEM_ALERT_WINDOW"
        )
        val requestedPerms = packageInfo.requestedPermissions ?: emptyArray()
        val foundHighRiskPerms = requestedPerms.filter { it in highRiskPerms }
        if (foundHighRiskPerms.isNotEmpty()) {
            risks.add("High-Risk Permissions Requested")
            score += 4
        }

        val appName = appInfo?.loadLabel(packageManager)?.toString() ?: packageInfo.packageName

        return AppRiskReport(
            packageName = packageInfo.packageName,
            appName = appName,
            score = score,
            risks = risks,
            exportedComponents = exportedComponents,
            dangerousPermissions = foundHighRiskPerms
        )
    }
}
