package com.dfn.fireballpoc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.dfn.fireballpoc.model.ObserverState
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

private const val DEFAULT_ZOOM = 11.0

/**
 * Lets the user pan a real OpenStreetMap view to pick a location to simulate being at, using a
 * fixed center pin (the pin stays screen-centered; the map pans underneath it) rather than a
 * tap-to-place marker - simpler to implement and arguably more precise to use.
 */
@Composable
fun MapPickerScreen(
    initialLocation: ObserverState?,
    onConfirm: (ObserverState) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val startLat = initialLocation?.latDeg ?: -31.95
    val startLon = initialLocation?.lonDeg ?: 115.86

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(GeoPoint(startLat, startLon))
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onDetach() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Pan the map so the pin marks where you want to simulate being",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            // Fixed center pin; nudged up so the glyph's point (not its visual center) lines up
            // with the true center of the view, since that's the coordinate actually captured.
            Text("📍", fontSize = 40.sp, modifier = Modifier.align(Alignment.Center).padding(bottom = 32.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val center = mapView.mapCenter
                    onConfirm(ObserverState(latDeg = center.latitude, lonDeg = center.longitude, altitudeM = 0.0))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Use this location")
            }
        }
    }
}
