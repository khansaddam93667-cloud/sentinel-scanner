package com.example.secscanner

import android.content.Context
import android.os.Build
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.function.Consumer

class ScreenRecordingMonitor(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val callback = Consumer<Int> { state ->
        if (Build.VERSION.SDK_INT >= 35) {
            _isRecording.value = state == WindowManager.SCREEN_RECORDING_STATE_ACTIVE
        }
    }

    fun startMonitoring() {
        if (Build.VERSION.SDK_INT >= 35) {
            try {
                windowManager.addScreenRecordingCallback(context.mainExecutor, callback)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= 35) {
            try {
                windowManager.removeScreenRecordingCallback(callback)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
