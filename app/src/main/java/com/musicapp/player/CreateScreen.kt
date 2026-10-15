package com.musicapp.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreateScreen(onCreate: (String) -> Unit) {
    var dialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Title("Create")
        CreateRow("Playlist", "Build a playlist with songs or episodes", Icons.Filled.MusicNote, true) {
            name = ""; dialog = true
        }
        CreateRow("Blend", "Combine tastes in a shared playlist. Coming soon", Icons.Filled.Group, false) {}
    }

    if (dialog) {
        AlertDialog(
            onDismissRequest = { dialog = false },
            containerColor = Card,
            title = { Text("Name your playlist") },
            text = {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    placeholder = { Text("My playlist") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = name.trim().ifEmpty { "My playlist" }
                    dialog = false
                    onCreate(n)
                }) { Text("Create", color = Green) }
            },
            dismissButton = { TextButton(onClick = { dialog = false }) { Text("Cancel", color = Sub) } },
        )
    }
}

@Composable
private fun CreateRow(title: String, desc: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f).clip(RoundedCornerShape(8.dp)).background(Card)
            .clickable(enabled = enabled) { onClick() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(32.dp))
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(desc, color = Sub, fontSize = 13.sp)
        }
    }
}
