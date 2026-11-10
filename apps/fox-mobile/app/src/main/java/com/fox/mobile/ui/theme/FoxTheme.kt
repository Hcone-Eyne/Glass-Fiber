package com.fox.mobile.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fox.mobile.R

object FoxColors {
    // 10 tokens from Phase 2
    val t1 = Color(0xFFF6F0E6)
    val t2 = Color(0xADF6F0E6) // #F6F0E6 at 68%
    val ember = Color(0xFFF2B33D)
    val alert = Color(0xFFFF4D4D)
    val ok = Color(0xFF7FD6A4)
    val screenBg = Color(0xFF07080C)
    val inkSurface = Color(0x99101016) // rgb(16,16,22) at 60%
    val inkMenu = Color(0xCC18181F) // rgb(24,24,31) at 80%
    val hair = Color(0x24F6F0E6) // #F6F0E6 at 14%
    val lineWarm = Color(0xD9F6F0E6) // #F6F0E6 at 85%

    // Compatibility aliases for existing code
    val BgVoid = screenBg
    val BgCard = inkSurface
    val BgPill = inkSurface
    val InkPrimary = t1
    val InkMuted = t2
    val InkDim = hair
    @get:JvmName("getEmberCompat")
    val Ember = ember
    val EmberWarm = ember
    val AlertRed = alert
    val OkMint = ok
    val CyanLens = t2
}

object FoxFonts {
    val Bricolage = FontFamily(
        Font(R.font.bricolage_grotesque, FontWeight.Normal),
        Font(R.font.bricolage_grotesque, FontWeight.Medium),
        Font(R.font.bricolage_grotesque, FontWeight.SemiBold),
        Font(R.font.bricolage_grotesque, FontWeight.Bold)
    )

    val Figtree = FontFamily(
        Font(R.font.figtree, FontWeight.Normal),
        Font(R.font.figtree, FontWeight.Medium),
        Font(R.font.figtree, FontWeight.SemiBold),
        Font(R.font.figtree, FontWeight.Bold)
    )

    val JetBrainsMono = FontFamily(
        Font(R.font.jetbrains_mono, FontWeight.Normal),
        Font(R.font.jetbrains_mono, FontWeight.Medium),
        Font(R.font.jetbrains_mono, FontWeight.SemiBold),
        Font(R.font.jetbrains_mono, FontWeight.Bold)
    )
}

object FoxType {
    val meta12 = TextStyle(
        fontFamily = FoxFonts.Figtree,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = FoxColors.t2
    )
    val body14 = TextStyle(
        fontFamily = FoxFonts.Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = FoxColors.t1
    )
    val button14 = TextStyle(
        fontFamily = FoxFonts.Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = FoxColors.t1
    )
    val rowTitle16 = TextStyle(
        fontFamily = FoxFonts.Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = FoxColors.t1
    )
    val cardTitle24 = TextStyle(
        fontFamily = FoxFonts.Bricolage,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = (-0.02).em,
        color = FoxColors.t1
    )
    val clock118 = TextStyle(
        fontFamily = FoxFonts.Bricolage,
        fontWeight = FontWeight.Bold,
        fontSize = 118.sp,
        letterSpacing = (-0.055).em,
        lineHeight = (118 * 0.86).sp,
        color = FoxColors.t1
    )
    val homeSentence23 = TextStyle(
        fontFamily = FoxFonts.Bricolage,
        fontWeight = FontWeight.Normal,
        fontSize = 23.sp,
        letterSpacing = (-0.02).em,
        color = FoxColors.t2
    )
    val words16 = TextStyle(
        fontFamily = FoxFonts.Bricolage,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = FoxColors.t1
    )
    val code12 = TextStyle(
        fontFamily = FoxFonts.JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = FoxColors.t2
    )
}

object FoxMotion {
    val EnterEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    const val PressScale = 0.97f
}

@Composable
fun FoxInkBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    isMenu: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val fill = if (isMenu) FoxColors.inkMenu else FoxColors.inkSurface
    val strokeWidth = 1.dp

    Box(
        modifier = modifier
            .clip(shape)
            .background(fill, shape)
            .border(width = strokeWidth, color = FoxColors.hair, shape = shape)
            .padding(contentPadding),
        content = content
    )
}

private val FoxDarkColorScheme = darkColorScheme(
    primary = FoxColors.ember,
    secondary = FoxColors.ok,
    tertiary = FoxColors.t2,
    background = FoxColors.screenBg,
    surface = FoxColors.inkSurface,
    error = FoxColors.alert,
    onPrimary = FoxColors.screenBg,
    onBackground = FoxColors.t1,
    onSurface = FoxColors.t1
)

@Composable
fun FoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FoxDarkColorScheme,
        content = content
    )
}
