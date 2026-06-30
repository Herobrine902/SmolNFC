package com.smol.smolnfc

import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.smol.smolnfc.data.CardViewModel
import com.smol.smolnfc.nfc.NfcReader
import com.smol.smolnfc.ui.App

class MainActivity : ComponentActivity() {

    private val vm: CardViewModel by viewModels()
    private var adapter: NfcAdapter? = null

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
        adapter = NfcAdapter.getDefaultAdapter(this)
        setContent {
            val cards by vm.cards.collectAsState()
            val active by vm.activeEmulationId.collectAsState()
            App(
                cards = cards,
                activeEmulationId = active,
                onRename = vm::rename,
                onDelete = vm::delete,
                onSetEmulating = vm::setEmulating
            )
        }
        if (adapter == null) {
            Toast.makeText(this, "No NFC on this device", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        adapter?.enableReaderMode(this, readerCallback, readerFlags, null)
    }

    override fun onPause() {
        super.onPause()
        adapter?.disableReaderMode(this)
    }
}
