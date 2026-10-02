package com.planecatcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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

@Composable
fun PlaneCatcherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
}

val Tier.color: Color
    get() = when (this) {
        Tier.COMMON -> Color(0xFFCFD8DC)
        Tier.RARE -> Color(0xFF4FC3F7)
        Tier.EPIC -> Color(0xFFD59CFF)
        Tier.LEGENDARY -> Color(0xFFFFD54F)
    }

val MonoStyle = TextStyle(fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
