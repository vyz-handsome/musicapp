package com.musicapp.player

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var ver by remember { mutableStateOf("...") }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { ver = withContext(Dispatchers.IO) { Engine.version(ctx) } }
    fun toast(m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()

    Column(Modifier.fillMaxSize().background(Bg).windowInsetsPadding(WindowInsets.systemBars)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ArrowBack, "Back", Modifier.clip(CircleShape).clickable { onBack() }.padding(8.dp))
            Text("Settings", Modifier.padding(start = 8.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            SectionLabel("Appearance")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeCard("Spotify", Color(0xFF121212), Color(0xFF2A2A2A), Color(0xFF1ED760), Color.White, !isApple, Modifier.weight(1f)) {
                    Prefs.setTheme("spotify")
                }
                ThemeCard(
                    "Apple Music", if (Prefs.dark) Color.Black else Color.White,
                    if (Prefs.dark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7), Color(0xFFFA243C),
                    if (Prefs.dark) Color.White else Color.Black, isApple, Modifier.weight(1f),
                ) { Prefs.setTheme("apple") }
            }
            if (isApple) {
                Spacer(Modifier.height(14.dp))
                ChoiceRow("Appearance", listOf("Light", "Dark"), if (Prefs.dark) 1 else 0) { Prefs.setDark(it == 1) }
            }

            SectionLabel("Playback")
            ChoiceRow("Audio quality", listOf("Low", "Normal", "High"), Prefs.quality, "Lower quality uses less data. Applies to the next song.") { Prefs.setQuality(it) }
            ToggleRow("Autoplay", "Keep playing the next song in the queue", Prefs.autoplay) { Prefs.setAutoplay(it) }

            SectionLabel("Lyrics")
            ToggleRow("Show lyrics", "Fetch and show synced lyrics in the player", Prefs.lyricsOn) { Prefs.setLyrics(it) }
            ToggleRow("Blur effect", "Blur lyric lines that are not playing (Android 12+)", Prefs.blurOn) { Prefs.setBlur(it) }

            SectionLabel("Storage")
            ActionRow("Clear cache", "Cached stream links, lyrics and cover images", true) {
                Api.clearCache(); Lyrics.clear()
                ctx.imageLoader.memoryCache?.clear(); ctx.imageLoader.diskCache?.clear()
                toast("Cache cleared")
            }

            SectionLabel("Music engine")
            ActionRow(if (busy) "Updating yt-dlp..." else "Update yt-dlp now", "Version: $ver", !busy) {
                busy = true
                scope.launch {
                    val m = Engine.updateNow(ctx)
                    busy = false
                    toast(m)
                    ver = withContext(Dispatchers.IO) { Engine.version(ctx) }
                }
            }

            SectionLabel("About")
            InfoRow("Version", "0.1")
            InfoRow("Music sources", "YouTube (yt-dlp), Audius")
            InfoRow("Lyrics", "LRCLIB")
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), Modifier.padding(top = 26.dp, bottom = 10.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Sub, letterSpacing = 0.8.sp)
}

@Composable
private fun ThemeCard(name: String, bg: Color, card: Color, accent: Color, ink: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(bg)
            .border(2.dp, if (selected) Green else Sub.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable { onClick() }.padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(accent))
            Box(Modifier.padding(start = 8.dp).width(40.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(card))
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(6.dp)).background(card))
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, Modifier.weight(1f), color = ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (selected) Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = accent)
        }
    }
}

@Composable
private fun ToggleRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 16.sp)
            Text(sub, fontSize = 13.sp, color = Sub)
        }
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Green, checkedThumbColor = Color.White, checkedBorderColor = Green),
        )
    }
}

@Composable
private fun ChoiceRow(title: String, options: List<String>, selected: Int, sub: String? = null, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, fontSize = 16.sp)
        if (sub != null) Text(sub, fontSize = 13.sp, color = Sub)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { i, label -> Chip(label, selected == i, Modifier.clickable { onSelect(i) }) }
        }
    }
}

@Composable
private fun ActionRow(title: String, sub: String, enabled: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f).clickable(enabled = enabled) { onClick() }.padding(vertical = 10.dp)) {
        Text(title, fontSize = 16.sp)
        Text(sub, fontSize = 13.sp, color = Sub)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(title, Modifier.weight(1f), fontSize = 16.sp)
        Text(value, fontSize = 14.sp, color = Sub)
    }
}
