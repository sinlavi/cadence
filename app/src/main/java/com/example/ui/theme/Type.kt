package com.example.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val InterFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

fun buildCadenceTypography(scale: Float = 1.0f): Typography {
    val s = scale.coerceIn(0.85f, 1.25f)
    return Typography(
        defaultFontFamily = InterFamily,
        h4 = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (30 * s).sp,
            lineHeight = (36 * s).sp,
            letterSpacing = (-0.5).sp
        ),
        h5 = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (22 * s).sp,
            lineHeight = (28 * s).sp,
            letterSpacing = (-0.3).sp
        ),
        h6 = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18 * s).sp,
            lineHeight = (24 * s).sp,
            letterSpacing = (-0.15).sp
        ),
        subtitle1 = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15.5f * s).sp,
            lineHeight = (21 * s).sp,
            letterSpacing = 0.sp
        ),
        subtitle2 = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13.5f * s).sp,
            lineHeight = (19 * s).sp,
            letterSpacing = 0.05.sp
        ),
        body1 = TextStyle(
            fontFamily = InterFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15 * s).sp,
            lineHeight = (22 * s).sp,
            letterSpacing = 0.1.sp
        ),
        body2 = TextStyle(
            fontFamily = InterFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13.5f * s).sp,
            lineHeight = (19 * s).sp,
            letterSpacing = 0.1.sp
        ),
        button = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13 * s).sp,
            lineHeight = (16 * s).sp,
            letterSpacing = 0.35.sp
        ),
        caption = TextStyle(
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * s).sp,
            lineHeight = (16 * s).sp,
            letterSpacing = 0.2.sp
        ),
        overline = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (10.5f * s).sp,
            lineHeight = (14 * s).sp,
            letterSpacing = 1.1.sp
        )
    )
}

fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")
