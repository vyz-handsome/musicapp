package com.musicapp.player

import android.app.Activity
import android.content.ComponentName
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Prefs.load(this)
        setContent { AppTheme { Root() } }
    }
}

private data class Tab(val label: String, val icon: ImageVector)

private fun tabsFor(apple: Boolean) = if (apple) listOf(
    Tab("Listen Now", Icons.Filled.PlayCircle),
    Tab("Search", Icons.Filled.Search),
    Tab("Library", Icons.Filled.LibraryMusic),
    Tab("Create", Icons.Filled.Add),
) else listOf(
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
    var showNow by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var shuffle by remember { mutableStateOf(false) }
    var repeat by remember { mutableIntStateOf(0) } // 0 off, 1 all, 2 one
    var queue by remember { mutableStateOf(listOf<Track>()) }
    val liked = remember { mutableStateListOf<String>() }
    val mine = remember { mutableStateListOf<Track>() }
    val palette = listOf(Color(0xFF6A5AE0), Color(0xFF1E9E78), Color(0xFFD9534F), Color(0xFF2F7DD1), Color(0xFFE08A1E))
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
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

    fun toast(m: String) = android.widget.Toast.makeText(ctx, m, android.widget.Toast.LENGTH_LONG).show()
    fun startTrack(t: Track, skipsLeft: Int) {
        current = t
        val c = controller ?: return
        if (t.id.isEmpty()) { c.pause(); return }
        loading = true
        scope.launch {
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
                loading = false
                val msg = (e.message ?: e.javaClass.simpleName).lineSequence().last { it.isNotBlank() }.take(90)
                val i = queue.indexOf(t)
                if (skipsLeft > 0 && i in 0 until queue.size - 1) {
                    toast("Dilewati: $msg")
                    startTrack(queue[i + 1], skipsLeft - 1)
                } else toast("Gagal memutar: $msg")
            }
        }
    }
    val playTrack: (Track) -> Unit = { startTrack(it, 0) }
    val next: () -> Unit = {
        val i = queue.indexOf(current)
        if (queue.isNotEmpty()) {
            val n = when {
                shuffle -> queue.random()
                i in 0 until queue.size - 1 -> queue[i + 1]
                repeat == 1 -> queue[0]
                else -> null
            }
            if (n != null) startTrack(n, 2)
        }
    }
    val prev: () -> Unit = {
        val c = controller
        val i = queue.indexOf(current)
        if (c != null && (c.currentPosition > 3000 || i <= 0)) c.seekTo(0) else if (i > 0) playTrack(queue[i - 1])
    }
    val toggle: () -> Unit = {
        val c = controller
        if (c != null && c.mediaItemCount > 0) { if (c.isPlaying) c.pause() else c.play() }
    }
    val nextRef by rememberUpdatedState(next)
    val repeatRef by rememberUpdatedState(repeat)

    DisposableEffect(controller) {
        val c = controller
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlaybackStateChanged(state: Int) {
                loading = state == Player.STATE_BUFFERING
                if (state == Player.STATE_ENDED) {
                    if (repeatRef == 2) { c?.seekTo(0); c?.play() } else if (Prefs.autoplay) nextRef()
                }
            }
        }
        c?.addListener(l)
        onDispose { c?.removeListener(l) }
    }

    val key = current.id.ifEmpty { current.title }
    val isLiked = key in liked
    val toggleLike = { if (isLiked) liked.remove(key) else liked.add(key); Unit }

    val darkUi = !isApple || Prefs.dark || showNow
    val view = LocalView.current
    SideEffect {
        (ctx as? Activity)?.window?.let { w ->
            val c = WindowCompat.getInsetsController(w, view)
            c.isAppearanceLightStatusBars = !darkUi
            c.isAppearanceLightNavigationBars = !darkUi
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                Column {
                    MiniPlayer(current, playing, isLiked, { showNow = true }, toggleLike, toggle)
                    NavigationBar(containerColor = Bg, tonalElevation = 0.dp) {
                        tabsFor(isApple).forEachIndexed { i, t ->
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = { tab = i },
                                icon = { Icon(t.icon, t.label) },
                                label = { Text(t.label, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = if (isApple) Green else Ink,
                                    selectedTextColor = if (isApple) Green else Ink,
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
                when (tab) {
                    0 -> HomeScreen(current, playing, playTrack) { showSettings = true }
                    1 -> SearchScreen(current, playing, playTrack) { queue = it }
                    2 -> LibraryScreen(mine, current, playing, playTrack)
                    else -> CreateScreen { name ->
                        mine.add(0, Track(name, "You", palette[mine.size % palette.size]))
                        tab = 2
                    }
                }
            }
        }
        AnimatedVisibility(visible = showNow, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            NowPlayingScreen(
                t = current, playing = playing, loading = loading, controller = controller, liked = isLiked,
                shuffle = shuffle, repeat = repeat, queue = queue,
                onClose = { showNow = false }, onLike = { toggleLike() }, onToggle = toggle,
                onPrev = prev, onNext = next,
                onShuffle = { shuffle = !shuffle }, onRepeat = { repeat = (repeat + 1) % 3 },
                onPick = playTrack,
            )
        }
        AnimatedVisibility(visible = showSettings, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) {
            SettingsScreen { showSettings = false }
        }
    }
    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = showNow) { showNow = false }
}

@Composable
private fun MiniPlayer(
    t: Track, playing: Boolean, liked: Boolean,
    onOpen: () -> Unit, onLike: () -> Unit, onToggle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isApple) Card else t.color.copy(alpha = 0.55f))
            .clickable { onOpen() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(t.color, 40.dp, url = t.cover)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(t.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Text(t.artist, color = if (isApple) Sub else Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1)
        }
        Icon(
            if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Like",
            Modifier.clickable { onLike() }.padding(8.dp), tint = if (liked) Green else Ink,
        )
        Icon(
            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pause",
            Modifier.clickable { onToggle() }.padding(8.dp),
        )
    }
}
