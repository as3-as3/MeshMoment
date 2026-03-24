package com.as3as3.meshmoment.feature.radar

import android.bluetooth.le.ScanResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.as3as3.meshmoment.core.connectivity.MeshManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RadarViewModel(private val meshManager: MeshManager) : ViewModel() {

    val discoveredNodes: StateFlow<List<ScanResult>> = meshManager.foundDevices
        .map { it.toList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun startScanning() {
        meshManager.start()
    }

    fun stopScanning() {
        meshManager.stop()
    }
    
    fun sendMessage(result: ScanResult, message: String) {
        meshManager.broadcastMessage(message)
    }
}
