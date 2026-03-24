package com.as3as3.meshmoment.core.connectivity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BleScannerManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bleScanner = bluetoothAdapter?.bluetoothLeScanner

    private val _foundDevices = MutableStateFlow<Set<ScanResult>>(emptySet())
    val foundDevices: StateFlow<Set<ScanResult>> = _foundDevices.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val currentDevices = _foundDevices.value.toMutableSet()
            currentDevices.add(result)
            _foundDevices.value = currentDevices
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            val currentDevices = _foundDevices.value.toMutableSet()
            currentDevices.addAll(results)
            _foundDevices.value = currentDevices
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleScannerManager", "Scan failed with error: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning(serviceUuid: UUID) {
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(serviceUuid))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bleScanner?.startScan(listOf(filter), settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        bleScanner?.stopScan(scanCallback)
    }
}