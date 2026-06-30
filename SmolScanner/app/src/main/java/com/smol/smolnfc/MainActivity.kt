package com.smol.smolnfc

import android.Manifest
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.smol.smolnfc.data.CardViewModel
import com.smol.smolnfc.nfc.NfcReader
import com.smol.smolnfc.scanner.BleScanner
import com.smol.smolnfc.scanner.SensorReader
import com.smol.smolnfc.scanner.WifiScanner
import com.smol.smolnfc.ui.RootApp

class MainActivity : ComponentActivity() {

    private val vm: CardViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null

    private lateinit var bleScanner: BleScanner
    private lateinit var wifiScanner: WifiScanner
    private lateinit var sensorReader: SensorReader

    private val permissionsGranted = mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val granted = requiredPermissions().all { hasPermission(it) }
        permissionsGranted.value = granted
        if (granted) startScanners()
    }

    private val readerFlags =
        NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        val card = NfcReader.parse(tag)
        vm.add(card)
        runOnUiThread {
            Toast.makeText(this, "Scanned ${card.icGuess}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        bleScanner = BleScanner(this)
        wifiScanner = WifiScanner(this)
        sensorReader = SensorReader(this)
        permissionsGranted.value = requiredPermissions().all { hasPermission(it) }

        setContent {
            val cards by vm.cards.collectAsState()
            val active by vm.activeEmulationId.collectAsState()
            val ble by bleScanner.devices.collectAsState()
            val wifi by wifiScanner.networks.collectAsState()
            val mag by sensorReader.magnitude.collectAsState()
            RootApp(
                cards = cards,
                activeEmulationId = active,
                onRename = vm::rename,
                onDelete = vm::delete,
                onSetEmulating = vm::setEmulating,
                bleDevices = ble,
                wifiNetworks = wifi,
                magnitude = mag,
                hasMagnetometer = sensorReader.hasMagnetometer(),
                permissionsGranted = permissionsGranted.value,
                onRequestPermissions = { requestPermissions() },
                onRefreshWifi = { wifiScanner.triggerScan() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(this, readerCallback, readerFlags, null)
        sensorReader.start()
        if (permissionsGranted.value) startScanners()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
        stopScanners()
    }

    private fun startScanners() {
        bleScanner.start()
        wifiScanner.start()
    }

    private fun stopScanners() {
        bleScanner.stop()
        wifiScanner.stop()
        sensorReader.stop()
    }

    private fun requestPermissions() {
        val missing = requiredPermissions().filterNot { hasPermission(it) }
        if (missing.isEmpty()) {
            permissionsGranted.value = true
            startScanners()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun hasPermission(p: String): Boolean =
        ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun requiredPermissions(): List<String> {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms += Manifest.permission.BLUETOOTH_SCAN
            perms += Manifest.permission.BLUETOOTH_CONNECT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.NEARBY_WIFI_DEVICES
        }
        return perms
    }
}
