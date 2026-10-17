package com.musicapp.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val kinds = listOf("Playlist", "Artist", "Album")
private val kindLabels = listOf("Playlists", "Artists", "Albums")

@Composable
fun LibraryScreen(mine: List<Track>, current: Track, playing: Boolean, onPlay: (Track) -> Unit) {
    var filter by remember { mutableIntStateOf(-1) }
    var grid by remember { mutableStateOf(false) }
    val all = mine.map { it to "Playlist" } + sampleTracks.mapIndexed { i, t -> t to kinds[i % 3] }
    val shown = if (filter < 0) all else all.filter { it.second == kinds[filter] }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Green), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, null, tint = OnAccent, modifier = Modifier.size(20.dp))
                }
                Text(if (isApple) "Library" else "Your Library", Modifier.padding(start = 12.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                kindLabels.forEachIndexed { i, l ->
                    Chip(l, filter == i, Modifier.clickable { filter = if (filter == i) -1 else i })
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Recents", Modifier.weight(1f), color = Sub, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Icon(
                    if (grid) Icons.Filled.ViewList else Icons.Filled.GridView, "Change layout",
                    Modifier.clickable { grid = !grid }.size(22.dp),
                )
            }
        }
        if (grid) {
            items(shown.chunked(2)) { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { (t, kind) ->
                        Column(Modifier.weight(1f).clickable { onPlay(t) }) {
                            Cover(t.color, 140.dp, if (kind == "Artist") 70.dp else 4.dp)
                            Text(t.title, Modifier.padding(top = 6.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, color = if (t == current && playing) Green else Ink)
                            Text(kind, color = Sub, fontSize = 12.sp)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            items(shown) { (t, kind) ->
                Row(Modifier.fillMaxWidth().clickable { onPlay(t) }, verticalAlignment = Alignment.CenterVertically) {
                    Cover(t.color, 56.dp, if (kind == "Artist") 28.dp else 4.dp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(t.title, fontWeight = FontWeight.Bold, maxLines = 1,
                            color = if (t == current && playing) Green else Ink)
                        Text(if (kind == "Artist") kind else "$kind • ${t.artist}", color = Sub, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}
