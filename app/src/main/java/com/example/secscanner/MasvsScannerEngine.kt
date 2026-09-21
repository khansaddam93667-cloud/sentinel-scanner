package com.example.secscanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.security.NetworkSecurityPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore

sealed class MasvsResult {
    object Pass : MasvsResult()
    data class Fail(val reason: String) : MasvsResult()
}

data class MasvsControlResult(val controlName: String, val result: MasvsResult)

interface MasvsChecker {
    val controlName: String
    suspend fun check(context: Context): MasvsControlResult
}

class StorageChecker : MasvsChecker {
    override val controlName = "MASVS-STORAGE (Keystore)"

    override suspend fun check(context: Context): MasvsControlResult = withContext(Dispatchers.IO) {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val aliases = keyStore.aliases()
            if (aliases.hasMoreElements()) {
                MasvsControlResult(controlName, MasvsResult.Pass)
            } else {
                MasvsControlResult(controlName, MasvsResult.Fail("No keys found in AndroidKeyStore (Preferences may be unencrypted)"))
            }
        } catch (e: Exception) {
            MasvsControlResult(controlName, MasvsResult.Fail("Failed to access Keystore: ${e.message}"))
        }
    }
}

class ResilienceChecker : MasvsChecker {
    override val controlName = "MASVS-RESILIENCE (Root Detection)"

    override suspend fun check(context: Context): MasvsControlResult = withContext(Dispatchers.IO) {
        val rootIndicators = mutableListOf<String>()

        // 1. Check for test-keys
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            rootIndicators.add("Test-keys detected")
        }

        // 2. Check for su binaries
        val suPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in suPaths) {
            if (File(path).exists()) {
                rootIndicators.add("su binary found at $path")
            }
        }

        // 3. Check for known root management apps
        val rootApps = arrayOf(
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.zachspong.temprootremovejb",
            "com.ramdroid.appquarantine",
            "com.topjohnwu.magisk"
        )
        val pm = context.packageManager
        for (pkg in rootApps) {
            try {
                pm.getPackageInfo(pkg, 0)
                rootIndicators.add("Root management app found: $pkg")
            } catch (e: Exception) {
                // Not installed
            }
        }

        if (rootIndicators.isNotEmpty()) {
            MasvsControlResult(controlName, MasvsResult.Fail(rootIndicators.joinToString(", ")))
        } else {
            MasvsControlResult(controlName, MasvsResult.Pass)
        }
    }
}

class NetworkChecker : MasvsChecker {
    override val controlName = "MASVS-NETWORK (Cleartext Traffic)"

    override suspend fun check(context: Context): MasvsControlResult = withContext(Dispatchers.IO) {
        val isCleartextPermitted = NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted
        if (isCleartextPermitted) {
            MasvsControlResult(controlName, MasvsResult.Fail("Cleartext traffic is permitted in app configuration"))
        } else {
            MasvsControlResult(controlName, MasvsResult.Pass)
        }
    }
}

class CodeChecker : MasvsChecker {
    override val controlName = "MASVS-CODE (Debuggable)"

    override suspend fun check(context: Context): MasvsControlResult = withContext(Dispatchers.IO) {
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            MasvsControlResult(controlName, MasvsResult.Fail("App is debuggable"))
        } else {
            MasvsControlResult(controlName, MasvsResult.Pass)
        }
    }
}

class MasvsScannerEngine(private val context: Context) {
    private val checkers = listOf(
        StorageChecker(),
        ResilienceChecker(),
        NetworkChecker(),
        CodeChecker()
    )

    suspend fun runChecks(): List<MasvsControlResult> = withContext(Dispatchers.IO) {
        checkers.map { it.check(context) }
    }
}
