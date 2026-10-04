package com.planecatcher.ui.home

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planecatcher.BuildConfig
import com.planecatcher.R
import com.planecatcher.core.catalog.AircraftCatalog
import com.planecatcher.core.geo.Geo
import com.planecatcher.core.model.Tier
import com.planecatcher.core.radar.NearbyPlane
import com.planecatcher.core.rules.GameRules
import com.planecatcher.ui.common.formatDistance
import com.planecatcher.ui.theme.color
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File

/** The Map tab from the mockup: live map of nearby aircraft, a Nearby Aircraft card, and Location Jump. */
@Composable
fun MapScreen(
    requestedHex: String?,
    onRequestHandled: () -> Unit,
    onStartCatch: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showJumpSheet by rememberJumpSheetState()
    RadarScreenEffects(vm, state, requestedHex, onRequestHandled)
    val inRange = state.radar.planes.filter { it.inRange }
    val tierColors = Tier.entries.associateWith { it.color.toArgb() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 76.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(28.dp)),
            ) {
                PlaneMap(
                    planes = state.radar.planes,
                    center = state.radar.center,
                    isJump = state.radar.jumpTarget != null,
                    tierColors = tierColors,
                    onPlaneTap = vm::onPlaneTapped,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    "© OpenStreetMap contributors",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nearby Aircraft:", style = MaterialTheme.typography.titleLarge)
                    if (inRange.isEmpty()) {
                        RadarStatus(state, 0)
                    } else {
                        inRange.forEach { p ->
                            NearbyRow(
                                plane = p,
                                caught = p.hex in state.caughtHexes,
                                metric = state.settings.useMetric,
                                onClick = { vm.onPlaneTapped(p.hex) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Explore elsewhere", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
        }
        item { JumpCard(state.jump, onOpen = { showJumpSheet = true }, onEnd = vm::endJump, modifier = Modifier.fillMaxWidth()) }
    }

    RadarOverlays(vm, state, showJumpSheet, { showJumpSheet = false }, onStartCatch)
}

@Composable
private fun NearbyRow(plane: NearbyPlane, caught: Boolean, metric: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).background(plane.tier.color, CircleShape))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                AircraftCatalog.lookup(plane.aircraft.typeCode)?.fullName ?: plane.aircraft.typeCode ?: "Unknown aircraft",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${plane.aircraft.displayName} · ${plane.tier.displayName}${if (caught) " · caught" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(formatDistance(plane.distanceMiles, metric), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** osmdroid map: you (or your jump point), the 10-mile catch ring, and a marker per plane. */
@Composable
private fun PlaneMap(
    planes: List<NearbyPlane>,
    center: com.planecatcher.core.model.GeoPoint?,
    isJump: Boolean,
    tierColors: Map<Tier, Int>,
    onPlaneTap: (String) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val map = remember { createMap(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) map.onResume()
            if (e == Lifecycle.Event.ON_PAUSE) map.onPause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        map.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            map.onPause()
            map.onDetach()
        }
    }
    val holder = remember { MapState() }

    AndroidView(
        factory = { map },
        update = { view ->
            view.overlays.clear()
            if (center != null) {
                val c = GeoPoint(center.lat, center.lon)
                if (holder.lastCenter != center) {
                    holder.lastCenter = center
                    view.controller.setZoom(10.0)
                    view.controller.setCenter(c)
                }
                view.overlays.add(
                    Polygon(view).apply {
                        points = Polygon.pointsAsCircle(c, GameRules.CATCH_RANGE_MILES * Geo.KM_PER_MILE * 1000)
                        fillPaint.color = android.graphics.Color.argb(30, 156, 203, 245)
                        outlinePaint.color = android.graphics.Color.argb(200, 30, 110, 200)
                        outlinePaint.strokeWidth = 4f
                        setOnClickListener { _, _, _ -> false }
                    },
                )
                view.overlays.add(
                    Marker(view).apply {
                        position = c
                        icon = dot(context, if (isJump) 0xFFFF8F00.toInt() else 0xFF1C1C1E.toInt())
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        infoWindow = null
                    },
                )
            }
            for (p in planes) {
                view.overlays.add(
                    Marker(view).apply {
                        position = GeoPoint(p.aircraft.lat, p.aircraft.lon)
                        icon = planeIcon(context, tierColors.getValue(p.tier))
                        rotation = -(p.aircraft.headingDeg ?: 0.0).toFloat()
                        isFlat = true
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setOnMarkerClickListener { _, _ ->
                            onPlaneTap(p.hex)
                            true
                        }
                    },
                )
            }
            view.invalidate()
        },
        modifier = modifier,
    )
}

private class MapState {
    var lastCenter: com.planecatcher.core.model.GeoPoint? = null
}

private fun createMap(context: Context): MapView {
    Configuration.getInstance().apply {
        userAgentValue = BuildConfig.APPLICATION_ID
        osmdroidBasePath = File(context.filesDir, "osmdroid")
        osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
    }
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        isTilesScaledToDpi = true
        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
        controller.setZoom(3.0)
    }
}

private fun planeIcon(context: Context, color: Int): Drawable =
    ContextCompat.getDrawable(context, R.drawable.ic_map_plane)!!.mutate().apply { setTint(color) }

private fun dot(context: Context, color: Int): Drawable =
    ContextCompat.getDrawable(context, R.drawable.ic_map_dot)!!.mutate().apply { setTint(color) }
