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
                1 -> SearchScreen()
                2 -> LibraryScreen(play)
                else -> CreateScreen()
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
