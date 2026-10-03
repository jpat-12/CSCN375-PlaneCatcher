package com.planecatcher.ui.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.planecatcher.BuildConfig
import com.planecatcher.core.model.GeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import org.osmdroid.util.GeoPoint as OsmPoint

/**
 * OpenStreetMap world map. Tap anywhere to drop a pin. Uses osmdroid, so it needs
 * no Google services or API key (works on Windows Subsystem for Android too).
 */
@Composable
fun MapPicker(selected: GeoPoint?, onPick: (GeoPoint) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val map = remember { createMap(context) }
    val marker = remember { Marker(map).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) } }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        map.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            map.onPause()
            map.onDetach()
        }
    }

    AndroidView(
        factory = {
            map.overlays.add(
                MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: OsmPoint): Boolean {
                        onPick(GeoPoint(p.latitude.coerceIn(-90.0, 90.0), wrapLon(p.longitude)))
                        return true
                    }

                    override fun longPressHelper(p: OsmPoint): Boolean = false
                }),
            )
            map
        },
        update = { view ->
            view.overlays.remove(marker)
            if (selected != null) {
                marker.position = OsmPoint(selected.lat, selected.lon)
                view.overlays.add(marker)
            }
            view.invalidate()
        },
        modifier = modifier,
    )
}

private fun wrapLon(lon: Double): Double = ((lon + 180.0) % 360.0 + 360.0) % 360.0 - 180.0

private fun createMap(context: Context): MapView {
    Configuration.getInstance().apply {
        userAgentValue = BuildConfig.APPLICATION_ID
        // Keep tiles in app-private storage; no storage permission needed.
        osmdroidBasePath = File(context.filesDir, "osmdroid")
        osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
    }
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        isTilesScaledToDpi = true
        minZoomLevel = 2.0
        controller.setZoom(2.5)
        controller.setCenter(OsmPoint(30.0, 0.0))
    }
}
