package com.example

import com.example.core.model.LrcLine
import com.example.core.model.LrcWord
import com.example.data.lyrics.LyricsSyncEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsSyncEngineTest {

    private val sampleLines = listOf(
        LrcLine(id = "1", timeMs = 5_000L, text = "First line"),
        LrcLine(
            id = "2",
            timeMs = 10_000L,
            text = "Second line here",
            words = listOf(
                LrcWord(10_000L, "Second "),
                LrcWord(12_000L, "line "),
                LrcWord(13_500L, "here")
            )
        ),
        LrcLine(id = "3", timeMs = 15_000L, text = "Final line")
    )

    @Test
    fun computeActiveState_emptyLines_returnsInactive() {
        val snap = LyricsSyncEngine.computeActiveState(emptyList(), 5_000L, 60_000L)
        assertEquals(-1, snap.activeLineIndex)
        assertNull(snap.activeLine)
        assertNull(snap.nextLine)
        assertEquals(0f, snap.lineProgress, 0.001f)
    }

    @Test
    fun computeActiveState_beforeFirstTimestamp_pointsToNextLine() {
        val snap = LyricsSyncEngine.computeActiveState(sampleLines, 2_000L, 60_000L)
        assertEquals(-1, snap.activeLineIndex)
        assertNull(snap.activeLine)
        assertEquals("1", snap.nextLine?.id)
    }

    @Test
    fun computeActiveState_middleLine_calculatesProgressAndActiveWord() {
        val snap = LyricsSyncEngine.computeActiveState(sampleLines, 12_500L, 60_000L)
        assertEquals(1, snap.activeLineIndex)
        assertEquals("2", snap.activeLine?.id)
        assertEquals("3", snap.nextLine?.id)
        assertEquals(0.5f, snap.lineProgress, 0.01f)
        assertEquals(1, snap.activeWordIndex)
    }

    @Test
    fun computeActiveState_lastLine_usesTrackDurationAndClampsProgress() {
        val snap = LyricsSyncEngine.computeActiveState(sampleLines, 20_000L, 25_000L)
        assertEquals(2, snap.activeLineIndex)
        assertEquals("3", snap.activeLine?.id)
        assertNull(snap.nextLine)
        assertEquals(0.5f, snap.lineProgress, 0.01f)
    }
}
