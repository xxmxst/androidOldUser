package com.example.phoneclass.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF202E3F)
val Blue = Color(0xFF235BBD)
val Paper = Color(0xFFF5F7FB)
val Muted = Color(0xFF536174)
val Green = Color(0xFF226147)
val Warm = Color(0xFFFFF1D9)

@Composable
fun PhoneClassTheme(fontLevel: Int = 0, content: @Composable () -> Unit) {
    val factor = listOf(1f, 1.18f, 1.36f)[fontLevel.coerceIn(0, 2)]
    fun style(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = weight, fontSize = (size * factor).sp, lineHeight = (size * factor * 1.5f).sp)
    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, onPrimary = Color.White, primaryContainer = Color(0xFFE8F0FF), onPrimaryContainer = Ink,
        secondary = Green, secondaryContainer = Color(0xFFE8F0FF), onSecondaryContainer = Blue, background = Paper, onBackground = Ink, surface = Color.White, onSurface = Ink, onSurfaceVariant = Muted,
        surfaceVariant = Color(0xFFEDF1F6), outline = Color(0xFFB6C1D1)), typography = Typography(
        headlineLarge = style(32, FontWeight.Bold), headlineMedium = style(28, FontWeight.Bold), headlineSmall = style(26, FontWeight.Bold),
        titleLarge = style(24, FontWeight.Bold), titleMedium = style(22, FontWeight.SemiBold), titleSmall = style(20, FontWeight.SemiBold),
        bodyLarge = style(22), bodyMedium = style(20), bodySmall = style(18), labelLarge = style(22, FontWeight.SemiBold), labelMedium = style(18, FontWeight.Medium), labelSmall = style(16)
    ), content = content)
}
