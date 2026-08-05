package com.dfn.fireballpoc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dfn.fireballpoc.geometry.PointingMode
import com.dfn.fireballpoc.geometry.greatCircleDistanceKm
import com.dfn.fireballpoc.geometry.initialBearingDeg
import com.dfn.fireballpoc.geometry.isVisible
import com.dfn.fireballpoc.geometry.latLonAltToEcef
import com.dfn.fireballpoc.geometry.moonAzEl
import com.dfn.fireballpoc.geometry.nearestCompassLabel
import com.dfn.fireballpoc.geometry.pointingAzEl
import com.dfn.fireballpoc.geometry.sunAzEl
import com.dfn.fireballpoc.geometry.trackAzEl
import com.dfn.fireballpoc.model.EventType
import com.dfn.fireballpoc.model.FireballEvent
import com.dfn.fireballpoc.model.ObserverState
import com.dfn.fireballpoc.sensors.OrientationTracker
import kotlinx.coroutines.delay

private const val TRACK_SAMPLE_COUNT = 30

@Composable
fun HudScreen(
    event: FireballEvent,
    observerState: ObserverState?,
    locationPermissionGranted: Boolean,
    onBackToForm: () -> Unit
) {
    val context = LocalContext.current
    val orientationTracker = remember { OrientationTracker(context) }
    val rotationMatrix by orientationTracker.rotationMatrix.collectAsState()
    var pointingMode by remember { mutableStateOf(PointingMode.CAMERA_BACK) }
    val devicePointing = pointingAzEl(rotationMatrix, pointingMode)

    DisposableEffect(Unit) {
        orientationTracker.start()
        onDispose { orientationTracker.stop() }
    }

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(200)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(event.identifier, style = MaterialTheme.typography.titleMedium)
        Text(typeLabel(event.type), style = MaterialTheme.typography.bodyMedium)
        Text(statusLabel(nowMs, event), style = MaterialTheme.typography.labelLarge)
        Text(
            statusValue(nowMs, event),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PointingModeButton("Camera", PointingMode.CAMERA_BACK, pointingMode) { pointingMode = it }
            PointingModeButton("Long edge", PointingMode.LONG_EDGE, pointingMode) { pointingMode = it }
        }
        Text(
            aimInstructionText(pointingMode),
            style = MaterialTheme.typography.titleMedium
        )

        when {
            !locationPermissionGranted -> {
                Text("Location permission was not granted; can't compute visibility.")
            }

            observerState == null -> {
                Text("Waiting for GPS fix...")
            }

            else -> {
                val betterViewMessage = remember(event, observerState) { betterViewSuggestion(observerState, event) }
                betterViewMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                }

                val observerEcef = latLonAltToEcef(
                    Math.toRadians(observerState.latDeg),
                    Math.toRadians(observerState.lonDeg),
                    observerState.altitudeM
                )
                val startEcef = latLonAltToEcef(
                    Math.toRadians(event.startLatDeg),
                    Math.toRadians(event.startLonDeg),
                    event.startHeightM
                )
                val endEcef = latLonAltToEcef(
                    Math.toRadians(event.endLatDeg),
                    Math.toRadians(event.endLonDeg),
                    event.endHeightM
                )

                val fraction = ((nowMs - event.startTimeEpochMs).toDouble() /
                    (event.endTimeEpochMs - event.startTimeEpochMs).toDouble())
                    .coerceIn(0.0, 1.0)

                val target = trackAzEl(observerEcef, startEcef, endEcef, fraction)

                if (isVisible(target.elevationDeg)) {
                    val trackSamples = remember(event) {
                        (0..TRACK_SAMPLE_COUNT).map { i ->
                            trackAzEl(observerEcef, startEcef, endEcef, i.toDouble() / TRACK_SAMPLE_COUNT)
                        }
                    }
                    val startAzEl = trackSamples.first()

                    // Sun/Moon positions are computed once, at the event's start time ("time of
                    // impact"), not live - they barely move over the span of one simulated event.
                    val sunPosition = remember(event, observerState) {
                        sunAzEl(observerState.latDeg, observerState.lonDeg, observerState.altitudeM, event.startTimeEpochMs)
                            .takeIf { isVisible(it.elevationDeg) }
                    }
                    val moonPosition = remember(event, observerState) {
                        moonAzEl(observerState.latDeg, observerState.lonDeg, observerState.altitudeM, event.startTimeEpochMs)
                            .takeIf { isVisible(it.elevationDeg) }
                    }

                    VirtualHorizonHud(
                        trackSamples = trackSamples,
                        currentAzEl = target,
                        startAzEl = startAzEl,
                        devicePointing = devicePointing,
                        rotationMatrix = rotationMatrix,
                        pointingMode = pointingMode,
                        sunAzEl = sunPosition,
                        moonAzEl = moonPosition,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    Text("NOT VISIBLE from here (below horizon)", style = MaterialTheme.typography.titleMedium)
                    Text("Target azimuth: %.1f°, elevation: %.1f°".format(target.azimuthDeg, target.elevationDeg))
                }
            }
        }

        Button(onClick = onBackToForm, modifier = Modifier.padding(top = 24.dp)) {
            Text("Back to home")
        }
    }
}

@Composable
private fun PointingModeButton(label: String, mode: PointingMode, current: PointingMode, onSelect: (PointingMode) -> Unit) {
    if (mode == current) {
        Button(onClick = { onSelect(mode) }) { Text(label) }
    } else {
        OutlinedButton(onClick = { onSelect(mode) }) { Text(label) }
    }
}

private fun aimInstructionText(mode: PointingMode): String = when (mode) {
    PointingMode.CAMERA_BACK -> "Aim your phone like you're taking a photo of the target"
    PointingMode.LONG_EDGE -> "Point the long edge of your phone at the target (e.g. taped to another instrument)"
}

private const val BETTER_VIEW_DISTANCE_KM = 200.0

/** Suggests a rough compass direction to walk/drive when the event is far enough away that
 *  moving toward it would meaningfully help (mainly matters for the "not visible" case, but is
 *  shown whenever relevant since a closer position also means a higher, easier-to-spot elevation). */
private fun betterViewSuggestion(observerState: ObserverState, event: FireballEvent): String? {
    val distanceKm = greatCircleDistanceKm(
        observerState.latDeg, observerState.lonDeg,
        event.startLatDeg, event.startLonDeg
    )
    if (distanceKm <= BETTER_VIEW_DISTANCE_KM) return null

    val bearingDeg = initialBearingDeg(
        observerState.latDeg, observerState.lonDeg,
        event.startLatDeg, event.startLonDeg
    )
    return "You'd get a better view if you moved %s from here (event is ~%.0f km away)"
        .format(nearestCompassLabel(bearingDeg), distanceKm)
}

private fun typeLabel(type: EventType): String = if (type == EventType.FIREBALL) "Fireball" else "Re-entry"

private fun statusLabel(nowMs: Long, event: FireballEvent): String {
    val toStartMs = event.startTimeEpochMs - nowMs
    return when {
        toStartMs > 0 -> "Time until event"
        nowMs < event.endTimeEpochMs -> "IN PROGRESS"
        else -> "Event ended"
    }
}

private fun statusValue(nowMs: Long, event: FireballEvent): String {
    val toStartMs = event.startTimeEpochMs - nowMs
    return when {
        toStartMs > 0 -> formatHms(toStartMs)
        nowMs < event.endTimeEpochMs -> "T+${formatHms(nowMs - event.startTimeEpochMs)}"
        else -> "${formatHms(nowMs - event.endTimeEpochMs)} ago"
    }
}
