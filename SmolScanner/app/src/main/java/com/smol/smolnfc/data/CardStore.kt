package com.smol.smolnfc.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.smol.smolnfc.hce.HceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class StoredCard(
    val id: String,
    val label: String,
    val savedAt: Long,
    val uid: String,
    val atqa: String,
    val sak: String,
    val techList: List<String>,
    val icGuess: String,
    val memory: List<String>,
    val emulable: Boolean,
    val emulableReason: String,
    val ndefHex: String?,
    val ndefDesc: String?
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("label", label)
        put("savedAt", savedAt)
        put("uid", uid)
        put("atqa", atqa)
        put("sak", sak)
        put("techList", JSONArray(techList))
        put("icGuess", icGuess)
        put("memory", JSONArray(memory))
        put("emulable", emulable)
        put("emulableReason", emulableReason)
        put("ndefHex", ndefHex ?: JSONObject.NULL)
        put("ndefDesc", ndefDesc ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(o: JSONObject): StoredCard {
            val tech = o.optJSONArray("techList") ?: JSONArray()
            val mem = o.optJSONArray("memory") ?: JSONArray()
            return StoredCard(
                id = o.getString("id"),
                label = o.optString("label"),
                savedAt = o.optLong("savedAt"),
                uid = o.optString("uid"),
                atqa = o.optString("atqa"),
                sak = o.optString("sak"),
                techList = (0 until tech.length()).map { tech.getString(it) },
                icGuess = o.optString("icGuess"),
                memory = (0 until mem.length()).map { mem.getString(it) },
                emulable = o.optBoolean("emulable"),
                emulableReason = o.optString("emulableReason"),
                ndefHex = o.opt("ndefHex").takeIf { it != null && it != JSONObject.NULL }?.toString(),
                ndefDesc = o.opt("ndefDesc").takeIf { it != null && it != JSONObject.NULL }?.toString()
            )
        }

        fun newId(): String = UUID.randomUUID().toString()
    }
}

class CardStore(private val file: File) {

    private val cards = mutableListOf<StoredCard>()

    init {
        load()
    }

    @Synchronized
    fun all(): List<StoredCard> = cards.sortedByDescending { it.savedAt }

    @Synchronized
    fun add(card: StoredCard) {
        cards.add(card)
        persist()
    }

    @Synchronized
    fun rename(id: String, label: String) {
        val i = cards.indexOfFirst { it.id == id }
        if (i >= 0) {
            cards[i] = cards[i].copy(label = label)
            persist()
        }
    }

    @Synchronized
    fun delete(id: String) {
        cards.removeAll { it.id == id }
        persist()
    }

    private fun load() {
        if (!file.exists()) return
        runCatching {
            val arr = JSONArray(file.readText())
            for (i in 0 until arr.length()) {
                cards.add(StoredCard.fromJson(arr.getJSONObject(i)))
            }
        }
    }

    private fun persist() {
        val arr = JSONArray()
        cards.forEach { arr.put(it.toJson()) }
        runCatching { file.writeText(arr.toString()) }
    }
}

class CardViewModel(app: Application) : AndroidViewModel(app) {

    private val store = CardStore(File(app.filesDir, "cards.json"))

    private val _cards = MutableStateFlow(store.all())
    val cards = _cards.asStateFlow()

    private val _activeEmulationId = MutableStateFlow<String?>(null)
    val activeEmulationId = _activeEmulationId.asStateFlow()

    fun add(card: StoredCard) {
        store.add(card)
        _cards.value = store.all()
    }

    fun rename(id: String, label: String) {
        store.rename(id, label)
        _cards.value = store.all()
    }

    fun delete(id: String) {
        if (_activeEmulationId.value == id) setEmulating(null)
        store.delete(id)
        _cards.value = store.all()
    }

    fun setEmulating(card: StoredCard?) {
        HceState.setPayload(getApplication(), card?.ndefHex)
        _activeEmulationId.value = card?.id
    }
}
