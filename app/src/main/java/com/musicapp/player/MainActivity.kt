package com.musicapp.player

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
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
        setContent { AppTheme { Root() } }
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
fun Root() {
    val ctx = LocalContext.current
    val first = remember { !Engine.wasReady(ctx) }
    var done by remember { mutableStateOf(Engine.done) }
    LaunchedEffect(Unit) {
        if (!done) { Engine.init(ctx); done = true }
        Engine.updateIfStale(ctx)
    }
    if (done) App() else LoadingScreen(first)
}

@Composable
fun App() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var current by remember { mutableStateOf(sampleTracks[0]) }
    var playing by remember { mutableStateOf(false) }
    val mine = remember { mutableStateListOf<Track>() }
    val ctx = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) notifPerm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
    DisposableEffect(Unit) {
        val f = MediaController.Builder(ctx, SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))).buildAsync()
        f.addListener({ controller = f.get() }, ContextCompat.getMainExecutor(ctx))
        onDispose { MediaController.releaseFuture(f) }
    }
    DisposableEffect(controller) {
        val c = controller
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
        }
        c?.addListener(l)
        onDispose { c?.removeListener(l) }
    }
    val scope = rememberCoroutineScope()
    val playTrack: (Track) -> Unit = { t ->
        current = t
        val c = controller
        if (c != null) {
            if (t.id.isEmpty()) c.pause() else scope.launch {
                try {
                    val url = Api.streamUrl(t)
                    val item = MediaItem.Builder()
                        .setUri(url)
                        .setMediaMetadata(
                            MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist)
                                .setArtworkUri(t.cover?.let { Uri.parse(it) }).build()
                        ).build()
                    c.setMediaItem(item); c.prepare(); c.play()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(ctx, "Gagal memutar lagu ini", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val toggle: () -> Unit = {
        val c = controller
        if (c != null && c.mediaItemCount > 0) { if (c.isPlaying) c.pause() else c.play() }
    }
    val palette = listOf(Color(0xFF6A5AE0), Color(0xFF1E9E78), Color(0xFFD9534F), Color(0xFF2F7DD1), Color(0xFFE08A1E))

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            Column {
                MiniPlayer(current, playing) { toggle() }
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
            val play = playTrack
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
        Cover(t.color, 40.dp, url = t.cover)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(t.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Text(t.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1)
        }
        Icon(Icons.Outlined.FavoriteBorder, "Like", Modifier.padding(horizontal = 8.dp))
        Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pause", Modifier.padding(horizontal = 8.dp))
    }
}
