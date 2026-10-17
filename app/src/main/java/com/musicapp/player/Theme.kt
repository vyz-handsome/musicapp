package com.musicapp.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warna mengikuti tema yang dipilih di Settings (Spotify atau Apple Music).
val isApple: Boolean get() = Prefs.theme == "apple"

val Bg: Color get() = when {
    !isApple -> Color(0xFF121212)
    Prefs.dark -> Color.Black
    else -> Color.White
}
val Card: Color get() = when {
    !isApple -> Color(0xFF2A2A2A)
    Prefs.dark -> Color(0xFF1C1C1E)
    else -> Color(0xFFF2F2F7)
}
val Green: Color get() = if (isApple) Color(0xFFFA243C) else Color(0xFF1ED760) // warna aksen
val Sub: Color get() = when {
    !isApple -> Color(0xFFB3B3B3)
    Prefs.dark -> Color(0xFF98989F)
    else -> Color(0xFF8E8E93)
}
val Ink: Color get() = if (isApple && !Prefs.dark) Color.Black else Color.White   // warna teks utama
val OnAccent: Color get() = if (isApple) Color.White else Color.Black

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val darkUi = !isApple || Prefs.dark
    val scheme = if (darkUi) {
        darkColorScheme(primary = Green, background = Bg, surface = Bg, onBackground = Ink, onSurface = Ink)
    } else {
        lightColorScheme(primary = Green, background = Bg, surface = Bg, onBackground = Ink, onSurface = Ink)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
