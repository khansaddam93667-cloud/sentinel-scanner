package com.example.secscanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.pm.PackageInfo

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val scannerEngine = ScannerEngine(application.packageManager)
    private val masvsScannerEngine = MasvsScannerEngine(application)
    val screenMonitor = ScreenRecordingMonitor(application)

    private val _appReports = MutableStateFlow<List<AppRiskReport>>(emptyList())
    val appReports: StateFlow<List<AppRiskReport>> = _appReports.asStateFlow()

    private val _masvsReports = MutableStateFlow<List<MasvsControlResult>>(emptyList())
    val masvsReports: StateFlow<List<MasvsControlResult>> = _masvsReports.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _overlayApps = MutableStateFlow<List<PackageInfo>>(emptyList())
    val overlayApps: StateFlow<List<PackageInfo>> = _overlayApps.asStateFlow()

    init {
        fetchOverlayApps()
    }

    fun fetchOverlayApps() {
        viewModelScope.launch {
            _overlayApps.value = scannerEngine.getOverlayApps()
        }
    }

    val isScreenRecording = screenMonitor.isRecording

    private val _isSecureModeEnabled = MutableStateFlow(false)
    val isSecureModeEnabled: StateFlow<Boolean> = _isSecureModeEnabled.asStateFlow()

    fun toggleSecureMode(enabled: Boolean) {
        _isSecureModeEnabled.value = enabled
    }

    fun scanDevice() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val reports = scannerEngine.scanApps()
                _appReports.value = reports

                val masvsResults = masvsScannerEngine.runChecks()
                _masvsReports.value = masvsResults
            } finally {
                _isScanning.value = false
            }
        }
    }
}
