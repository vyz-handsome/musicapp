package com.musicapp.player

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Menyiapkan yt-dlp di dalam app. Extract pertama kali agak lama, berikutnya cepat.
object Engine {
    @Volatile var done = false
    @Volatile var ytOk = false

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("engine", Context.MODE_PRIVATE)

    fun wasReady(ctx: Context) = prefs(ctx).getBoolean("ready", false)

    suspend fun init(ctx: Context) = withContext(Dispatchers.IO) {
        try {
            YoutubeDL.getInstance().init(ctx.applicationContext)
            ytOk = true
            prefs(ctx).edit().putBoolean("ready", true).apply()
        } catch (e: Throwable) {
            ytOk = false // app tetap jalan, pencarian pindah ke Audius
        }
        done = true
    }

    // Update yt-dlp di background kalau udah lebih dari 3 hari.
    suspend fun updateIfStale(ctx: Context) = withContext(Dispatchers.IO) {
        if (!ytOk) return@withContext
        val p = prefs(ctx)
        val now = System.currentTimeMillis()
        if (now - p.getLong("updated", 0L) < 3L * 24 * 3600 * 1000) return@withContext
        try {
            YoutubeDL.getInstance().updateYoutubeDL(ctx.applicationContext)
            p.edit().putLong("updated", now).apply()
        } catch (e: Throwable) { }
    }
}
