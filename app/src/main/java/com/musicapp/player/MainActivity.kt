package com.musicapp.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { App() } }
    }
}

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Home", Icons.Filled.Home),
    Tab("Search", Icons.Filled.Search),
    Tab("Your Library", Icons.Filled.LibraryMusic),
    Tab("Create", Icons.Filled.Add),
)

@Composable
fun App() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var current by remember { mutableStateOf(sampleTracks[0]) }
    var playing by remember { mutableStateOf(false) }
    val mine = remember { mutableStateListOf<Track>() }
    val palette = listOf(Color(0xFF6A5AE0), Color(0xFF1E9E78), Color(0xFFD9534F), Color(0xFF2F7DD1), Color(0xFFE08A1E))

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            Column {
                MiniPlayer(current, playing) { playing = !playing }
                NavigationBar(containerColor = Bg) {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(t.icon, t.label) },
                            label = { Text(t.label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                unselectedIconColor = Sub,
                                unselectedTextColor = Sub,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val play = { t: Track -> current = t; playing = true }
            when (tab) {
                0 -> HomeScreen(current, playing, play)
                1 -> SearchScreen(current, playing, play)
                2 -> LibraryScreen(mine, current, playing, play)
                else -> CreateScreen { name ->
                    mine.add(0, Track(name, "You", palette[mine.size % palette.size]))
                    tab = 2
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(t: Track, playing: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(t.color.copy(alpha = 0.55f))
            .clickable { onToggle() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(t.color, 40.dp)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(t.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Text(t.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1)
        }
        Icon(Icons.Outlined.FavoriteBorder, "Like", Modifier.padding(horizontal = 8.dp))
        Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pause", Modifier.padding(horizontal = 8.dp))
    }
}
