package com.smol.smolnfc.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleScanner(context: Context) {

    private val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = manager?.adapter

    private val found = LinkedHashMap<String, BleDevice>()
    private val _devices = MutableStateFlow<List<BleDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    private var scanning = false

    fun isBluetoothOn(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun start() {
        if (scanning) return
        val scanner = adapter?.bluetoothLeScanner ?: return
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        try {
            scanner.startScan(null, settings, callback)
            scanning = true
        } catch (e: SecurityException) {
            scanning = false
        } catch (e: IllegalStateException) {
            scanning = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!scanning) return
        try {
            adapter?.bluetoothLeScanner?.stopScan(callback)
        } catch (e: SecurityException) {
        } catch (e: IllegalStateException) {
        }
        scanning = false
    }

    private val callback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val address = device.address ?: return
            val record = result.scanRecord
            val name = try {
                device.name
            } catch (e: SecurityException) {
                null
            } ?: record?.deviceName

            val manufacturerId = record?.manufacturerSpecificData?.let { msd ->
                if (msd.size() > 0) msd.keyAt(0) else null
            }

            val existing = found[address]
            val firstSeen = existing?.firstSeen ?: System.currentTimeMillis()
            found[address] = BleDevice(
                address = address,
                name = name,
                rssi = result.rssi,
                manufacturerId = manufacturerId,
                firstSeen = firstSeen,
                lastSeen = System.currentTimeMillis()
            )
            _devices.value = found.values.sortedByDescending { it.rssi }
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
        }
    }
}
