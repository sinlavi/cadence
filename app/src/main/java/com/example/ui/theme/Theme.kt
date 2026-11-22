package com.example.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.example.core.model.RatingSystemMode
import com.example.core.model.ThemeBaseMode
import com.example.core.model.ThemeConfig

object CadenceSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val mdLg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
}

object CadenceRadius {
    val xs: Dp = 6.dp
    val sm: Dp = 10.dp
    val md: Dp = 14.dp
    val lg: Dp = 18.dp
    val xl: Dp = 24.dp
}

data class ArtworkPaletteColors(
    val dominant: Color,
    val vibrant: Color,
    val muted: Color,
    val onAccent: Color
)

data class CadenceExtendedTheme(
    val config: ThemeConfig = ThemeConfig(),
    val isDark: Boolean = true,
    val isAmoled: Boolean = false,
    val dividerColor: Color = Color(0x14FFFFFF),
    val subtleBorder: Color = Color(0x1EFFFFFF),
    val elevatedSurface: Color = Color(0xFF171B26),
    val tonalSurface: Color = Color(0xFF1E2331),
    val subtleText: Color = Color(0xFF9BA3B4),
    val artworkPalette: ArtworkPaletteColors? = null
)

val LocalCadenceTheme = staticCompositionLocalOf { CadenceExtendedTheme() }
val LocalIsCompactWidth = staticCompositionLocalOf { false }
val LocalRatingSystemMode = staticCompositionLocalOf { RatingSystemMode.HEARTS }

fun contrastOnColor(bg: Color): Color {
    val bgLum = bg.luminance()
    val darkInk = Color(0xFF0E1118)
    val darkLum = darkInk.luminance()
    val whiteContrast = (1.0f + 0.05f) / (bgLum + 0.05f)
    val darkContrast = (maxOf(bgLum, darkLum) + 0.05f) / (minOf(bgLum, darkLum) + 0.05f)
    return if (darkContrast > whiteContrast) darkInk else Color(0xFFFFFFFF)
}

fun extractArtworkPalette(bitmap: Bitmap): ArtworkPaletteColors {
    val palette = Palette.from(bitmap).maximumColorCount(16).generate()
    val dominantInt = palette.getDominantColor(0xFF161A24.toInt())
    val vibrantInt = palette.getVibrantColor(
        palette.getLightVibrantColor(
            palette.getDarkVibrantColor(0xFF4F7CFF.toInt())
        )
    )
    val mutedInt = palette.getDarkMutedColor(
        palette.getMutedColor(0xFF252A36.toInt())
    )
    val vibrant = Color(vibrantInt)
    return ArtworkPaletteColors(
        dominant = Color(dominantInt),
        vibrant = vibrant,
        muted = Color(mutedInt),
        onAccent = contrastOnColor(vibrant)
    )
}

@Composable
fun CadenceMaterial2Theme(
    themeConfig: ThemeConfig,
    artworkPalette: ArtworkPaletteColors? = null,
    ratingSystemMode: RatingSystemMode = RatingSystemMode.HEARTS,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val effectiveDark = when (themeConfig.baseMode) {
        ThemeBaseMode.SYSTEM -> systemDark
        ThemeBaseMode.LIGHT -> false
        ThemeBaseMode.DARK, ThemeBaseMode.AMOLED_BLACK -> true
    }
    val isAmoled = themeConfig.baseMode == ThemeBaseMode.AMOLED_BLACK

    val useArt = themeConfig.useArtworkColors && themeConfig.artworkThemeAppWide && artworkPalette != null
    val primary = if (useArt) artworkPalette!!.vibrant else Color(themeConfig.primaryHex)
    val secondary = if (useArt) artworkPalette!!.dominant else Color(themeConfig.secondaryHex)

    val background = when {
        themeConfig.customBackgroundHex != null -> Color(themeConfig.customBackgroundHex)
        isAmoled -> Color(0xFF000000)
        effectiveDark -> Color(0xFF0B0E14)
        else -> Color(0xFFF6F7FB)
    }

    val surface = when {
        themeConfig.customSurfaceHex != null -> Color(themeConfig.customSurfaceHex)
        isAmoled -> Color(0xFF080A0F)
        effectiveDark -> Color(0xFF11151E)
        else -> Color(0xFFFFFFFF)
    }

    val elevatedSurface = when {
        isAmoled -> Color(0xFF12151E)
        effectiveDark -> Color(0xFF171C28)
        else -> Color(0xFFEEF1F7)
    }

    val tonalSurface = when {
        isAmoled -> Color(0xFF181C27)
        effectiveDark -> Color(0xFF1E2433)
        else -> Color(0xFFE5E9F2)
    }

    val onBackground = if (background.luminance() < 0.4f) Color(0xFFF3F5FA) else Color(0xFF121622)
    val onSurface = if (surface.luminance() < 0.4f) Color(0xFFF3F5FA) else Color(0xFF121622)
    val subtleText = if (onSurface.luminance() > 0.5f) Color(0xFF9BA3B4) else Color(0xFF5D667A)
    val divider = if (onSurface.luminance() > 0.5f) Color(0x17FFFFFF) else Color(0x14121622)
    val subtleBorder = if (onSurface.luminance() > 0.5f) Color(0x24FFFFFF) else Color(0x1E121622)

    val m2Colors: Colors = if (effectiveDark) {
        darkColors(
            primary = primary,
            primaryVariant = primary.copy(alpha = 0.85f),
            secondary = secondary,
            secondaryVariant = secondary.copy(alpha = 0.85f),
            background = background,
            surface = surface,
            error = Color(0xFFF87171),
            onPrimary = contrastOnColor(primary),
            onSecondary = contrastOnColor(secondary),
            onBackground = onBackground,
            onSurface = onSurface,
            onError = Color(0xFF11151E)
        )
    } else {
        lightColors(
            primary = primary,
            primaryVariant = primary.copy(alpha = 0.85f),
            secondary = secondary,
            secondaryVariant = secondary.copy(alpha = 0.85f),
            background = background,
            surface = surface,
            error = Color(0xFFDC2626),
            onPrimary = contrastOnColor(primary),
            onSecondary = contrastOnColor(secondary),
            onBackground = onBackground,
            onSurface = onSurface,
            onError = Color.White
        )
    }

    val corner = themeConfig.artworkCornerRadiusDp.coerceIn(2, 24).coerceAtLeast(8)
    val shapes = Shapes(
        small = RoundedCornerShape(corner.coerceAtMost(12).dp),
        medium = RoundedCornerShape(corner.dp),
        large = RoundedCornerShape((corner + 4).dp)
    )

    val extended = CadenceExtendedTheme(
        config = themeConfig,
        isDark = effectiveDark,
        isAmoled = isAmoled,
        dividerColor = divider,
        subtleBorder = subtleBorder,
        elevatedSurface = elevatedSurface,
        tonalSurface = tonalSurface,
        subtleText = subtleText,
        artworkPalette = artworkPalette
    )

    CompositionLocalProvider(
        LocalCadenceTheme provides extended,
        LocalRatingSystemMode provides ratingSystemMode
    ) {
        MaterialTheme(
            colors = m2Colors,
            typography = buildCadenceTypography(themeConfig.fontScale),
            shapes = shapes,
            content = content
        )
    }
}
