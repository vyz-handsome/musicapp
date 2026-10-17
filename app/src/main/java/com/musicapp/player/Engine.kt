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

    suspend fun updateNow(ctx: Context): String = withContext(Dispatchers.IO) {
        if (!ytOk) return@withContext "yt-dlp belum siap"
        try {
            val s = YoutubeDL.getInstance().updateYoutubeDL(ctx.applicationContext)
            prefs(ctx).edit().putLong("updated", System.currentTimeMillis()).apply()
            "Update selesai: $s"
        } catch (e: Throwable) { "Gagal update: " + (e.message ?: "").take(60) }
    }

    fun version(ctx: Context): String = try {
        YoutubeDL.getInstance().version(ctx.applicationContext) ?: "-"
    } catch (e: Throwable) { "-" }

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
