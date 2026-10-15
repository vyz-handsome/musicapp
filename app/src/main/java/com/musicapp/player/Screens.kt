package com.musicapp.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Track(val title: String, val artist: String, val color: Color)

val sampleTracks = listOf(
    Track("Liked Songs", "Playlist", Color(0xFF4A3AFF)),
    Track("Daily Mix 1", "Made for you", Color(0xFF1E8E5A)),
    Track("Midnight Drive", "Neon Harbor", Color(0xFF8E2DE2)),
    Track("Late Bloom", "Sora Vale", Color(0xFFE35D5B)),
    Track("Glass Rooms", "The Quiet Hours", Color(0xFF2B7DE9)),
    Track("Sunday Static", "Mika Lund", Color(0xFFE89B2D)),
    Track("Paper Planes", "Orchid Club", Color(0xFFB02A5B)),
    Track("Low Tide", "Aru", Color(0xFF0F7A8A)),
)

private val genres = listOf(
    "Pop" to Color(0xFFE8115B), "Hip-Hop" to Color(0xFFBC5900),
    "Rock" to Color(0xFFE91429), "Indie" to Color(0xFF608108),
    "Chill" to Color(0xFF477D95), "Workout" to Color(0xFF777777),
    "Podcasts" to Color(0xFF0D73EC), "Charts" to Color(0xFF8D67AB),
)

@Composable
fun Cover(color: Color, size: Dp, radius: Dp = 4.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(radius))
            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.55f)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(size / 2))
    }
}

@Composable
fun Chip(label: String, selected: Boolean, modifier: Modifier = Modifier) {
    Text(
        label,
        Modifier.clip(RoundedCornerShape(50)).background(if (selected) Green else Card).then(modifier)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        color = if (selected) Color.Black else Color.White,
        fontSize = 13.sp, fontWeight = FontWeight.Medium,
    )
}

@Composable
fun Title(text: String) = Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold)

@Composable
fun SearchScreen() {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Title("Search") }
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, tint = Color.Black)
                Text("What do you want to listen to?", Modifier.padding(start = 10.dp),
                    color = Color(0xFF535353), fontWeight = FontWeight.Bold)
            }
        }
        item { Text("Browse all", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        items(genres.chunked(2)) { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { (name, c) ->
                    Box(Modifier.weight(1f).height(96.dp).clip(RoundedCornerShape(8.dp)).background(c).padding(12.dp)) {
                        Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(onPlay: (Track) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Title("Your Library") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Playlists", false); Chip("Artists", false); Chip("Albums", false)
            }
        }
        items(sampleTracks) { t ->
            Row(Modifier.fillMaxWidth().clickable { onPlay(t) }, verticalAlignment = Alignment.CenterVertically) {
                Cover(t.color, 56.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(t.title, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Playlist • ${t.artist}", color = Sub, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun CreateScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Title("Create")
        listOf(
            Triple("Playlist", "Build a playlist with songs or episodes", Icons.Filled.MusicNote),
            Triple("Blend", "Combine tastes in a shared playlist", Icons.Filled.Group),
        ).forEach { (name, desc, icon) ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Card).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, Modifier.size(32.dp))
                Column(Modifier.padding(start = 14.dp)) {
                    Text(name, fontWeight = FontWeight.Bold)
                    Text(desc, color = Sub, fontSize = 13.sp)
                }
            }
        }
    }
}
