package com.example

import com.example.core.util.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun normalizeForSearch_foldsArabicAndPersianVariantsAndDigits() {
        val arabicForm = "علي كرمي ١٢٣"
        val persianForm = "علی کرمی ۱۲۳"
        assertEquals(
            TextNormalizer.normalizeForSearch(persianForm),
            TextNormalizer.normalizeForSearch(arabicForm)
        )
        assertEquals("علی کرمی 123", TextNormalizer.normalizeForSearch(arabicForm))
    }

    @Test
    fun normalizeForSearch_stripsLatinAccentsAndZwnj() {
        assertEquals("cafe del mar", TextNormalizer.normalizeForSearch("Café\u200Cdel\u200DMar"))
    }

    @Test
    fun similarity_scoresIdenticalNormalizedStringsAsOne() {
        assertEquals(1f, TextNormalizer.similarity("Beyoncé", "beyonce"), 0.0001f)
        assertTrue(TextNormalizer.similarity("Bohemian Rhapsody", "Bohemian") > 0.4f)
    }

    @Test
    fun sanitizeFilename_replacesUnsafeCharacters() {
        assertEquals("Artist_ Track _1_", TextNormalizer.sanitizeFilename("Artist: Track <1>"))
        assertEquals("Untitled", TextNormalizer.sanitizeFilename("..."))
    }
}
