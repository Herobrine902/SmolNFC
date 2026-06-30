package com.smol.smolnfc.hce

import android.content.Context
import android.nfc.cardemulation.HostApduService
import android.os.Bundle

object HceState {
    private const val PREFS = "smol_hce"
    private const val KEY = "ndef_hex"

    fun setPayload(context: Context, hex: String?) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (hex == null) p.remove(KEY) else p.putString(KEY, hex)
        p.apply()
    }

    fun payloadBytes(context: Context): ByteArray? {
        val hex = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return null
        return runCatching {
            ByteArray(hex.length / 2) { i ->
                hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        }.getOrNull()
    }
}

class NdefHostApduService : HostApduService() {

    private enum class Selected { NONE, CC, NDEF }

    private var selected = Selected.NONE

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val apdu = commandApdu ?: return SW_FAIL

        if (isSelectAid(apdu)) {
            selected = Selected.NONE
            return SW_OK
        }

        if (isSelectFile(apdu)) {
            val fileId = (apdu[5].toInt() and 0xFF shl 8) or (apdu[6].toInt() and 0xFF)
            return when (fileId) {
                FILE_CC -> { selected = Selected.CC; SW_OK }
                FILE_NDEF -> { selected = Selected.NDEF; SW_OK }
                else -> SW_FILE_NOT_FOUND
            }
        }

        if (apdu.size >= 5 && apdu[0] == 0x00.toByte() && apdu[1] == 0xB0.toByte()) {
            val offset = (apdu[2].toInt() and 0xFF shl 8) or (apdu[3].toInt() and 0xFF)
            val length = apdu[4].toInt() and 0xFF
            val source = when (selected) {
                Selected.CC -> CC_FILE
                Selected.NDEF -> ndefFile()
                Selected.NONE -> return SW_FILE_NOT_FOUND
            }
            if (offset >= source.size) return SW_FAIL
            val end = minOf(offset + length, source.size)
            return source.copyOfRange(offset, end) + SW_OK
        }

        return SW_FAIL
    }

    override fun onDeactivated(reason: Int) {
        selected = Selected.NONE
    }

    private fun ndefFile(): ByteArray {
        val message = HceState.payloadBytes(this) ?: ByteArray(0)
        val len = message.size
        return byteArrayOf((len shr 8 and 0xFF).toByte(), (len and 0xFF).toByte()) + message
    }

    private fun isSelectAid(apdu: ByteArray): Boolean {
        if (apdu.size < 6) return false
        if (apdu[0] != 0x00.toByte() || apdu[1] != 0xA4.toByte() || apdu[2] != 0x04.toByte()) return false
        val lc = apdu[4].toInt() and 0xFF
        if (apdu.size < 5 + lc) return false
        val aid = apdu.copyOfRange(5, 5 + lc)
        return aid.contentEquals(NDEF_AID)
    }

    private fun isSelectFile(apdu: ByteArray): Boolean =
        apdu.size >= 7 &&
            apdu[0] == 0x00.toByte() &&
            apdu[1] == 0xA4.toByte() &&
            apdu[2] == 0x00.toByte() &&
            apdu[3] == 0x0C.toByte() &&
            apdu[4] == 0x02.toByte()

    companion object {
        private const val FILE_CC = 0xE103
        private const val FILE_NDEF = 0xE104

        private val NDEF_AID = byteArrayOf(
            0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01
        )

        private val CC_FILE = byteArrayOf(
            0x00, 0x0F,
            0x20,
            0x00, 0x3B,
            0x00, 0x34,
            0x04, 0x06,
            0xE1.toByte(), 0x04,
            0x00, 0xFF.toByte(),
            0x00, 0x00
        )

        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
        private val SW_FILE_NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())
        private val SW_FAIL = byteArrayOf(0x6F, 0x00)
    }
}
