package com.example.secscanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val scannerEngine = ScannerEngine(application.packageManager)
    private val screenMonitor = ScreenRecordingMonitor(application)

    private val _appReports = MutableStateFlow<List<AppRiskReport>>(emptyList())
    val appReports: StateFlow<List<AppRiskReport>> = _appReports.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val isScreenRecording = screenMonitor.isRecording

    init {
        screenMonitor.startMonitoring()
    }

    override fun onCleared() {
        super.onCleared()
        screenMonitor.stopMonitoring()
    }

    fun scanDevice() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val reports = scannerEngine.scanApps()
                _appReports.value = reports
            } finally {
                _isScanning.value = false
            }
        }
    }
}
