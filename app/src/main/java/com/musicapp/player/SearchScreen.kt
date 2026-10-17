package com.musicapp.player

import androidx.compose.foundation.background
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val genres = listOf(
    "Pop" to Color(0xFFE8115B), "Hip-Hop" to Color(0xFFBC5900),
    "Rock" to Color(0xFFE91429), "Indie" to Color(0xFF608108),
    "Chill" to Color(0xFF477D95), "Workout" to Color(0xFF777777),
    "Podcasts" to Color(0xFF0D73EC), "Charts" to Color(0xFF8D67AB),
)

@Composable
fun SearchScreen(current: Track, playing: Boolean, onPlay: (Track) -> Unit, onQueue: (List<Track>) -> Unit) {
    var query by remember { mutableStateOf("") }
    val q = query.trim()
    var results by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(q) {
        results = emptyList(); error = null
        if (q.length < 2) { loading = false; return@LaunchedEffect }
        loading = true
        delay(500)
        try { results = Api.search(q) }
        catch (e: Exception) { error = "Search failed. Check your internet connection and try again." }
        loading = false
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Title("Search") }
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (isApple) Card else Color.White).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, tint = if (isApple) Sub else Color.Black)
                BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = TextStyle(color = if (isApple) Ink else Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(if (isApple) Ink else Color.Black),
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) Text(if (isApple) "Artists, Songs, Lyrics, and More" else "What do you want to listen to?", color = if (isApple) Sub else Color(0xFF535353), fontWeight = FontWeight.Bold)
                            inner()
                        }
                    },
                )
                if (query.isNotEmpty()) Icon(Icons.Filled.Close, "Clear", Modifier.clickable { query = "" }, tint = if (isApple) Sub else Color.Black)
            }
        }
        if (q.isEmpty()) {
            item { Text(if (isApple) "Browse Categories" else "Browse all", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            items(genres.chunked(2)) { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { (name, c) ->
                        Box(Modifier.weight(1f).height(96.dp).clip(RoundedCornerShape(8.dp)).background(c).padding(12.dp)) {
                            Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        } else if (results.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    when {
                        loading -> Text("Searching...", color = Sub, fontSize = 16.sp)
                        error != null -> Text(error ?: "", color = Sub, fontSize = 14.sp)
                        else -> {
                            Text("Couldn't find \"$q\"", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Check the spelling, or try a different title or artist.", color = Sub, fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            items(results) { t ->
                Row(Modifier.fillMaxWidth().clickable { onQueue(results); onPlay(t) }, verticalAlignment = Alignment.CenterVertically) {
                    Cover(t.color, 56.dp, url = t.cover)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(t.title, fontWeight = FontWeight.Bold, maxLines = 1,
                            color = if (t == current && playing) Green else Ink)
                        Text("Song • ${t.artist}", color = Sub, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}
