package com.musicapp.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF121212)
val Card = Color(0xFF2A2A2A)
val Green = Color(0xFF1ED760)
val Sub = Color(0xFFB3B3B3)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Green,
            background = Bg,
            surface = Bg,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
        content = content,
    )
}
