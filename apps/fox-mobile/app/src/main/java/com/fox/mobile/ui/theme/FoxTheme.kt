package com.fox.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object FoxColors {
    val BgVoid = Color(0xFF0A0B10)
    val BgCard = Color(0xEC0D0F18)
    val BgPill = Color(0xB80E1018)
    val InkPrimary = Color(0xFFF3EFE6)
    val InkMuted = Color(0x9EEDE8DC)
    val InkDim = Color(0x52EDE8DC)
    val Ember = Color(0xFFF2B33D)
    val EmberWarm = Color(0xFFFF9640)
    val AlertRed = Color(0xFFFF4D4D)
    val OkMint = Color(0xFF7FD6A4)
    val CyanLens = Color(0xFF6EC8FF)
}

private val FoxDarkColorScheme = darkColorScheme(
    primary = FoxColors.Ember,
    secondary = FoxColors.OkMint,
    tertiary = FoxColors.CyanLens,
    background = FoxColors.BgVoid,
    surface = FoxColors.BgCard,
    error = FoxColors.AlertRed,
    onPrimary = FoxColors.BgVoid,
    onBackground = FoxColors.InkPrimary,
    onSurface = FoxColors.InkPrimary
)

@Composable
fun FoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FoxDarkColorScheme,
        content = content
    )
}
