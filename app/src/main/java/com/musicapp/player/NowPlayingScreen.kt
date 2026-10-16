package com.musicapp.player

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import kotlinx.coroutines.delay

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    t: Track, playing: Boolean, controller: MediaController?, liked: Boolean,
    shuffle: Boolean, repeat: Int, queue: List<Track>,
    onClose: () -> Unit, onLike: () -> Unit, onToggle: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit,
    onShuffle: () -> Unit, onRepeat: () -> Unit, onPick: (Track) -> Unit,
) {
    val ctx = LocalContext.current
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var drag by remember { mutableStateOf<Float?>(null) }
    var showQueue by remember { mutableStateOf(false) }

    LaunchedEffect(controller, t) {
        while (true) {
            controller?.let { pos = it.currentPosition.coerceAtLeast(0L); dur = it.duration.coerceAtLeast(0L) }
            delay(400)
        }
    }
    val frac = if (dur > 0) (drag ?: (pos.toFloat() / dur)).coerceIn(0f, 1f) else 0f

    Box(
        Modifier.fillMaxSize().background(Bg)
            .background(Brush.verticalGradient(0f to t.color.copy(alpha = 0.9f), 0.75f to Bg, 1f to Bg)),
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.KeyboardArrowDown, "Close", Modifier.size(32.dp).clickable { onClose() })
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PLAYING FROM SEARCH", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.7f))
                    Text("Your queue", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.size(32.dp))
            }
            Spacer(Modifier.height(24.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) { Cover(t.color, maxWidth, 8.dp, t.cover) }
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(t.artist, fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                }
                Icon(
                    if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Like",
                    Modifier.size(28.dp).clickable { onLike() }, tint = if (liked) Green else Color.White,
                )
            }
            Spacer(Modifier.height(12.dp))
            Slider(
                value = frac,
                onValueChange = { drag = it },
                onValueChangeFinished = { drag?.let { controller?.seekTo((it * dur).toLong()) }; drag = null },
                modifier = Modifier.fillMaxWidth().height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White, activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
                thumb = { Box(Modifier.size(12.dp).clip(CircleShape).background(Color.White)) },
            )
            Row(Modifier.fillMaxWidth()) {
                Text(fmt(drag?.let { (it * dur).toLong() } ?: pos), Modifier.weight(1f), fontSize = 12.sp, color = Sub)
                Text(fmt(dur), fontSize = 12.sp, color = Sub)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shuffle, "Shuffle", Modifier.size(26.dp).clickable { onShuffle() },
                    tint = if (shuffle) Green else Color.White.copy(alpha = 0.8f))
                Icon(Icons.Filled.SkipPrevious, "Previous", Modifier.size(40.dp).clickable { onPrev() })
                Box(Modifier.size(68.dp).clip(CircleShape).background(Color.White).clickable { onToggle() }, contentAlignment = Alignment.Center) {
                    Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pause", Modifier.size(38.dp), tint = Color.Black)
                }
                Icon(Icons.Filled.SkipNext, "Next", Modifier.size(40.dp).clickable { onNext() })
                Icon(if (repeat == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat, "Repeat",
                    Modifier.size(26.dp).clickable { onRepeat() },
                    tint = if (repeat > 0) Green else Color.White.copy(alpha = 0.8f))
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(Icons.Filled.Share, "Share", Modifier.size(24.dp).clickable {
                    val link = if (t.id.startsWith("yt:")) " https://music.youtube.com/watch?v=" + t.id.substringAfter(':') else ""
                    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${t.title} - ${t.artist}$link") }
                    ctx.startActivity(Intent.createChooser(i, null))
                }, tint = Color.White.copy(alpha = 0.8f))
                Icon(Icons.Filled.QueueMusic, "Queue", Modifier.size(24.dp).clickable { showQueue = true },
                    tint = Color.White.copy(alpha = 0.8f))
            }
        }
    }

    if (showQueue) {
        AlertDialog(
            onDismissRequest = { showQueue = false },
            containerColor = Card,
            title = { Text("Queue") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (queue.isEmpty()) Text("Nothing in the queue yet. Play a song from Search.", color = Sub)
                    queue.forEach { q ->
                        Row(Modifier.fillMaxWidth().clickable { showQueue = false; onPick(q) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Cover(q.color, 40.dp, url = q.cover)
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(q.title, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 14.sp,
                                    color = if (q == t) Green else Color.White)
                                Text(q.artist, color = Sub, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQueue = false }) { Text("Close", color = Green) } },
        )
    }
}
