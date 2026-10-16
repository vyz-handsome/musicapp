package com.musicapp.player

import androidx.compose.ui.graphics.Color
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private val apiPalette = listOf(
    Color(0xFF6A5AE0), Color(0xFF1E9E78), Color(0xFFD9534F), Color(0xFF2F7DD1),
    Color(0xFFE08A1E), Color(0xFF8E2DE2), Color(0xFF0F7A8A),
)
private fun colorFor(id: String) = apiPalette[(id.hashCode() and 0x7fffffff) % apiPalette.size]
private fun JSONObject.str(k: String) = if (isNull(k)) "" else optString(k)

// Sumber lagu: yt-dlp (di dalam app) dulu, kalau gagal/kosong pindah ke Audius.
object Api {
    private const val AU = "https://api.audius.co"
    private const val APP = "musicapp"
    private val cache = HashMap<String, Pair<String, Long>>()

    suspend fun search(q: String): List<Track> = withContext(Dispatchers.IO) {
        var list = emptyList<Track>()
        if (Engine.ytOk) {
            try { list = ytSearch(q) } catch (e: Exception) { }
        }
        if (list.isNotEmpty()) list else auSearch(q)
    }

    suspend fun streamUrl(t: Track): String = withContext(Dispatchers.IO) {
        cache[t.id]?.let { if (it.second > System.currentTimeMillis()) return@withContext it.first }
        val src = t.id.substringBefore(':')
        val raw = t.id.substringAfter(':')
        var url: String? = null
        if (src == "yt" && Engine.ytOk) {
            try { url = ytStream(raw) } catch (e: Exception) { }
        }
        if (url == null && src == "au") url = auStream(raw)
        if (url == null) {
            val alt = auSearch("${t.title} ${t.artist}").firstOrNull() ?: error("no source")
            url = auStream(alt.id.substringAfter(':'))
        }
        cache[t.id] = url to System.currentTimeMillis() + 20 * 60 * 1000
        url
    }

    private fun ytSearch(q: String): List<Track> {
        val r = YoutubeDLRequest("ytsearch10:$q")
        r.addOption("--flat-playlist"); r.addOption("--dump-json"); r.addOption("--no-warnings")
        val out = YoutubeDL.getInstance().execute(r).out
        return out.lines().filter { it.isNotBlank() }.mapNotNull { runCatching { JSONObject(it) }.getOrNull() }.map { j ->
            val id = j.getString("id")
            Track(j.str("title"), j.str("channel").ifEmpty { j.str("uploader") }, colorFor(id),
                "yt:$id", "https://i.ytimg.com/vi/$id/mqdefault.jpg")
        }
    }

    private fun ytStream(raw: String): String {
        require(Regex("^[\\w-]{6,20}$").matches(raw))
        val url = "https://www.youtube.com/watch?v=$raw"
        return try { extract(url, null) } catch (e: Exception) { extract(url, "youtube:player_client=android_vr,web_safari") }
    }

    private fun extract(url: String, clientArgs: String?): String {
        val r = YoutubeDLRequest(url)
        r.addOption("-f", "bestaudio[ext=m4a]/bestaudio/best")
        r.addOption("-g"); r.addOption("--no-playlist"); r.addOption("--no-warnings")
        if (clientArgs != null) r.addOption("--extractor-args", clientArgs)
        return YoutubeDL.getInstance().execute(r).out.lines().first { it.trim().startsWith("http") }.trim()
    }

    private fun auSearch(q: String): List<Track> {
        val c = URL("$AU/v1/tracks/search?query=${URLEncoder.encode(q, "UTF-8")}&limit=10&app_name=$APP")
            .openConnection() as HttpURLConnection
        c.connectTimeout = 10000; c.readTimeout = 20000
        try {
            val data = JSONObject(c.inputStream.bufferedReader().readText()).optJSONArray("data") ?: return emptyList()
            return (0 until data.length()).map { i ->
                val o = data.getJSONObject(i)
                val id = o.getString("id")
                val cover = o.optJSONObject("artwork")?.str("480x480")?.ifEmpty { null }
                Track(o.str("title"), o.optJSONObject("user")?.str("name") ?: "", colorFor(id), "au:$id", cover)
            }
        } finally { c.disconnect() }
    }

    private fun auStream(raw: String) = "$AU/v1/tracks/${URLEncoder.encode(raw, "UTF-8")}/stream?app_name=$APP"
}
