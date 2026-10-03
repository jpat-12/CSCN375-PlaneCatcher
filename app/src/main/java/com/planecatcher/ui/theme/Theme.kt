package com.planecatcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.planecatcher.core.model.Tier

// Colours from the app icon. The scheme is dark and high-contrast on purpose:
// interviewees use their phones outdoors, where low-contrast screens are hard to read.
val RadarNavy = Color(0xFF0B1E33)
val RadarSurface = Color(0xFF12304A)
val RadarSurfaceHigh = Color(0xFF1A3D5C)
val RadarGreen = Color(0xFF3DFFA2)
val RadarGreenDim = Color(0xFF2A9E72)
val OnRadar = Color(0xFFF4F8FC)
val OnRadarMuted = Color(0xFFB8C7D6)
val ErrorRed = Color(0xFFFF6B6B)

private val Scheme = darkColorScheme(
    primary = RadarGreen,
    onPrimary = Color(0xFF00210F),
    primaryContainer = RadarGreenDim,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF7FD4FF),
    onSecondary = Color(0xFF002233),
    background = RadarNavy,
    onBackground = OnRadar,
    surface = RadarNavy,
    onSurface = OnRadar,
    surfaceVariant = RadarSurface,
    onSurfaceVariant = OnRadarMuted,
    surfaceContainer = RadarSurface,
    surfaceContainerHigh = RadarSurfaceHigh,
    surfaceContainerHighest = RadarSurfaceHigh,
    outline = Color(0xFF5E7A94),
    error = ErrorRed,
    onError = Color.Black,
)

private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    )
}

/** Sunlight mode: white background, near-black text and deep colours that stay readable outdoors. */
private val SunlightScheme = lightColorScheme(
    primary = Color(0xFF006B3F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F5D3),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF00497A),
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFEDF1F4),
    onSurfaceVariant = Color(0xFF263238),
    surfaceContainer = Color(0xFFF1F4F6),
    surfaceContainerHigh = Color(0xFFE4E9ED),
    surfaceContainerHighest = Color(0xFFDDE3E8),
    outline = Color(0xFF455A64),
    error = Color(0xFFB00020),
    onError = Color.White,
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
            Tier.COMMON -> Color(0xFFCFD8DC)
            Tier.RARE -> Color(0xFF4FC3F7)
            Tier.EPIC -> Color(0xFFD59CFF)
            Tier.LEGENDARY -> Color(0xFFFFD54F)
        }
    }

val MonoStyle = TextStyle(fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
