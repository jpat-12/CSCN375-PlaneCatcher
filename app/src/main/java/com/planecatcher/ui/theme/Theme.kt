package com.planecatcher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.planecatcher.R
import com.planecatcher.core.model.Tier

// Look from the presentation mockup: charcoal background, lighter rounded tiles,
// a white pill switcher with a light-blue selection, and the rounded Nunito font.
// Contrast stays high on purpose: interviewees use their phones outdoors.
val Charcoal = Color(0xFF2B2B2B)
val Tile = Color(0xFF3D3D3D)
val TileHigh = Color(0xFF4A4A4A)
val PillBlue = Color(0xFFCFE3F7)
val SkyBlue = Color(0xFF9CCBF5)
val RadarGreen = Color(0xFF3DFFA2)
val InkDark = Color(0xFF1C1C1E)

val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

private val Scheme = darkColorScheme(
    primary = SkyBlue,
    onPrimary = Color(0xFF0D2B45),
    primaryContainer = PillBlue,
    onPrimaryContainer = InkDark,
    secondary = RadarGreen,
    onSecondary = Color(0xFF00210F),
    background = Charcoal,
    onBackground = Color.White,
    surface = Charcoal,
    onSurface = Color.White,
    surfaceVariant = Tile,
    onSurfaceVariant = Color(0xFFC9C9CE),
    surfaceContainer = Tile,
    surfaceContainerHigh = TileHigh,
    surfaceContainerHighest = Color(0xFF555555),
    outline = Color(0xFF8A8A90),
    error = Color(0xFFFF6B6B),
    onError = Color.Black,
)

/** Sunlight mode: white background, near-black text and deep colours that stay readable outdoors. */
private val SunlightScheme = lightColorScheme(
    primary = Color(0xFF0B5CAD),
    onPrimary = Color.White,
    primaryContainer = PillBlue,
    onPrimaryContainer = InkDark,
    secondary = Color(0xFF006B3F),
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFEDEFF2),
    onSurfaceVariant = Color(0xFF2E3338),
    surfaceContainer = Color(0xFFEEF0F3),
    surfaceContainerHigh = Color(0xFFE2E5E9),
    surfaceContainerHighest = Color(0xFFD8DCE1),
    outline = Color(0xFF4A5560),
    error = Color(0xFFB00020),
    onError = Color.White,
)

private val AppTypography: Typography = Typography().let { t ->
    fun TextStyle.n(weight: FontWeight? = null) = copy(fontFamily = Nunito, fontWeight = weight ?: fontWeight)
    Typography(
        displayLarge = t.displayLarge.n(FontWeight.ExtraBold),
        displayMedium = t.displayMedium.n(FontWeight.ExtraBold),
        displaySmall = t.displaySmall.n(FontWeight.ExtraBold),
        headlineLarge = t.headlineLarge.n(FontWeight.Bold),
        headlineMedium = t.headlineMedium.n(FontWeight.Bold),
        headlineSmall = t.headlineSmall.n(FontWeight.Bold),
        titleLarge = t.titleLarge.n(FontWeight.Bold),
        titleMedium = t.titleMedium.n(FontWeight.Bold),
        titleSmall = t.titleSmall.n(FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.n(),
        bodyMedium = t.bodyMedium.n(),
        bodySmall = t.bodySmall.n(),
        labelLarge = t.labelLarge.n(FontWeight.Bold).copy(fontSize = 15.sp),
        labelMedium = t.labelMedium.n(FontWeight.SemiBold),
        labelSmall = t.labelSmall.n(FontWeight.SemiBold),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val LocalSunlight = staticCompositionLocalOf { false }

@Composable
fun PlaneCatcherTheme(
    sunlight: Boolean = false,
    largeText: Boolean = false,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scaled = if (largeText) Density(density.density, density.fontScale * 1.25f) else density
    CompositionLocalProvider(LocalSunlight provides sunlight, LocalDensity provides scaled) {
        MaterialTheme(
            colorScheme = if (sunlight) SunlightScheme else Scheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Tier colour, darker in sunlight mode so it contrasts with the white background. */
val Tier.color: Color
    @Composable get() = if (LocalSunlight.current) {
        when (this) {
            Tier.COMMON -> Color(0xFF546E7A)
            Tier.RARE -> Color(0xFF0277BD)
            Tier.EPIC -> Color(0xFF7B1FA2)
            Tier.LEGENDARY -> Color(0xFFC77800)
        }
    } else {
        when (this) {
            Tier.COMMON -> Color(0xFFE0E0E0)
            Tier.RARE -> Color(0xFF4FC3F7)
            Tier.EPIC -> Color(0xFFD59CFF)
            Tier.LEGENDARY -> Color(0xFFFFD54F)
        }
    }
