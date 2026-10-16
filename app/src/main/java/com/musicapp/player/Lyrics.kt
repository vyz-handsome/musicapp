package com.musicapp.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LyricLine(val ms: Long, val text: String)

// Lirik sinkron dari LRCLIB (gratis, tanpa API key).
object Lyrics {
    private val cache = HashMap<String, List<LyricLine>>()
    private val stamp = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")

    suspend fun get(t: Track): List<LyricLine> = withContext(Dispatchers.IO) {
        cache[t.id]?.let { return@withContext it }
        try {
            val r = fetch(t)
            cache[t.id] = r
            r
        } catch (e: Exception) { emptyList() }
    }

    private fun clean(s: String) = s
        .replace(Regex("\\(.*?\\)|\\[.*?]|【.*?】"), " ")
        .replace(Regex("(?i)\\b(official|lyrics?|lirik( lagu)?|video|audio|mv|hd|hq|4k|full album)\\b"), " ")
        .replace(Regex("\\s+"), " ").trim(' ', '-', '|')

    private fun fetch(t: Track): List<LyricLine> {
        val title = clean(t.title)
        val queries = listOf(title.replace(" - ", " "), title.substringBefore(" - ")).distinct().filter { it.length >= 2 }
        for (q in queries) {
            val c = URL("https://lrclib.net/api/search?q=" + URLEncoder.encode(q, "UTF-8")).openConnection() as HttpURLConnection
            c.setRequestProperty("User-Agent", "MusicApp/0.1")
            c.connectTimeout = 10000; c.readTimeout = 15000
            try {
                val arr = JSONArray(c.inputStream.bufferedReader().readText())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    if (o.isNull("syncedLyrics")) continue
                    val lines = parse(o.getString("syncedLyrics"))
                    if (lines.isNotEmpty()) return lines
                }
            } finally { c.disconnect() }
        }
        return emptyList()
    }

    private fun parse(lrc: String): List<LyricLine> = lrc.lines().flatMap { line ->
        val stamps = stamp.findAll(line).toList()
        if (stamps.isEmpty()) return@flatMap emptyList<LyricLine>()
        val text = line.substring(stamps.last().range.last + 1).trim()
        stamps.map { m ->
            val (mm, ss, frac) = m.destructured
            LyricLine(mm.toLong() * 60000 + ss.toLong() * 1000 + frac.padEnd(3, '0').take(3).toLong(), text)
        }
    }.filter { it.text.isNotEmpty() }.sortedBy { it.ms }
}
