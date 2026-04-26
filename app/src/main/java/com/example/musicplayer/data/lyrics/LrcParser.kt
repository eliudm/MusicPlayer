package com.example.musicplayer.data.lyrics

import com.example.musicplayer.domain.model.LyricsLine

object LrcParser {
    private val lineRegex = Regex("""\[(\d+):(\d+)\.(\d+)\](.*)""")

    fun parse(lrc: String): List<LyricsLine> =
        lrc.lines().mapNotNull { line ->
            lineRegex.find(line.trim())?.let { m ->
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val fracMs = when (frac.length) {
                    3 -> frac.toLong()
                    else -> frac.toLong() * 10
                }
                val timeMs = min * 60_000L + sec * 1_000L + fracMs
                val text = m.groupValues[4].trim()
                if (text.isNotEmpty()) LyricsLine(timeMs, text) else null
            }
        }.sortedBy { it.timeMs }

    fun plainToLines(plain: String): List<LyricsLine> {
        var timeMs = 0L
        return plain.lines()
            .filter { it.isNotBlank() }
            .map { line -> LyricsLine(timeMs, line.trim()).also { timeMs += 3_000L } }
    }

    fun toRawLrc(lines: List<LyricsLine>): String =
        lines.joinToString("\n") { line ->
            val min = line.timeMs / 60_000
            val sec = (line.timeMs % 60_000) / 1_000
            val cs = (line.timeMs % 1_000) / 10
            "[%02d:%02d.%02d] %s".format(min, sec, cs, line.text)
        }
}
