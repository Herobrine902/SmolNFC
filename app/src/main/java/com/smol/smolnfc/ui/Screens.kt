package com.smol.smolnfc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smol.smolnfc.data.StoredCard

@Composable
fun SmolTheme(content: @Composable () -> Unit) {
    val scheme: ColorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = scheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

@Composable
fun App(
    cards: List<StoredCard>,
    activeEmulationId: String?,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onSetEmulating: (StoredCard?) -> Unit
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    SmolTheme {
        val current = cards.firstOrNull { it.id == selectedId }
        if (current == null) {
            CardListScreen(cards, activeEmulationId) { selectedId = it }
        } else {
            CardDetailScreen(
                card = current,
                isEmulating = activeEmulationId == current.id,
                onBack = { selectedId = null },
                onRename = { onRename(current.id, it) },
                onDelete = { onDelete(current.id); selectedId = null },
                onToggleEmulate = {
                    if (activeEmulationId == current.id) onSetEmulating(null)
                    else onSetEmulating(current)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardListScreen(
    cards: List<StoredCard>,
    activeEmulationId: String?,
    onOpen: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Smol NFC", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Tap a card to the back of the phone to scan",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            })
        }
    ) { pad ->
        if (cards.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text(
                    "No cards yet.\nHold one to the upper back of the phone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(pad)
            ) {
                items(cards, key = { it.id }) { card ->
                    CardRow(card, activeEmulationId == card.id) { onOpen(card.id) }
                }
            }
        }
    }
}

@Composable
private fun CardRow(card: StoredCard, emulating: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    card.label,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StatusChip(card.emulable, emulating)
            }
            Text(
                card.uid,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                card.icGuess,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun StatusChip(emulable: Boolean, emulating: Boolean) {
    val (text, bg, fg) = when {
        emulating -> Triple("emulating", Color(0xFF1B5E20), Color.White)
        emulable -> Triple("NDEF", Color(0xFF2E7D32), Color.White)
        else -> Triple("read-only", Color(0x33000000), MaterialTheme.colorScheme.onSurface)
    }
    Box(
        Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardDetailScreen(
    card: StoredCard,
    isEmulating: Boolean,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onToggleEmulate: () -> Unit
) {
    var label by remember(card.id) { mutableStateOf(card.label) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card details") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(pad)
        ) {
            item {
                TextField(
                    value = label,
                    onValueChange = { label = it; onRename(it) },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Field("UID", card.uid, mono = true) }
            item { Field("ATQA", card.atqa, mono = true) }
            item { Field("SAK", card.sak, mono = true) }
            item { Field("IC guess", card.icGuess) }
            item { Field("Tech", card.techList.joinToString(", ")) }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            if (card.emulable) "Emulable" else "Not emulable",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            card.emulableReason,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        card.ndefDesc?.let {
                            Text(
                                "Payload: $it",
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        if (card.emulable) {
                            OutlinedButton(
                                onClick = onToggleEmulate,
                                modifier = Modifier.padding(top = 12.dp)
                            ) {
                                Text(if (isEmulating) "Stop emulating" else "Emulate this card")
                            }
                            if (isEmulating) {
                                Text(
                                    "Hold the phone to the reader now.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (card.memory.isNotEmpty()) {
                item {
                    Text("Memory", fontWeight = FontWeight.SemiBold)
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(14.dp)
                        ) {
                            card.memory.forEach { line ->
                                Text(
                                    line,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            item {
                TextButton(onClick = onDelete) {
                    Text("Delete card", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, mono: Boolean = false) {
    Column {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value.ifBlank { "—" },
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
