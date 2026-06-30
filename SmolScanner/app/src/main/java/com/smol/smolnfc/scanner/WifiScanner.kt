package com.smol.smolnfc.scanner

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiScanner(context: Context) {

    private val appContext = context.applicationContext
    private val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val _networks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val networks = _networks.asStateFlow()

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            publish()
        }
    }

    fun start() {
        if (!registered) {
            val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            registered = true
        }
        triggerScan()
        publish()
    }

    fun stop() {
        if (registered) {
            try {
                appContext.unregisterReceiver(receiver)
            } catch (e: Exception) {
            }
            registered = false
        }
    }

    @Suppress("DEPRECATION")
    fun triggerScan() {
        try {
            wifi?.startScan()
        } catch (e: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun publish() {
        val results = try {
            wifi?.scanResults ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
        _networks.value = results.map { r ->
            val ssid = r.SSID
            WifiNetwork(
                ssid = if (ssid.isNullOrBlank()) "(hidden)" else ssid,
                bssid = r.BSSID ?: "",
                rssi = r.level,
                frequency = r.frequency,
                capabilities = r.capabilities ?: ""
            )
        }.sortedByDescending { it.rssi }
    }
}
