package com.smol.smolnfc.scanner

data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val manufacturerId: Int?,
    val firstSeen: Long,
    val lastSeen: Long
)

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val capabilities: String
)

object Vendors {
    private val map = mapOf(
        0x004C to "Apple",
        0x0075 to "Samsung",
        0x0006 to "Microsoft",
        0x00E0 to "Google",
        0x0059 to "Nordic",
        0x0087 to "Garmin"
    )

    fun name(id: Int?): String? {
        if (id == null) return null
        return map[id] ?: "ID 0x%04X".format(id)
    }

    fun isPossibleTrackerVendor(id: Int?): Boolean = id == 0x004C || id == 0x0075
}
