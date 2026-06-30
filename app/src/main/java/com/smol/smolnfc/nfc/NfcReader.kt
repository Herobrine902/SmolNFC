package com.smol.smolnfc.nfc

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import com.smol.smolnfc.data.StoredCard

object NfcReader {

    fun parse(tag: Tag): StoredCard {
        val uid = tag.id.toHex()
        val techShort = tag.techList.map { it.substringAfterLast('.') }

        var atqa = "—"
        var sak = "—"
        NfcA.get(tag)?.let { a ->
            atqa = a.atqa.toHex()
            sak = "0x%02X".format(a.sak.toInt() and 0xFFFF)
        }

        val memory = mutableListOf<String>()
        var ndefHex: String? = null
        var ndefDesc: String? = null

        when {
            tag.techList.contains(MifareUltralight::class.java.name) ->
                memory += readUltralight(tag)

            tag.techList.contains(MifareClassic::class.java.name) ->
                memory += readClassic(tag)
        }

        if (tag.techList.contains(Ndef::class.java.name)) {
            val msg = readNdef(tag)
            if (msg != null) {
                ndefHex = msg.toByteArray().toHex(separator = "")
                ndefDesc = describeNdef(msg)
            }
        }

        val ic = guessIc(techShort, sak)
        val (emulable, reason) = emulability(techShort, ndefHex != null)

        return StoredCard(
            id = StoredCard.newId(),
            label = ic,
            savedAt = System.currentTimeMillis(),
            uid = uid,
            atqa = atqa,
            sak = sak,
            techList = techShort,
            icGuess = ic,
            memory = memory,
            emulable = emulable,
            emulableReason = reason,
            ndefHex = ndefHex,
            ndefDesc = ndefDesc
        )
    }

    private fun readUltralight(tag: Tag): List<String> {
        val out = mutableListOf<String>()
        val ul = MifareUltralight.get(tag) ?: return out
        runCatching {
            ul.connect()
            var page = 0
            while (page < 64) {
                val chunk = try {
                    ul.readPages(page)
                } catch (e: Exception) {
                    break
                }
                for (p in 0 until 4) {
                    val start = p * 4
                    if (start + 4 > chunk.size) break
                    val slice = chunk.copyOfRange(start, start + 4)
                    out += "[%02X] %s".format(page + p, slice.toHex())
                }
                page += 4
            }
        }
        runCatching { ul.close() }
        return out
    }

    private fun readClassic(tag: Tag): List<String> {
        val out = mutableListOf<String>()
        val mc = MifareClassic.get(tag) ?: return out
        val keys = listOf(
            MifareClassic.KEY_DEFAULT,
            MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY,
            MifareClassic.KEY_NFC_FORUM
        )
        runCatching {
            mc.connect()
            for (sector in 0 until mc.sectorCount) {
                val key = keys.firstOrNull { k ->
                    runCatching { mc.authenticateSectorWithKeyA(sector, k) }.getOrDefault(false)
                }
                if (key == null) {
                    out += "sector %02d  locked (no default key)".format(sector)
                    continue
                }
                val first = mc.sectorToBlock(sector)
                val count = mc.getBlockCountInSector(sector)
                for (b in 0 until count) {
                    val block = first + b
                    val data = runCatching { mc.readBlock(block) }.getOrNull()
                    out += if (data != null) "blk %03d  %s".format(block, data.toHex())
                    else "blk %03d  unreadable".format(block)
                }
            }
        }
        runCatching { mc.close() }
        return out
    }

    private fun readNdef(tag: Tag): NdefMessage? {
        val ndef = Ndef.get(tag) ?: return null
        return runCatching {
            ndef.connect()
            val live = ndef.ndefMessage
            ndef.close()
            live
        }.getOrElse {
            runCatching { ndef.close() }
            ndef.cachedNdefMessage
        }
    }

    private fun describeNdef(msg: NdefMessage): String {
        val parts = msg.records.map { r ->
            val payload = r.payload
            when {
                r.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN &&
                    r.type.contentEquals(android.nfc.NdefRecord.RTD_URI) -> {
                    val prefix = uriPrefix(payload.firstOrNull()?.toInt()?.and(0xFF) ?: 0)
                    prefix + String(payload.copyOfRange(1, payload.size), Charsets.UTF_8)
                }
                r.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN &&
                    r.type.contentEquals(android.nfc.NdefRecord.RTD_TEXT) -> {
                    val status = payload.firstOrNull()?.toInt()?.and(0xFF) ?: 0
                    val langLen = status and 0x3F
                    "text: " + String(payload.copyOfRange(1 + langLen, payload.size), Charsets.UTF_8)
                }
                else -> "record (${String(r.type, Charsets.UTF_8)})"
            }
        }
        return parts.joinToString("  •  ")
    }

    private fun uriPrefix(code: Int): String = when (code) {
        0x01 -> "http://www."
        0x02 -> "https://www."
        0x03 -> "http://"
        0x04 -> "https://"
        0x05 -> "tel:"
        0x06 -> "mailto:"
        else -> ""
    }

    private fun guessIc(tech: List<String>, sak: String): String = when {
        tech.contains("MifareUltralight") -> "MIFARE Ultralight family"
        tech.contains("MifareClassic") -> "MIFARE Classic"
        tech.contains("IsoDep") -> "ISO-DEP (Type 4 / DESFire-class)"
        tech.contains("NfcF") -> "FeliCa (Type F)"
        tech.contains("NfcV") -> "ISO 15693 (Type V)"
        tech.contains("Ndef") -> "NDEF tag"
        else -> "Unknown (SAK $sak)"
    }

    private fun emulability(tech: List<String>, hasNdef: Boolean): Pair<Boolean, String> = when {
        hasNdef -> true to "NDEF payload — phone can present this as a Type 4 tag."
        tech.contains("IsoDep") ->
            false to "App/encrypted card — emulation needs its keys and protocol, which aren't on the chip."
        tech.contains("MifareClassic") || tech.contains("MifareUltralight") ->
            false to "UID-checked Type 2 card — stock Android can't emulate Type 2 or set the UID."
        else ->
            false to "No emulable payload found for this tag type."
    }
}

private fun ByteArray.toHex(separator: String = " "): String =
    joinToString(separator) { "%02X".format(it.toInt() and 0xFF) }
