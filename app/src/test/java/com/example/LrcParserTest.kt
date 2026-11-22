package com.example

import com.example.core.model.LrcLine
import com.example.data.lyrics.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun parseSyncedAndMultiTimestampLrc_sortsChronologicallyAndAppliesOffset() {
        val raw = """
            [ti:Midnight Echo]
            [ar:Cadence Artist]
            [al:Night Sessions]
            [offset:500]
            [00:10.00][00:40.00]Chorus line
            [00:05.25]First verse
        """.trimIndent()

        val parsed = LrcParser.parse(raw)
        assertTrue(parsed.isSynced)
        assertEquals("Midnight Echo", parsed.title)
        assertEquals("Cadence Artist", parsed.artist)
        assertEquals("Night Sessions", parsed.album)
        assertEquals(500L, parsed.offsetMs)
        assertEquals(3, parsed.lines.size)

        assertEquals(5_750L, parsed.lines[0].timeMs)
        assertEquals("First verse", parsed.lines[0].text)
        assertEquals(10_500L, parsed.lines[1].timeMs)
        assertEquals("Chorus line", parsed.lines[1].text)
        assertEquals(40_500L, parsed.lines[2].timeMs)
        assertEquals("Chorus line", parsed.lines[2].text)
    }

    @Test
    fun parseEnhancedWordTimestamps_extractsWordsAndCleanText() {
        val raw = "[00:12.00]<00:12.00>Hello <00:12.50>world"
        val parsed = LrcParser.parse(raw)
        assertTrue(parsed.isSynced)
        assertEquals(1, parsed.lines.size)
        assertEquals("Hello world", parsed.lines.first().text)
        assertEquals(2, parsed.lines.first().words.size)
        assertEquals(12_500L, parsed.lines.first().words[1].timeMs)
    }

    @Test
    fun parsePlainLyrics_setsUnsynced() {
        val parsed = LrcParser.parse("Line one\nLine two\nLine three")
        assertFalse(parsed.isSynced)
        assertEquals(3, parsed.lines.size)
        assertEquals(null, parsed.lines[0].timeMs)
    }

    @Test
    fun findActiveLineIndex_binarySearchTracksPositionAccurately() {
        val lines = listOf(
            LrcLine("1", 5_000L, "Line 1"),
            LrcLine("2", 12_500L, "Line 2"),
            LrcLine("3", 20_000L, "Line 3")
        )
        assertEquals(-1, LrcParser.findActiveLineIndex(lines, 1_000L))
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 5_000L))
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 12_499L))
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 12_500L))
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 19_999L))
        assertEquals(2, LrcParser.findActiveLineIndex(lines, 25_000L))
    }

    @Test
    fun shiftAllTimestamps_clampsAtZero() {
        val lines = listOf(
            LrcLine("1", 300L, "Intro"),
            LrcLine("2", 5_000L, "Verse"),
            LrcLine("3", null, "Note")
        )
        val shifted = LrcParser.shiftAllTimestamps(lines, -1_000L)
        assertEquals(0L, shifted[0].timeMs)
        assertEquals(4_000L, shifted[1].timeMs)
        assertEquals(null, shifted[2].timeMs)
    }

    @Test
    fun serialize_formatsTimestampsAndMetadataHeaders() {
        val lines = listOf(
            LrcLine("2", 65_230L, "Second"),
            LrcLine("1", 4_050L, "First")
        )
        val out = LrcParser.serialize(lines, title = "Song", artist = "Band")
        val expected = "[ti:Song]\n[ar:Band]\n[00:04.05]First\n[01:05.23]Second".replace("\n", System.lineSeparator())
        assertEquals(expected, out)
    }
}
