package com.musicapp.player

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Pengaturan app. Semua berupa Compose state, jadi UI langsung berubah dan otomatis tersimpan.
object Prefs {
    private var sp: SharedPreferences? = null

    var theme by mutableStateOf("spotify")
        private set
    var dark by mutableStateOf(true)      // khusus tema Apple Music: Light / Dark
        private set
    var quality by mutableIntStateOf(2)   // 0 rendah, 1 normal, 2 tinggi
        private set
    var autoplay by mutableStateOf(true)
        private set
    var lyricsOn by mutableStateOf(true)
        private set
    var blurOn by mutableStateOf(true)
        private set

    fun load(ctx: Context) {
        val p = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
        sp = p
        theme = p.getString("theme", "spotify") ?: "spotify"
        dark = p.getBoolean("dark", true)
        quality = p.getInt("quality", 2)
        autoplay = p.getBoolean("autoplay", true)
        lyricsOn = p.getBoolean("lyrics", true)
        blurOn = p.getBoolean("blur", true)
    }

    fun chooseTheme(v: String) { theme = v; sp?.edit()?.putString("theme", v)?.apply() }
    fun chooseDark(v: Boolean) { dark = v; sp?.edit()?.putBoolean("dark", v)?.apply() }
    fun chooseQuality(v: Int) { quality = v; sp?.edit()?.putInt("quality", v)?.apply() }
    fun chooseAutoplay(v: Boolean) { autoplay = v; sp?.edit()?.putBoolean("autoplay", v)?.apply() }
    fun chooseLyrics(v: Boolean) { lyricsOn = v; sp?.edit()?.putBoolean("lyrics", v)?.apply() }
    fun chooseBlur(v: Boolean) { blurOn = v; sp?.edit()?.putBoolean("blur", v)?.apply() }
}
