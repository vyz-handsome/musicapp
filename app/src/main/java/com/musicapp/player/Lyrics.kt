package com.musicapp.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

data class LyricLine(val ms: Long, val text: String)
data class LyricsResult(val lines: List<LyricLine>, val plain: String?)

// Lirik dari LRCLIB: cari yang sinkron dulu (durasi dicocokin), kalau gak ada pakai lirik biasa.
object Lyrics {
    private val cache = HashMap<String, LyricsResult>()
    fun clear() = cache.clear()
    private val stamp = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")

    suspend fun get(t: Track): LyricsResult = withContext(Dispatchers.IO) {
        cache[t.id]?.let { return@withContext it }
        try {
            val r = fetch(t)
            if (r.lines.isNotEmpty() || r.plain != null) cache[t.id] = r
            r
        } catch (e: Exception) { LyricsResult(emptyList(), null) }
    }

    private fun clean(s: String) = s
        .replace(Regex("\\(.*?\\)|\\[.*?]|【.*?】"), " ")
        .replace(Regex("(?i)\\b(official|lyrics?|lirik( lagu)?|video|audio|mv|hd|hq|4k|full album)\\b"), " ")
        .replace(Regex("\\s+"), " ").trim(' ', '-', '|')

    private fun search(q: String): JSONArray? {
        val c = URL("https://lrclib.net/api/search?q=" + URLEncoder.encode(q, "UTF-8")).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "MusicApp/0.1")
        c.connectTimeout = 10000; c.readTimeout = 15000
        return try { JSONArray(c.inputStream.bufferedReader().readText()) } catch (e: Exception) { null } finally { c.disconnect() }
    }

    private fun fetch(t: Track): LyricsResult {
        val dur = t.durationSec
        val full = clean(t.title)
        val qs = listOf(full.replace(" - ", " "), full.substringBefore(" - "), full.substringAfter(" - ", ""))
            .map { it.trim() }.filter { it.length >= 2 }.distinct()
        var plain: String? = null
        for (q in qs) {
            val arr = search(q) ?: continue
            val all = (0 until arr.length()).map { arr.getJSONObject(it) }
                .filter { dur == 0 || abs(it.optInt("duration") - dur) <= 8 }
                .sortedBy { if (dur == 0) 0 else abs(it.optInt("duration") - dur) }
            for (o in all) {
                if (o.isNull("syncedLyrics")) continue
                val lines = parse(o.getString("syncedLyrics"))
                if (lines.isNotEmpty()) return LyricsResult(lines, null)
            }
            if (plain == null) plain = all.firstOrNull { !it.isNull("plainLyrics") && it.optString("plainLyrics").isNotBlank() }?.getString("plainLyrics")
        }
        return LyricsResult(emptyList(), plain)
    }

    private fun parse(lrc: String): List<LyricLine> = lrc.lines().flatMap { line ->
        val stamps = stamp.findAll(line).toList()
        if (stamps.isEmpty()) return@flatMap emptyList<LyricLine>()
        val text = line.substring(stamps.last().range.last + 1).trim()
        stamps.map { m ->
            val (mm, ss, frac) = m.destructured
            LyricLine(mm.toLong() * 60000 + ss.toLong() * 1000 + frac.padEnd(3, '0').take(3).toLong(), text)
        }
    }.sortedBy { it.ms }
}
