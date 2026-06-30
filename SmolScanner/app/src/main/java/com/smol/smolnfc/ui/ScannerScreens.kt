package com.smol.smolnfc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smol.smolnfc.scanner.BleDevice
import com.smol.smolnfc.scanner.Vendors
import com.smol.smolnfc.scanner.WifiNetwork

@Composable
private fun PermissionGate(message: String, onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = onRequest) { Text("Grant permission") }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text(
            subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun rssiBucket(rssi: Int): String = when {
    rssi >= -55 -> "very close"
    rssi >= -70 -> "close"
    rssi >= -85 -> "nearby"
    else -> "far"
}

private fun band(freq: Int): String = when {
    freq in 2400..2500 -> "2.4 GHz"
    freq in 4900..5900 -> "5 GHz"
    freq in 5925..7125 -> "6 GHz"
    else -> "$freq MHz"
}

@Composable
fun BluetoothScreen(
    devices: List<BleDevice>,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit
) {
    if (!permissionsGranted) {
        PermissionGate("Bluetooth scanning needs nearby-devices and location permission.", onRequestPermissions)
        return
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Bluetooth", "Devices broadcasting near you. If empty, turn on Bluetooth + Location.")
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(devices, key = { it.address }) { d ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                d.name ?: "(no name)",
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("${d.rssi} dBm", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            d.address,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val vendor = Vendors.name(d.manufacturerId)
                        Text(
                            listOfNotNull(rssiBucket(d.rssi), vendor).joinToString("  •  "),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WifiScreen(
    networks: List<WifiNetwork>,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit
) {
    if (!permissionsGranted) {
        PermissionGate("WiFi scanning needs location permission and Location turned on.", onRequestPermissions)
        return
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("WiFi", "Networks around you. Names and signal only — never passwords.")
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                    Text("Refresh scan")
                }
            }
            items(networks) { n ->
                val secured = n.capabilities.contains("WPA") || n.capabilities.contains("WEP")
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                n.ssid,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("${n.rssi} dBm", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            n.bssid,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            listOf(band(n.frequency), rssiBucket(n.rssi), if (secured) "secured" else "open").joinToString("  •  "),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetalDetectorScreen(magnitude: Float, hasSensor: Boolean) {
    if (!hasSensor) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("This device has no magnetometer.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val earthMax = 65f
    val spike = (magnitude - earthMax).coerceAtLeast(0f)
    val detected = magnitude > 90f
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Magnetic field", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("%.1f µT".format(magnitude), fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(
            progress = { (magnitude / 300f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        Text(
            if (detected) "Metal / magnet detected" else "Normal — Earth's field is ~25–65 µT",
            color = if (detected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (detected) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Move the top of the phone near an object. Spike above baseline: %.0f µT".format(spike),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TrackerScreen(
    devices: List<BleDevice>,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit
) {
    if (!permissionsGranted) {
        PermissionGate("Tracker detection scans Bluetooth, which needs nearby-devices and location permission.", onRequestPermissions)
        return
    }
    val sorted = devices.sortedByDescending { it.lastSeen - it.firstSeen }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Tracker check", "Sorted by how long each device has stayed near you.")
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        "This is a hint, not proof. Trackers rotate their IDs, so this can't reliably identify one. For real alerts use Android Settings → Safety & emergency → Unknown tracker alerts.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            items(sorted, key = { it.address }) { d ->
                val persistedSec = ((d.lastSeen - d.firstSeen) / 1000).toInt()
                val vendor = Vendors.name(d.manufacturerId)
                val flag = Vendors.isPossibleTrackerVendor(d.manufacturerId) && persistedSec > 300
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                d.name ?: "(no name)",
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (flag) {
                                Text("watch", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Text(
                            d.address,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            listOfNotNull("near you ${persistedSec}s", vendor).joinToString("  •  "),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
