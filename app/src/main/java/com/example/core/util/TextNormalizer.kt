package com.example.core.util

import java.text.Normalizer
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object TextNormalizer {

    private val diacriticsRegex = Regex("[\\p{InCombiningDiacriticalMarks}\\u064B-\\u065F\\u0670]")
    private val zwnjAndPunctRegex = Regex("[\\u200C\\u200D\\s]+")
    private val unsafeFilenameRegex = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")

    /**
     * Normalizes Latin, Persian/Farsi, and Arabic text for reliable search & matching
     * without mutating original display tags.
     */
    fun normalizeForSearch(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFKD)
        val noDiacritics = diacriticsRegex.replace(decomposed, "")
        val folded = buildString(noDiacritics.length) {
            for (ch in noDiacritics) {
                when (ch) {
                    'ي', 'ى', 'ئ' -> append('ی')
                    'ك' -> append('ک')
                    'ة', 'ۀ' -> append('ه')
                    'أ', 'إ', 'آ' -> append('ا')
                    'ؤ' -> append('و')
                    '۰', '٠' -> append('0')
                    '۱', '١' -> append('1')
                    '۲', '٢' -> append('2')
                    '۳', '٣' -> append('3')
                    '۴', '٤' -> append('4')
                    '۵', '٥' -> append('5')
                    '۶', '٦' -> append('6')
                    '۷', '٧' -> append('7')
                    '۸', '٨' -> append('8')
                    '۹', '٩' -> append('9')
                    else -> append(ch.lowercaseChar())
                }
            }
        }
        return zwnjAndPunctRegex.replace(folded, " ").trim()
    }

    fun buildTrackSearchIndex(
        title: String,
        artist: String,
        album: String,
        albumArtist: String,
        genre: String,
        composer: String,
        folderName: String
    ): String {
        return listOf(title, artist, album, albumArtist, genre, composer, folderName)
            .map { normalizeForSearch(it) }
            .filter { it.isNotEmpty() }
            .joinToString(" | ")
    }

    fun similarity(a: String, b: String): Float {
        val na = normalizeForSearch(a)
        val nb = normalizeForSearch(b)
        if (na.isEmpty() && nb.isEmpty()) return 1f
        if (na.isEmpty() || nb.isEmpty()) return 0f
        if (na == nb) return 1f
        val maxLen = max(na.length, nb.length)
        val dist = levenshtein(na, nb)
        return (1f - dist.toFloat() / maxLen.toFloat()).coerceIn(0f, 1f)
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        var prev = IntArray(n + 1) { it }
        var curr = IntArray(n + 1)
        for (i in 1..m) {
            curr[0] = i
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = min(
                    min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
                )
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[n]
    }

    fun sanitizeFilename(name: String): String {
        val cleaned = unsafeFilenameRegex.replace(name, "_").trim().trim('.')
        return cleaned.ifBlank { "Untitled" }
    }

    fun toTitleCase(input: String): String {
        if (input.isBlank()) return ""
        return input.trim()
            .split(Regex("\\s+"))
            .joinToString(" ") { word ->
                word.lowercase(Locale.getDefault()).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            }
    }

    fun formatDuration(ms: Long): String {
        val totalSec = (ms.coerceAtLeast(0L) / 1000L)
        val hours = totalSec / 3600L
        val mins = (totalSec % 3600L) / 60L
        val secs = totalSec % 60L
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

    fun fallbackLyricsKey(title: String, artist: String): String {
        return "${normalizeForSearch(artist)}::${normalizeForSearch(title)}"
    }
}
