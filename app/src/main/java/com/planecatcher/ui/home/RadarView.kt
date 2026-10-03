package com.planecatcher.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.planecatcher.core.radar.NearbyPlane
import com.planecatcher.core.rules.GameRules
import androidx.compose.material3.MaterialTheme
import com.planecatcher.core.model.Tier
import com.planecatcher.ui.theme.color
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Radar-style view of the 10-mile catch ring. Up is north. Blips are coloured by
 * tier; caught planes are hollow rings. Tap a blip to open its pop-up.
 */
@Composable
fun RadarView(
    planes: List<NearbyPlane>,
    caughtHexes: Set<String>,
    isJump: Boolean,
    onPlaneTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sweep by rememberInfiniteTransition(label = "sweep").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Restart),
        label = "sweepAngle",
    )
    val accent = MaterialTheme.colorScheme.primary
    val ringColor = accent.copy(alpha = 0.45f)
    val centerColor = if (isJump) Color(0xFFFF8F00) else MaterialTheme.colorScheme.onBackground
    // Tier colours depend on the theme, so read them here rather than inside the draw block.
    val tierColors = Tier.entries.associateWith { it.color }

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { contentDescription = "Radar showing ${planes.size} planes within 10 miles" }
            .pointerInput(planes) {
                detectTapGestures { tap ->
                    val radius = min(size.width, size.height) / 2f * 0.95f
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val hit = planes.minByOrNull { p ->
                        val pos = blipPosition(p, c, radius)
                        hypot(pos.x - tap.x, pos.y - tap.y)
                    }?.takeIf { p ->
                        val pos = blipPosition(p, c, radius)
                        hypot(pos.x - tap.x, pos.y - tap.y) < 28.dp.toPx()
                    }
                    hit?.let { onPlaneTap(it.hex) }
                }
            },
    ) {
        val radius = min(size.width, size.height) / 2f * 0.95f
        val c = center
        val stroke = Stroke(width = 1.5.dp.toPx())

        for (i in 1..4) drawCircle(ringColor, radius * i / 4f, c, style = stroke)
        drawLine(ringColor, Offset(c.x - radius, c.y), Offset(c.x + radius, c.y), 1.dp.toPx())
        drawLine(ringColor, Offset(c.x, c.y - radius), Offset(c.x, c.y + radius), 1.dp.toPx())

        rotate(sweep - 90f, c) {
            drawCircle(
                brush = Brush.sweepGradient(
                    0f to Color.Transparent,
                    0.85f to Color.Transparent,
                    1f to accent.copy(alpha = 0.35f),
                    center = c,
                ),
                radius = radius,
                center = c,
            )
            drawLine(accent, c, Offset(c.x + radius, c.y), 2.dp.toPx())
        }

        for (p in planes) {
            val pos = blipPosition(p, c, radius)
            val caught = p.hex in caughtHexes
            val tc = tierColors.getValue(p.tier)
            if (caught) {
                drawCircle(tc, 6.dp.toPx(), pos, style = Stroke(2.dp.toPx()))
            } else {
                drawCircle(tc.copy(alpha = 0.3f), 12.dp.toPx(), pos)
                drawCircle(tc, 6.dp.toPx(), pos)
            }
        }

        drawCircle(centerColor, 5.dp.toPx(), c)
    }
}

private fun blipPosition(p: NearbyPlane, c: Offset, radius: Float): Offset {
    val r = (p.distanceMiles / GameRules.CATCH_RANGE_MILES).coerceIn(0.0, 1.0) * radius
    val theta = Math.toRadians(p.bearingDeg)
    return Offset((c.x + r * sin(theta)).toFloat(), (c.y - r * cos(theta)).toFloat())
}
