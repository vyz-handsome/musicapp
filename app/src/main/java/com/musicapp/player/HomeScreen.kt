package com.musicapp.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val podcasts = listOf(
    Track("The Long Take", "Rina & Dave", Color(0xFF7A3FD1)),
    Track("Soft Launch", "Ayo Bell", Color(0xFF1F9D8B)),
    Track("Side Quests", "Marlo Park", Color(0xFFD1623F)),
    Track("Night Shift Notes", "Ines Roca", Color(0xFF3F6FD1)),
)

private val artists = listOf(
    Track("Neon Harbor", "Artist", Color(0xFF8E2DE2)),
    Track("Sora Vale", "Artist", Color(0xFFE35D5B)),
    Track("The Quiet Hours", "Artist", Color(0xFF2B7DE9)),
    Track("Mika Lund", "Artist", Color(0xFFE89B2D)),
    Track("Orchid Club", "Artist", Color(0xFFB02A5B)),
)

@Composable
fun HomeScreen(current: Track, playing: Boolean, onPlay: (Track) -> Unit, onSettings: () -> Unit) {
    var filter by remember { mutableIntStateOf(0) }
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greet = when { hour < 12 -> "Good morning"; hour < 18 -> "Good afternoon"; else -> "Good evening" }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (isApple) "Listen Now" else greet, Modifier.weight(1f), fontSize = if (isApple) 32.sp else 24.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Filled.Notifications, "Notifications")
                Icon(Icons.Filled.History, "Listening history", Modifier.padding(start = 18.dp))
                Icon(Icons.Filled.Settings, "Settings", Modifier.padding(start = 18.dp).clickable { onSettings() })
            }
        }
        item {
            if (!isApple) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Music", "Podcasts").forEachIndexed { i, label ->
                    Chip(label, filter == i, Modifier.clickable { filter = i })
                }
            }
        }
        if (filter == 2) {
            item { Shelf("Shows to try", podcasts, current, playing, onPlay) }
            item { Shelf("New episodes", podcasts.reversed(), current, playing, onPlay) }
        } else {
            items(if (isApple) emptyList<List<Track>>() else sampleTracks.take(6).chunked(2)) { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { t ->
                        Row(
                            Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(4.dp)).background(Card)
                                .clickable { onPlay(t) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Cover(t.color, 56.dp, 0.dp)
                            Text(
                                t.title, Modifier.padding(horizontal = 8.dp), fontSize = 13.sp,
                                fontWeight = FontWeight.Bold, maxLines = 2,
                                color = if (t == current && playing) Green else Ink,
                            )
                        }
                    }
                }
            }
            item { Shelf(if (isApple) "Top Picks for You" else "Jump back in", sampleTracks.reversed().take(6), current, playing, onPlay) }
            item { Shelf(if (isApple) "Made for You" else "Made for you", sampleTracks, current, playing, onPlay) }
            item { Shelf("Your favorite artists", artists, current, playing, onPlay, round = true) }
            item { Shelf("Recently played", sampleTracks.shuffled(java.util.Random(7)), current, playing, onPlay) }
        }
    }
}

@Composable
private fun Shelf(
    title: String, list: List<Track>, current: Track, playing: Boolean,
    onPlay: (Track) -> Unit, round: Boolean = false,
) {
    val size = if (round) 120.dp else 140.dp
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (isApple) "$title  ›" else title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list) { t ->
                val active = t == current && playing
                Column(Modifier.width(size).clickable { onPlay(t) }) {
                    Cover(t.color, size, if (round) size / 2 else 4.dp)
                    Text(
                        t.title, Modifier.padding(top = 6.dp).fillMaxWidth(), fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1,
                        color = if (active) Green else Ink,
                        textAlign = if (round) TextAlign.Center else TextAlign.Start,
                    )
                    if (!round) Text(t.artist, color = Sub, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}
