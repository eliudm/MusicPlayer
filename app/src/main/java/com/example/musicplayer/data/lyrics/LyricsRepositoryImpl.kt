package com.example.musicplayer.data.lyrics

import android.util.Log
import com.example.musicplayer.data.local.MusicDatabase
import com.example.musicplayer.data.local.entity.LyricsEntity
import com.example.musicplayer.domain.model.Lyrics
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.LyricsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepositoryImpl @Inject constructor(
    private val db: MusicDatabase
) : LyricsRepository {

    private val TAG = "LyricsRepo"

    // Regex to extract: Artist "Song Title" or Artist 'Song Title'
    private val embeddedTitleRegex = Regex("""^(.+?)\s+["'](.+?)["']$""")

    override suspend fun getLyrics(song: Song): Result<Lyrics?> {
        return try {
            val cached = db.lyricsDao().get(song.id)
            if (cached != null) {
                Log.d(TAG, "cache hit songId=${song.id} isManual=${cached.isManual}")
                val lyrics = cached.toLyrics()
                // Empty entity means previously not found — return null so ViewModel shows NOT_FOUND
                return Result.success(if (lyrics.lines.isNotEmpty()) lyrics else null)
            }
            Log.d(TAG, "fetching: '${song.title}' by '${song.artist}'")
            val fetched = fetchLyrics(song)
            // Cache the result (even null — stores empty sentinel to skip future network calls)
            db.lyricsDao().upsert(
                LyricsEntity(
                    songId = song.id,
                    lrcContent = if (fetched?.isSynced == true) LrcParser.toRawLrc(fetched.lines) else null,
                    plainContent = if (fetched?.isSynced == false) fetched.lines.joinToString("\n") { it.text } else null,
                    isSynced = fetched?.isSynced ?: false,
                    isManual = false
                )
            )
            Result.success(fetched)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getLyrics error", e)
            Result.failure(e)
        }
    }

    override suspend fun saveManualLyrics(songId: Long, text: String) {
        val parsed = LrcParser.parse(text)
        val isSynced = parsed.any { it.timeMs > 0 }
        db.lyricsDao().upsert(
            LyricsEntity(
                songId = songId,
                lrcContent = if (isSynced) text else null,
                plainContent = if (!isSynced) text else null,
                isSynced = isSynced,
                isManual = true
            )
        )
    }

    override suspend fun clearLyrics(songId: Long) = db.lyricsDao().delete(songId)

    // ── Main fetch orchestrator ───────────────────────────────────────────────

    private suspend fun fetchLyrics(song: Song): Lyrics? {
        // Determine search parameters — try to handle "Artist "Song"" compilation titles
        val rawTitle = song.title.trim()
        val rawArtist = song.artist.trim().takeIf { it.isNotBlank() && it != "<unknown>" }

        // If title embeds the artist ("Michael Combs "Carry Me Jesus""), extract both
        val embeddedMatch = embeddedTitleRegex.matchEntire(rawTitle)
        val extractedTitle = embeddedMatch?.groupValues?.get(2)
        val extractedArtist = embeddedMatch?.groupValues?.get(1)

        // 1. Try LRCLIB with original metadata
        fetchFromLrcLib(rawTitle, rawArtist, song.duration)?.let { return it }

        // 2. If we detected embedded artist in title, retry with extracted title/artist
        if (extractedTitle != null) {
            Log.d(TAG, "retrying with extracted title='$extractedTitle' artist='$extractedArtist'")
            fetchFromLrcLib(extractedTitle, extractedArtist ?: rawArtist, song.duration)
                ?.let { return it }
        }

        // 3. lyrics.ovh fallback (different database, plain text only)
        val ovhArtist = extractedArtist ?: rawArtist
        val ovhTitle = extractedTitle ?: rawTitle
        if (ovhArtist != null) {
            fetchFromLyricsOvh(ovhTitle, ovhArtist)?.let { return it }
        }

        Log.d(TAG, "no lyrics found in any source for '${song.title}'")
        return null
    }

    // ── LRCLIB ───────────────────────────────────────────────────────────────

    private suspend fun fetchFromLrcLib(
        title: String,
        artist: String?,
        durationMs: Long
    ): Lyrics? = withContext(Dispatchers.IO) {
        val durationSec = (durationMs / 1000).toInt()
        Log.d(TAG, "LRCLIB: title='$title' artist='$artist' dur=$durationSec")

        // Strategy 1 — exact match with duration
        if (artist != null && durationSec > 0) {
            tryFetch(
                "https://lrclib.net/api/get?" +
                "track_name=${enc(title)}" +
                "&artist_name=${enc(artist)}" +
                "&duration=$durationSec"
            )?.let { parseLrcLibObj(it) }?.let { return@withContext it }
        }

        // Strategy 2 — track_name + artist_name search
        val searchUrl = buildString {
            append("https://lrclib.net/api/search?track_name=${enc(title)}")
            if (artist != null) append("&artist_name=${enc(artist)}")
        }
        firstFromArray(tryFetch(searchUrl))?.let { return@withContext it }

        // Strategy 3 — q= combined query
        val q = if (artist != null) "${enc(artist)}+${enc(title)}" else enc(title)
        firstFromArray(tryFetch("https://lrclib.net/api/search?q=$q"))?.let { return@withContext it }

        // Strategy 4 — title only
        firstFromArray(tryFetch("https://lrclib.net/api/search?q=${enc(title)}"))
            ?.let { return@withContext it }

        null
    }

    private fun firstFromArray(json: String?): Lyrics? {
        json ?: return null
        val arr = runCatching { JSONArray(json) }.getOrNull() ?: return null
        if (arr.length() == 0) return null
        return parseLrcLibObj(arr.getJSONObject(0).toString())
    }

    private fun parseLrcLibObj(json: String): Lyrics? {
        val obj = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val synced = obj.optString("syncedLyrics").takeIf { it.isNotBlank() }
        val plain = obj.optString("plainLyrics").takeIf { it.isNotBlank() }
        return when {
            synced != null -> LrcParser.parse(synced).takeIf { it.isNotEmpty() }
                ?.let { Lyrics(lines = it, isSynced = true, isManual = false) }
            plain != null -> Lyrics(
                lines = LrcParser.plainToLines(plain),
                isSynced = false,
                isManual = false
            )
            else -> null
        }
    }

    // ── lyrics.ovh fallback ───────────────────────────────────────────────────

    private fun fetchFromLyricsOvh(title: String, artist: String): Lyrics? {
        Log.d(TAG, "lyrics.ovh: '$title' by '$artist'")
        val url = "https://api.lyrics.ovh/v1/${enc(artist)}/${enc(title)}"
        val json = tryFetch(url) ?: return null
        val text = runCatching { JSONObject(json).getString("lyrics") }.getOrNull()
        if (text.isNullOrBlank()) return null
        Log.d(TAG, "lyrics.ovh: found ${text.lines().size} lines")
        return Lyrics(lines = LrcParser.plainToLines(text), isSynced = false, isManual = false)
    }

    // ── HTTP helper ───────────────────────────────────────────────────────────

    private fun tryFetch(urlStr: String): String? {
        Log.d(TAG, "GET $urlStr")
        return try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "MusicPlayerApp/1.0")
            conn.setRequestProperty("Lrclib-Client", "MusicPlayerApp v1.0")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val code = conn.responseCode
            Log.d(TAG, "HTTP $code ← $urlStr")
            if (code != 200) {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
                Log.w(TAG, "non-200: $err")
                return null
            }
            conn.inputStream.bufferedReader().readText().also {
                Log.d(TAG, "response length=${it.length}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "network error: ${e.javaClass.simpleName} ${e.message}")
            null
        }
    }

    private fun LyricsEntity.toLyrics(): Lyrics {
        val lines = when {
            isSynced && lrcContent != null -> LrcParser.parse(lrcContent)
            plainContent != null -> LrcParser.plainToLines(plainContent)
            else -> emptyList()
        }
        return Lyrics(lines = lines, isSynced = isSynced, isManual = isManual)
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
