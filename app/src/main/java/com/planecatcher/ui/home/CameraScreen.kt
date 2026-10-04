package com.planecatcher.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planecatcher.core.catalog.AircraftCatalog
import com.planecatcher.core.catalog.Airlines
import com.planecatcher.core.radar.NearbyPlane
import com.planecatcher.core.rules.JumpStatus
import com.planecatcher.data.PlanePhoto
import com.planecatcher.ui.common.PlanePhoto
import com.planecatcher.ui.common.TierBadge
import com.planecatcher.ui.common.formatAltitude
import com.planecatcher.ui.common.formatDistance
import com.planecatcher.ui.common.formatDuration
import com.planecatcher.ui.common.formatSpeed
import com.planecatcher.ui.theme.InkDark
import kotlin.math.abs
import kotlin.math.roundToInt

/** How far off (degrees) the camera can point and still "identify" a plane. */
private const val AIM_TOLERANCE_DEG = 25.0

/**
 * The Camera tab from the mockup: a live viewfinder with scan brackets. Point the phone
 * at a plane and the compass picks the aircraft in that direction; the white panel
 * underneath identifies it and offers the catch. Without a camera, the radar shows instead.
 */
@Composable
fun CameraScreen(
    requestedHex: String?,
    onRequestHandled: () -> Unit,
    onStartCatch: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    var showJumpSheet by rememberJumpSheetState()
    RadarScreenEffects(vm, state, requestedHex, onRequestHandled)

    val context = LocalContext.current
    var cameraAllowed by remember { mutableStateOf(hasCameraPermission(context)) }
    var cameraFailed by remember { mutableStateOf(false) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraAllowed = it }
    val useCamera = cameraAllowed && hasCamera(context) && !cameraFailed

    val heading by rememberCameraHeading()
    val inRange = state.radar.planes.filter { it.inRange }

    // Manual pick (arrows) wins; otherwise the plane the camera is aimed at; otherwise the nearest.
    var manualHex by rememberSaveable { mutableStateOf<String?>(null) }
    val aimed: NearbyPlane? = heading?.let { h ->
        inRange.minByOrNull { abs(bearingDelta(h, it.bearingDeg)) }
            ?.takeIf { abs(bearingDelta(h, it.bearingDeg)) <= AIM_TOLERANCE_DEG }
    }
    val selected = inRange.firstOrNull { it.hex == manualHex } ?: aimed ?: inRange.firstOrNull()
    val isAimed = selected != null && selected == aimed && manualHex == null

    // Photo for the identified plane.
    var photo by remember { mutableStateOf<PlanePhoto?>(null) }
    LaunchedEffect(selected?.hex) {
        photo = null
        selected?.let { photo = vm.photoFor(it.hex) }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val viewHeight = maxHeight * 0.55f
        // Viewfinder (or radar fallback).
        Box(Modifier.fillMaxWidth().height(viewHeight)) {
            if (useCamera) {
                CameraPreview(Modifier.fillMaxSize(), onError = { cameraFailed = true })
            } else {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color(0xFF5B8FC9), Color(0xFFA9CBEB), Color(0xFFDDEBF7))),
                    ),
                )
                RadarView(
                    planes = state.radar.planes,
                    caughtHexes = state.caughtHexes,
                    isJump = state.radar.jumpTarget != null,
                    onPlaneTap = { manualHex = it },
                    modifier = Modifier.align(Alignment.Center).padding(top = 72.dp).width(viewHeight * 0.7f),
                )
            }
            if (useCamera) ScanBrackets(Modifier.align(Alignment.Center).padding(top = 48.dp).size(viewHeight * 0.5f), locked = isAimed)

            // Status chips under the pill switcher.
            Row(
                Modifier.align(Alignment.TopCenter).padding(top = 76.dp, start = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                progress?.let { p ->
                    OverlayChip("Lv ${p.level.level}")
                    OverlayChip("${p.streakDays}", icon = { Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(0xFFFF8A3D), modifier = Modifier.size(18.dp)) })
                    OverlayChip("${p.today.count { it.complete }}/${p.today.size} today")
                }
                val jump = state.jump
                if (jump is JumpStatus.Active) {
                    OverlayChip("Jump · ${formatDuration(jump.remainingMs)}", icon = { Icon(Icons.Filled.TravelExplore, null, modifier = Modifier.size(18.dp)) })
                }
            }

            if (!cameraAllowed && hasCamera(context)) {
                Button(
                    onClick = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = InkDark),
                ) {
                    Icon(Icons.Filled.PhotoCamera, null)
                    Text("  Turn on camera view")
                }
            }
        }

        // White "Identified Aircraft" panel, overlapping the viewfinder like the mockup.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(maxHeight - viewHeight + 28.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IdentifiedPanel(
                state = state,
                selected = selected,
                isAimed = isAimed,
                inRange = inRange,
                photo = photo,
                headingAvailable = heading != null,
                onPrev = { manualHex = cycle(inRange, selected, -1) },
                onNext = { manualHex = cycle(inRange, selected, +1) },
                onCatch = { selected?.let { onStartCatch(it.hex) } },
                onJump = { showJumpSheet = true },
                onEndJump = vm::endJump,
            )
        }
    }

    RadarOverlays(vm, state, showJumpSheet, { showJumpSheet = false }, onStartCatch)
}

private fun cycle(list: List<NearbyPlane>, current: NearbyPlane?, step: Int): String? {
    if (list.isEmpty()) return null
    val i = list.indexOfFirst { it.hex == current?.hex }.coerceAtLeast(0)
    return list[(i + step + list.size) % list.size].hex
}

@Composable
private fun IdentifiedPanel(
    state: HomeUiState,
    selected: NearbyPlane?,
    isAimed: Boolean,
    inRange: List<NearbyPlane>,
    photo: PlanePhoto?,
    headingAvailable: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCatch: () -> Unit,
    onJump: () -> Unit,
    onEndJump: () -> Unit,
) {
    val ink = InkDark
    val muted = Color(0xFF5F6368)
    if (selected == null) {
        Text("No aircraft identified", style = MaterialTheme.typography.headlineSmall, color = ink)
        RadarStatus(state, inRange.size, textColor = muted)
        JumpCard(state.jump, onOpen = onJump, onEnd = onEndJump, modifier = Modifier.fillMaxWidth())
        return
    }
    val a = selected.aircraft
    val metric = state.settings.useMetric
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            when {
                isAimed -> "Identified Aircraft:"
                headingAvailable -> "Nearest Aircraft (point your camera to aim):"
                else -> "Nearest Aircraft:"
            },
            style = MaterialTheme.typography.labelLarge,
            color = muted,
            modifier = Modifier.weight(1f),
        )
        if (inRange.size > 1) {
            IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous aircraft", tint = ink) }
            Text("${inRange.indexOf(selected) + 1}/${inRange.size}", color = ink)
            IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next aircraft", tint = ink) }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                AircraftCatalog.lookup(a.typeCode)?.fullName ?: a.typeCode ?: "Unknown aircraft",
                style = MaterialTheme.typography.headlineMedium,
                color = ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TierBadge(selected.tier, Modifier.padding(top = 4.dp))
        }
        PlanePhoto(photo?.url, selected.tier, Modifier.padding(start = 12.dp).size(width = 110.dp, height = 76.dp))
    }
    Text("Details:", style = MaterialTheme.typography.titleMedium, color = ink)
    DetailRow("Callsign", a.callsign ?: "—")
    Airlines.fromCallsign(a.callsign)?.let { DetailRow("Airline", it) }
    a.registration?.let { DetailRow("Registration", it) }
    DetailRow("Distance", formatDistance(selected.distanceMiles, metric))
    DetailRow("Altitude", formatAltitude(a.altitudeFt, metric))
    DetailRow("Speed", formatSpeed(a.speedKt, metric))
    DetailRow("Direction", compass(selected.bearingDeg))
    photo?.photographer?.let { Text("Photo © $it · planespotters.net", style = MaterialTheme.typography.labelSmall, color = muted) }

    val caught = selected.hex in state.caughtHexes
    val lockedUntil = state.lockedUntil(selected.hex)
    Button(
        onClick = onCatch,
        enabled = !caught && lockedUntil == null && !(state.radar.mockLocation && state.radar.jumpTarget == null),
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ink, contentColor = Color.White),
    ) {
        Text(
            when {
                caught -> "Already in your collection"
                lockedUntil != null -> "Locked · ${formatDuration(lockedUntil - state.nowMs)}"
                else -> "Catch it!"
            },
        )
    }
    JumpCard(state.jump, onOpen = onJump, onEnd = onEndJump, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = Color(0xFF5F6368), modifier = Modifier.weight(1f))
        Text(value, color = InkDark, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun compass(bearing: Double): String {
    val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return "${dirs[((bearing / 45.0).roundToInt()) % 8]} (${bearing.roundToInt()}°)"
}

@Composable
private fun OverlayChip(text: String, icon: (@Composable () -> Unit)? = null) {
    AssistChip(
        onClick = {},
        label = { Text(text) },
        leadingIcon = icon,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = Color.Black.copy(alpha = 0.45f),
            labelColor = Color.White,
        ),
        border = null,
    )
}

/** The four white rounded corner brackets from the mockup. Turn green when locked onto a plane. */
@Composable
private fun ScanBrackets(modifier: Modifier, locked: Boolean) {
    val color = if (locked) Color(0xFF3DFFA2) else Color.White
    Canvas(modifier) {
        val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
        val len = size.minDimension * 0.28f
        val r = 22.dp.toPx()
        val w = size.width
        val h = size.height
        // Each corner: a quarter arc plus two straight legs.
        fun corner(x: Float, y: Float, sx: Float, sy: Float, start: Float) {
            drawArc(color, start, 90f, false, Offset(if (sx > 0) x else x - 2 * r, if (sy > 0) y else y - 2 * r), Size(2 * r, 2 * r), style = stroke)
            drawLine(color, Offset(x + sx * r, y), Offset(x + sx * len, y), stroke.width, StrokeCap.Round)
            drawLine(color, Offset(x, y + sy * r), Offset(x, y + sy * len), stroke.width, StrokeCap.Round)
        }
        corner(0f, 0f, 1f, 1f, 180f)
        corner(w, 0f, -1f, 1f, 270f)
        corner(0f, h, 1f, -1f, 90f)
        corner(w, h, -1f, -1f, 0f)
    }
}
