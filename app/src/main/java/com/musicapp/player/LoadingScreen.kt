package com.musicapp.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoadingScreen(firstTime: Boolean) {
    Column(
        Modifier.fillMaxSize().background(Bg).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.MusicNote, null, tint = Green, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator(color = Green)
        Spacer(Modifier.height(24.dp))
        Text(if (firstTime) "Menyiapkan mesin musik" else "Memuat...", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (firstTime) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Ini cuma sekali pas pertama kali install. Jangan tutup app ya, beberapa detik aja.",
                color = Sub, fontSize = 14.sp, textAlign = TextAlign.Center,
            )
        }
    }
}
