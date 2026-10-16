package com.musicapp.player

import android.content.Intent
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import kotlin.math.abs

// Layar pemutar ini meniru player di spotibot (gaya Apple Music).
private val Accent = Color(0xFFFC3C44)
private val canBlur = Build.VERSION.SDK_INT >= 31

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

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
            target = lerp(Color(rgb), Color.Black, 0.35f)
        } catch (e: Exception) { }
    }
    return animateColorAsState(target, tween(800), label = "bg").value
}

private fun satFilter(v: Float) = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(v) })

// Background: cover di-blur besar + gradasi gelap (crossfade pas ganti lagu).
@Composable
private fun NowBackground(cover: String?, base: Color) {
    Box(Modifier.fillMaxSize().background(base)) {
        if (cover != null && canBlur) {
            Crossfade(cover, animationSpec = tween(1200), label = "bgc") { url ->
                AsyncImage(
                    url, null, Modifier.fillMaxSize().scale(1.25f).blur(50.dp),
                    contentScale = ContentScale.Crop, colorFilter = satFilter(1.5f),
                )
            }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Black.copy(0.1f), 0.45f to Color.Black.copy(0.35f), 1f to Color.Black.copy(0.8f))
            )
        )
    }
}

@Composable
private fun ArtCover(t: Track, size: Dp, radius: Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(radius)).background(Card), contentAlignment = Alignment.Center) {
        if (t.cover == null) Cover(t.color, size, radius)
        else {
            AsyncImage(t.cover, null, Modifier.fillMaxSize().blur(24.dp), contentScale = ContentScale.Crop, alpha = 0.7f)
            AsyncImage(t.cover, null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
        }
    }
}

// Cover kecil + glow berdenyut pas lagu jalan, mengecil pas pause. Tap = play/pause.
@Composable
private fun CoverBlock(t: Track, playing: Boolean, size: Dp, onTap: () -> Unit) {
    val scale by animateFloatAsState(if (playing) 1f else 0.86f, tween(700), label = "cs")
    val glow by animateFloatAsState(if (playing) 1f else 0f, tween(800), label = "gl")
    val pulse by rememberInfiniteTransition(label = "pu").animateFloat(
        0.45f, 0.75f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "pa",
    )
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (t.cover != null && canBlur) {
            AsyncImage(
                t.cover, null,
                Modifier.size(size).scale(1.05f * scale).alpha(pulse * glow).blur(38.dp, BlurredEdgeTreatment.Unbounded),
                contentScale = ContentScale.Crop, colorFilter = satFilter(1.6f),
            )
        }
        Box(
            Modifier.size(size).scale(scale).shadow(26.dp, RoundedCornerShape(16.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTap() },
        ) { ArtCover(t, size, 16.dp) }
    }
}

@Composable
private fun EqBars(playing: Boolean) {
    val tr = rememberInfiniteTransition(label = "eq")
    Row(Modifier.height(18.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        listOf(1000, 800, 1150, 900).forEach { d ->
            val s by tr.animateFloat(0.2f, 1f, infiniteRepeatable(tween(d), RepeatMode.Reverse), label = "b")
            Box(Modifier.width(3.dp).height(18.dp * (if (playing) s else 0.2f)).clip(RoundedCornerShape(2.dp)).background(Accent))
        }
    }
}

@Composable
private fun RoundBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, on: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(if (on) Accent else Color.White.copy(alpha = 0.12f)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(19.dp)) }
}

// Satu baris lirik: terang di baris sekarang, makin jauh makin redup + blur tipis (sama kayak bot).
@Composable
private fun LyricLineRow(text: String, d: Int, onClick: () -> Unit) {
    val a = abs(d)
    val alpha by animateFloatAsState(when { d == 0 -> 1f; a == 1 -> 0.55f; a == 2 -> 0.32f; else -> 0.18f }, tween(350), label = "la")
    val blur by animateFloatAsState(when { d == 0 -> 0f; a == 1 -> 0.5f; a == 2 -> 1f; else -> 1.5f }, tween(450), label = "lb")
    val scale by animateFloatAsState(if (d == 0) 1.05f else 0.97f, tween(450), label = "ls")
    val dy by animateFloatAsState(when { d == 0 -> 0f; d < 0 -> -16f; else -> 16f }, tween(450), label = "ld")
    Text(
        text,
        Modifier.fillMaxWidth().padding(bottom = 14.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .graphicsLayer {
                this.alpha = alpha; scaleX = scale; scaleY = scale; translationY = dy.dp.toPx()
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .blur(blur.dp, BlurredEdgeTreatment.Unbounded),
        fontSize = 24.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp, color = Color.White,
    )
}

private fun Modifier.fadeEdges() = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, 0.16f to Color.Black, 0.84f to Color.Black, 1f to Color.Transparent),
            blendMode = BlendMode.DstIn,
        )
    }

@Composable
private fun LyricsPane(res: LyricsResult?, active: Int, onSeek: (Long) -> Unit, modifier: Modifier) {
    when {
        res == null -> Box(modifier, Alignment.TopCenter) { Text("Memuat lirik...", Modifier.padding(top = 40.dp), color = Color.White.copy(alpha = 0.5f), fontSize = 15.sp) }
        res.lines.isNotEmpty() -> BoxWithConstraints(modifier) {
            val state = rememberLazyListState()
            var userUntil by remember { mutableLongStateOf(0L) }
            val pad = maxHeight * 0.34f
            LaunchedEffect(res, active) {
                if (active < 0 || System.currentTimeMillis() < userUntil) return@LaunchedEffect
                val idx = active + 1
                var info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == idx }
                if (info == null) {
                    state.scrollToItem(idx)
                    info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == idx }
                }
                if (info != null) {
                    val center = (state.layoutInfo.viewportStartOffset + state.layoutInfo.viewportEndOffset) / 2
                    state.animateScrollBy((info.offset + info.size / 2 - center).toFloat(), tween(450))
                }
            }
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize().fadeEdges().pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) { awaitPointerEvent(PointerEventPass.Initial); userUntil = System.currentTimeMillis() + 4000 }
                    }
                },
            ) {
                item { Spacer(Modifier.height(pad)) }
                itemsIndexed(res.lines) { i, l -> LyricLineRow(l.text.ifEmpty { "♪" }, i - active) { onSeek(l.ms) } }
                item { Spacer(Modifier.height(pad)) }
            }
        }
        res.plain != null -> Column(modifier.fadeEdges().verticalScroll(rememberScrollState()).padding(vertical = 14.dp)) {
            Text(res.plain, fontSize = 17.sp, lineHeight = 28.sp, color = Color.White.copy(alpha = 0.85f))
        }
        else -> Box(modifier, Alignment.TopCenter) { Text("Lirik tidak ditemukan.", Modifier.padding(top = 40.dp), color = Color.White.copy(alpha = 0.5f), fontSize = 15.sp) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    t: Track, playing: Boolean, loading: Boolean, controller: MediaController?, liked: Boolean,
    shuffle: Boolean, repeat: Int, queue: List<Track>,
    onClose: () -> Unit, onLike: () -> Unit, onToggle: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit,
    onShuffle: () -> Unit, onRepeat: () -> Unit, onPick: (Track) -> Unit,
) {
    val ctx = LocalContext.current
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var drag by remember { mutableStateOf<Float?>(null) }
    var showQueue by remember { mutableStateOf(false) }
    var res by remember(t.id) { mutableStateOf<LyricsResult?>(null) }
    val base = rememberCoverColor(t.cover, t.color)

    LaunchedEffect(t.id) { res = if (t.id.isEmpty()) LyricsResult(emptyList(), null) else Lyrics.get(t) }
    LaunchedEffect(controller, t) {
        while (true) {
            controller?.let { pos = it.currentPosition.coerceAtLeast(0L); dur = it.duration.coerceAtLeast(0L) }
            delay(200)
        }
    }
    val frac = if (dur > 0) (drag ?: (pos.toFloat() / dur)).coerceIn(0f, 1f) else 0f
    val shownPos = drag?.let { (it * dur).toLong() } ?: pos
    val active = res?.lines?.indexOfLast { it.ms <= pos + 150 } ?: -1
    val upNext = queue.getOrNull(queue.indexOf(t) + 1)

    Box(Modifier.fillMaxSize()) {
        NowBackground(t.cover, base)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val coverSize = minOf(maxWidth * 0.62f, maxHeight * 0.31f)
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RoundBtn(Icons.Filled.KeyboardArrowDown, "Close") { onClose() }
                    Text(
                        if (upNext != null) "Berikutnya: ${upNext.title}" else "", Modifier.weight(1f).padding(horizontal = 8.dp),
                        fontSize = 12.sp, color = Color.White.copy(alpha = 0.62f), textAlign = TextAlign.Center,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.size(38.dp))
                }
                Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
                    CoverBlock(t, playing, coverSize) { onToggle() }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(t.title, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(t.artist, Modifier.padding(top = 3.dp), fontSize = 16.sp, color = Color.White.copy(alpha = 0.62f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(Modifier.padding(start = 12.dp, bottom = 4.dp)) { EqBars(playing) }
                    Icon(
                        if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Like",
                        Modifier.padding(start = 14.dp).size(26.dp).clickable { onLike() }, tint = if (liked) Accent else Color.White,
                    )
                }
                LyricsPane(res, active, { controller?.seekTo(it) }, Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp))

                Slider(
                    value = frac,
                    onValueChange = { drag = it },
                    onValueChangeFinished = { drag?.let { controller?.seekTo((it * dur).toLong()) }; drag = null },
                    modifier = Modifier.fillMaxWidth().height(22.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White, activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                    ),
                    thumb = { Box(Modifier.size(13.dp).clip(CircleShape).background(Color.White)) },
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(fmt(shownPos), Modifier.weight(1f), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    Text("-" + fmt(dur - shownPos), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.SkipPrevious, "Previous", Modifier.size(34.dp).clickable { onPrev() })
                    Icon(Icons.Filled.Replay10, "Back 10s", Modifier.size(34.dp).clickable { controller?.seekTo((pos - 10000).coerceAtLeast(0L)) })
                    Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(68.dp).clip(CircleShape).background(Color.White).clickable { onToggle() }, contentAlignment = Alignment.Center) {
                            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pause", Modifier.size(34.dp), tint = Color.Black)
                        }
                        if (loading) CircularProgressIndicator(Modifier.size(78.dp), color = Color.White, strokeWidth = 3.dp)
                    }
                    Icon(Icons.Filled.Forward10, "Forward 10s", Modifier.size(34.dp).clickable {
                        controller?.seekTo(if (dur > 0) minOf(dur, pos + 10000) else pos + 10000)
                    })
                    Icon(Icons.Filled.SkipNext, "Next", Modifier.size(34.dp).clickable { onNext() })
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shuffle, "Shuffle", Modifier.size(22.dp).clickable { onShuffle() }, tint = if (shuffle) Accent else Color.White.copy(alpha = 0.75f))
                    Icon(Icons.Filled.Share, "Share", Modifier.size(22.dp).clickable {
                        val link = if (t.id.startsWith("yt:")) " https://music.youtube.com/watch?v=" + t.id.substringAfter(':') else ""
                        val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${t.title} - ${t.artist}$link") }
                        ctx.startActivity(Intent.createChooser(i, null))
                    }, tint = Color.White.copy(alpha = 0.75f))
                    Icon(Icons.Filled.QueueMusic, "Queue", Modifier.size(22.dp).clickable { showQueue = true }, tint = Color.White.copy(alpha = 0.75f))
                    Icon(if (repeat == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat, "Repeat", Modifier.size(22.dp).clickable { onRepeat() },
                        tint = if (repeat > 0) Accent else Color.White.copy(alpha = 0.75f))
                }
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
                        Row(Modifier.fillMaxWidth().clickable { showQueue = false; onPick(q) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Cover(q.color, 40.dp, url = q.cover)
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(q.title, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 14.sp, color = if (q == t) Accent else Color.White)
                                Text(q.artist, color = Sub, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQueue = false }) { Text("Close", color = Accent) } },
        )
    }
}
