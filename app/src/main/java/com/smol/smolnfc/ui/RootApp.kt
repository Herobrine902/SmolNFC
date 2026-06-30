package com.smol.smolnfc.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.smol.smolnfc.data.StoredCard
import com.smol.smolnfc.scanner.BleDevice
import com.smol.smolnfc.scanner.WifiNetwork

@Composable
fun RootApp(
    cards: List<StoredCard>,
    activeEmulationId: String?,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onSetEmulating: (StoredCard?) -> Unit,
    bleDevices: List<BleDevice>,
    wifiNetworks: List<WifiNetwork>,
    magnitude: Float,
    hasMagnetometer: Boolean,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onRefreshWifi: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf(
        "\uD83E\uDEAA" to "NFC",
        "\uD83D\uDCF6" to "BT",
        "\uD83D\uDCE1" to "WiFi",
        "\uD83E\uDDF2" to "Metal",
        "\uD83C\uDFAF" to "Tracker"
    )
    SmolTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { i, (emoji, label) ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Text(emoji, fontSize = 18.sp) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                when (tab) {
                    0 -> NfcScreen(cards, activeEmulationId, onRename, onDelete, onSetEmulating)
                    1 -> BluetoothScreen(bleDevices, permissionsGranted, onRequestPermissions)
                    2 -> WifiScreen(wifiNetworks, permissionsGranted, onRequestPermissions, onRefreshWifi)
                    3 -> MetalDetectorScreen(magnitude, hasMagnetometer)
                    else -> TrackerScreen(bleDevices, permissionsGranted, onRequestPermissions)
                }
            }
        }
    }
}
