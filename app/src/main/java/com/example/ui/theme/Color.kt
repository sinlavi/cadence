package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.core.model.ThemeBaseMode

data class CuratedThemePreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val baseMode: ThemeBaseMode,
    val primaryHex: Long,
    val secondaryHex: Long,
    val backgroundHex: Long? = null,
    val surfaceHex: Long? = null
)

val CuratedThemePresets = listOf(
    CuratedThemePreset(
        id = "material_indigo",
        name = "Studio Sapphire",
        subtitle = "Obsidian & luminous sapphire accent",
        baseMode = ThemeBaseMode.SYSTEM,
        primaryHex = 0xFF4F7CFF,
        secondaryHex = 0xFF14B8A6
    ),
    CuratedThemePreset(
        id = "amber_vinyl",
        name = "Amber Vinyl",
        subtitle = "Warm analog mastering dark",
        baseMode = ThemeBaseMode.DARK,
        primaryHex = 0xFFF59E0B,
        secondaryHex = 0xFF38BDF8
    ),
    CuratedThemePreset(
        id = "amoled_neon",
        name = "AMOLED Cyan",
        subtitle = "True #000000 OLED contrast",
        baseMode = ThemeBaseMode.AMOLED_BLACK,
        primaryHex = 0xFF00E5FF,
        secondaryHex = 0xFFFF4081
    ),
    CuratedThemePreset(
        id = "studio_indigo",
        name = "Nocturne Violet",
        subtitle = "Deep slate & rose coral",
        baseMode = ThemeBaseMode.DARK,
        primaryHex = 0xFF818CF8,
        secondaryHex = 0xFFF43F5E
    ),
    CuratedThemePreset(
        id = "jade_hi_fi",
        name = "Jade Hi-Fi",
        subtitle = "Balanced emerald & champagne",
        baseMode = ThemeBaseMode.DARK,
        primaryHex = 0xFF10B981,
        secondaryHex = 0xFFFBBF24
    ),
    CuratedThemePreset(
        id = "paper_light",
        name = "Porcelain Editorial",
        subtitle = "Crisp daylight studio look",
        baseMode = ThemeBaseMode.LIGHT,
        primaryHex = 0xFFD97706,
        secondaryHex = 0xFF0284C7
    ),
    CuratedThemePreset(
        id = "crimson_tape",
        name = "Crimson Tape",
        subtitle = "Analog red & warm gold",
        baseMode = ThemeBaseMode.DARK,
        primaryHex = 0xFFEF4444,
        secondaryHex = 0xFFF59E0B
    )
)

val ThemeSwatchColors = listOf(
    Color(0xFF4F7CFF), // Sapphire
    Color(0xFFF59E0B), // Amber
    Color(0xFFEF4444), // Crimson
    Color(0xFFEC4899), // Rose
    Color(0xFF8B5CF6), // Violet
    Color(0xFF6366F1), // Indigo
    Color(0xFF06B6D4), // Cyan
    Color(0xFF10B981), // Emerald
    Color(0xFF84CC16), // Lime
    Color(0xFFF97316), // Tangerine
    Color(0xFF14B8A6), // Teal
    Color(0xFFE11D48)  // Ruby
)
