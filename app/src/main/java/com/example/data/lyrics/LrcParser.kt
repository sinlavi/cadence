package com.example.data.lyrics

import com.example.core.model.LrcLine
import com.example.core.model.LrcWord
import com.example.core.model.ParsedLrc
import java.util.Locale
import java.util.UUID

object LrcParser {
    private val lineTimestampRegex = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val wordTimestampRegex = Regex("<(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?>([^<]*)")
    private val metaTagRegex = Regex("^\\[(ti|ar|al|offset):(.*)]$", RegexOption.IGNORE_CASE)

    fun parse(rawText: String?): ParsedLrc {
        if (rawText.isNullOrBlank()) return ParsedLrc()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var offsetMs = 0L

        val parsedLines = mutableListOf<LrcLine>()
        var anySynced = false

        rawText.replace("\r\n", "\n").replace("\r", "\n").lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) {
                if (parsedLines.isNotEmpty()) {
                    parsedLines.add(LrcLine(id = UUID.randomUUID().toString(), timeMs = null, text = ""))
                }
                return@forEach
            }

            val metaMatch = metaTagRegex.matchEntire(trimmed)
            if (metaMatch != null) {
                val key = metaMatch.groupValues[1].lowercase(Locale.US)
                val value = metaMatch.groupValues[2].trim()
                when (key) {
                    "ti" -> title = value
                    "ar" -> artist = value
                    "al" -> album = value
                    "offset" -> offsetMs = value.toLongOrNull() ?: 0L
                }
                return@forEach
            }

            val timestampMatches = lineTimestampRegex.findAll(trimmed).toList()
            if (timestampMatches.isEmpty()) {
                parsedLines.add(
                    LrcLine(
                        id = UUID.randomUUID().toString(),
                        timeMs = null,
                        text = trimmed
                    )
                )
            } else {
                anySynced = true
                val contentPart = lineTimestampRegex.replace(trimmed, "").trim()
                val words = parseEnhancedWords(contentPart, offsetMs)
                val cleanText = if (words.isNotEmpty()) {
                    words.joinToString("") { it.text }.trim()
                } else {
                    contentPart
                }
                for (match in timestampMatches) {
                    val ms = parseTimestampToMs(
                        match.groupValues[1],
                        match.groupValues[2],
                        match.groupValues[3]
                    ) + offsetMs
                    parsedLines.add(
                        LrcLine(
                            id = UUID.randomUUID().toString(),
                            timeMs = ms.coerceAtLeast(0L),
                            text = cleanText,
                            words = words
                        )
                    )
                }
            }
        }

        val finalLines = if (anySynced) {
            parsedLines.filter { it.timeMs != null }.sortedBy { it.timeMs ?: Long.MAX_VALUE }
        } else {
            parsedLines
        }

        return ParsedLrc(
            title = title,
            artist = artist,
            album = album,
            offsetMs = offsetMs,
            lines = finalLines,
            isSynced = anySynced && finalLines.isNotEmpty()
        )
    }

    private fun parseEnhancedWords(content: String, offsetMs: Long): List<LrcWord> {
        val matches = wordTimestampRegex.findAll(content).toList()
        if (matches.isEmpty()) return emptyList()
        return matches.map { m ->
            val ms = parseTimestampToMs(m.groupValues[1], m.groupValues[2], m.groupValues[3]) + offsetMs
            LrcWord(timeMs = ms.coerceAtLeast(0L), text = m.groupValues[4])
        }
    }

    private fun parseTimestampToMs(minStr: String, secStr: String, fracStr: String): Long {
        val mins = minStr.toLongOrNull() ?: 0L
        val secs = secStr.toLongOrNull() ?: 0L
        val fracMs = when (fracStr.length) {
            0 -> 0L
            1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
            2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
            else -> fracStr.take(3).toLongOrNull() ?: 0L
        }
        return (mins * 60_000L) + (secs * 1_000L) + fracMs
    }

    fun formatTimestamp(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        val mins = safe / 60_000L
        val secs = (safe % 60_000L) / 1_000L
        val centis = (safe % 1_000L) / 10L
        return String.format(Locale.US, "[%02d:%02d.%02d]", mins, secs, centis)
    }

    fun serialize(lines: List<LrcLine>, title: String? = null, artist: String? = null, album: String? = null): String {
        val sb = StringBuilder()
        if (!title.isNullOrBlank()) sb.appendLine("[ti:$title]")
        if (!artist.isNullOrBlank()) sb.appendLine("[ar:$artist]")
        if (!album.isNullOrBlank()) sb.appendLine("[al:$album]")
        val hasAnyTime = lines.any { it.timeMs != null }
        val ordered = if (hasAnyTime) lines.sortedWith(compareBy(nullsLast()) { it.timeMs }) else lines
        for (line in ordered) {
            if (line.timeMs != null) {
                sb.appendLine("${formatTimestamp(line.timeMs)}${line.text}")
            } else {
                sb.appendLine(line.text)
            }
        }
        return sb.toString().trimEnd()
    }

    fun findActiveLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var low = 0
        var high = lines.lastIndex
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val t = lines[mid].timeMs
            if (t == null) {
                high = mid - 1
            } else if (t <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    fun shiftAllTimestamps(lines: List<LrcLine>, deltaMs: Long): List<LrcLine> {
        return lines.map { line ->
            if (line.timeMs != null) {
                line.copy(timeMs = (line.timeMs + deltaMs).coerceAtLeast(0L))
            } else {
                line
            }
        }
    }

    fun fromPlainLines(plainText: String): List<LrcLine> {
        return plainText.replace("\r\n", "\n").replace("\r", "\n")
            .lines()
            .map { LrcLine(id = UUID.randomUUID().toString(), timeMs = null, text = it) }
    }
}
