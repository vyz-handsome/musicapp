package com.musicapp.player

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.session.MediaController
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

// Warna background diambil dari cover lagu (dominan), transisinya halus.
@Composable
private fun rememberCoverColor(url: String?, fallback: Color): Color {
    val ctx = LocalContext.current
    var target by remember(url) { mutableStateOf(fallback) }
    LaunchedEffect(url) {
        if (url == null) return@LaunchedEffect
        try {
            val res = ctx.imageLoader.execute(ImageRequest.Builder(ctx).data(url).allowHardware(false).build())
            val bmp = (res as? SuccessResult)?.drawable?.toBitmap() ?: return@LaunchedEffect
            val rgb = withContext(Dispatchers.Default) {
                val p = Palette.from(bmp).generate()
                p.getDominantColor(p.getMutedColor(0xFF444444.toInt()))
            }
            target = lerp(Color(rgb), Color.Black, 0.25f)
        } catch (e: Exception) { }
    }
    return animateColorAsState(target, tween(600), label = "bg").value
}

// Cover persegi: thumbnail 16:9 ditampilin utuh, sisanya diisi versi blur dari gambar itu sendiri.
@Composable
private fun ArtCover(t: Track, size: Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(8.dp)).background(Card), contentAlignment = Alignment.Center) {
        if (t.cover == null) Cover(t.color, size, 8.dp)
        else {
            AsyncImage(t.cover, null, Modifier.fillMaxSize().blur(28.dp), contentScale = ContentScale.Crop, alpha = 0.7f)
            AsyncImage(t.cover, null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
        }
    }
}

// Baris lirik: yang lagi dinyanyiin terang, yang belum waktunya redup + blur (makin jauh makin blur).
@Composable
private fun LyricText(text: String, isActive: Boolean, future: Boolean, dist: Int, size: TextUnit, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(if (isActive) 1f else if (future) 0.55f else 0.4f, label = "a")
    val radius by animateFloatAsState(if (future) minOf(dist, 4) * 1.5f else 0f, label = "r")
    Text(
        text, modifier.blur(radius.dp, BlurredEdgeTreatment.Unbounded),
        fontSize = size, lineHeight = size * 1.25f, fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = alpha),
    )
}

@Composable
private fun LyricsView(lines: List<LyricLine>?, active: Int, onSeek: (Long) -> Unit, modifier: Modifier) {
    when {
        lines == null -> Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
        lines.isEmpty() -> Box(modifier, contentAlignment = Alignment.Center) {
            Text("Lirik belum tersedia untuk lagu ini", color = Sub, textAlign = TextAlign.Center)
        }
        else -> {
            val state = rememberLazyListState()
            val off = with(LocalDensity.current) { -90.dp.roundToPx() }
            LaunchedEffect(active) { state.animateScrollToItem(maxOf(active, 0), off) }
            LazyColumn(state = state, modifier = modifier, contentPadding = PaddingValues(vertical = 120.dp)) {
                itemsIndexed(lines) { i, l ->
                    LyricText(
                        l.text, i == active, i > active, i - active, 26.sp,
                        Modifier.fillMaxWidth().clickable { onSeek(l.ms) }.padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsPreview(lines: List<LyricLine>?, active: Int, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick).padding(16.dp),
    ) {
        Text("Lyrics", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
        Spacer(Modifier.height(8.dp))
        when {
            lines == null -> Text("Memuat lirik...", color = Sub, fontSize = 14.sp)
            lines.isEmpty() -> Text("Lirik belum tersedia untuk lagu ini", color = Sub, fontSize = 14.sp)
            else -> {
                val a = maxOf(active, 0)
                (a until minOf(a + 3, lines.size)).forEach { i ->
                    LyricText(lines[i].text, i == active, i > active, i - active, 18.sp, Modifier.padding(vertical = 3.dp))
                }
            }
        }
    }
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
    var showLyrics by remember { mutableStateOf(false) }
    var lines by remember(t.id) { mutableStateOf<List<LyricLine>?>(null) }
    val base = rememberCoverColor(t.cover, t.color)

    LaunchedEffect(t.id) { lines = if (t.id.isEmpty()) emptyList() else Lyrics.get(t) }
    LaunchedEffect(controller, t) {
        while (true) {
            controller?.let { pos = it.currentPosition.coerceAtLeast(0L); dur = it.duration.coerceAtLeast(0L) }
            delay(250)
        }
    }
    val frac = if (dur > 0) (drag ?: (pos.toFloat() / dur)).coerceIn(0f, 1f) else 0f
    val active = lines?.indexOfLast { it.ms <= pos + 250 } ?: -1

    Box(
        Modifier.fillMaxSize().background(Bg)
            .background(Brush.verticalGradient(0f to base.copy(alpha = 0.95f), 0.8f to Bg, 1f to Bg)),
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

            if (showLyrics) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Cover(t.color, 52.dp, 4.dp, t.cover)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(t.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(t.artist, fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                    }
                    Icon(
                        if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Like",
                        Modifier.size(26.dp).clickable { onLike() }, tint = if (liked) Green else Color.White,
                    )
                }
                LyricsView(lines, active, { controller?.seekTo(it) }, Modifier.weight(1f).fillMaxWidth())
            } else {
                Spacer(Modifier.height(24.dp))
                BoxWithConstraints(Modifier.fillMaxWidth()) { ArtCover(t, maxWidth) }
                Spacer(Modifier.height(24.dp))
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
            }

            Spacer(Modifier.height(10.dp))
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
            Spacer(Modifier.height(12.dp))
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
            Spacer(Modifier.height(16.dp))
            if (!showLyrics) {
                LyricsPreview(lines, active) { showLyrics = true }
                Spacer(Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Share, "Share", Modifier.size(24.dp).clickable {
                    val link = if (t.id.startsWith("yt:")) " https://music.youtube.com/watch?v=" + t.id.substringAfter(':') else ""
                    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${t.title} - ${t.artist}$link") }
                    ctx.startActivity(Intent.createChooser(i, null))
                }, tint = Color.White.copy(alpha = 0.8f))
                Text(
                    if (showLyrics) "Cover" else "Lyrics", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.15f))
                        .clickable { showLyrics = !showLyrics }.padding(horizontal = 18.dp, vertical = 8.dp),
                )
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
