package com.dfn.fireballpoc.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dfn.fireballpoc.geometry.AzEl
import com.dfn.fireballpoc.geometry.NormalizedPoint
import com.dfn.fireballpoc.geometry.Point2D
import com.dfn.fireballpoc.geometry.PointingMode
import com.dfn.fireballpoc.geometry.arrowAngleDeg
import com.dfn.fireballpoc.geometry.clipPolygonToHalfPlane
import com.dfn.fireballpoc.geometry.projectNormalized
import com.dfn.fireballpoc.geometry.skyValue
import com.dfn.fireballpoc.geometry.worldToDeviceFrame
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

private val SkyColor = Color(0xFF7EC0EE)
private val GroundColor = Color(0xFF5D4632)
private val HorizonColor = Color(0xFFFFFFFF)
private val TrackColor = Color(0xFFFFA000)
private val StartMarkerColor = Color(0xFF1565C0)
private val CurrentMarkerColor = Color(0xFFD32F2F)
private val AlignedColor = Color(0xFF2E7D32)
private val ReticleColor = Color(0xFFFFFFFF)
private val SunColor = Color(0xFFFFC107)
private val MoonColor = Color(0xFFE8E8E0)
private val GridColor = Color(0x33FFFFFF)

private val COMPASS_LABELS = listOf(
    0.0 to "N", 45.0 to "NE", 90.0 to "E", 135.0 to "SE",
    180.0 to "S", 225.0 to "SW", 270.0 to "W", 315.0 to "NW"
)

/**
 * Virtual horizon: projects the horizon line, the whole predicted track, and the observer's
 * current pointing direction onto a simulated view centered on [pointingMode]'s boresight axis
 * (no actual camera preview), using the device's full rotation matrix so azimuth, elevation, AND
 * roll are all correctly accounted for. When the event's start point is outside the current field
 * of view, an edge arrow points toward it so the observer can tell they're facing the wrong way.
 */
@Composable
fun VirtualHorizonHud(
    trackSamples: List<AzEl>,
    currentAzEl: AzEl,
    startAzEl: AzEl,
    devicePointing: AzEl,
    rotationMatrix: FloatArray,
    pointingMode: PointingMode,
    sunAzEl: AzEl? = null,
    moonAzEl: AzEl? = null,
    modifier: Modifier = Modifier,
    fieldOfViewDeg: Double = 70.0,
    alignmentToleranceDeg: Double = 5.0
) {
    val azimuthDelta = angularDelta(currentAzEl.azimuthDeg, devicePointing.azimuthDeg)
    val elevationDelta = currentAzEl.elevationDeg - devicePointing.elevationDeg
    val aligned = abs(azimuthDelta) <= alignmentToleranceDeg && abs(elevationDelta) <= alignmentToleranceDeg
    val currentColor = if (aligned) AlignedColor else CurrentMarkerColor

    val tanHalfFov = tan(Math.toRadians(fieldOfViewDeg / 2.0))

    val startFrame = worldToDeviceFrame(startAzEl.azimuthDeg, startAzEl.elevationDeg, rotationMatrix)
    val startNormalized = startFrame.projectNormalized(tanHalfFov, pointingMode)
    val startOnScreen = startNormalized.inFront && abs(startNormalized.x) <= 1.0 && abs(startNormalized.y) <= 1.0

    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .sizeIn(maxWidth = 440.dp, maxHeight = 440.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val scale = size.minDimension / 2f

                fun pixelOf(np: NormalizedPoint) = Offset(cx + np.x.toFloat() * scale, cy - np.y.toFloat() * scale)
                fun pixelOf(p: Point2D) = Offset(cx + p.x.toFloat() * scale, cy - p.y.toFloat() * scale)
                fun project(azEl: AzEl) = worldToDeviceFrame(azEl.azimuthDeg, azEl.elevationDeg, rotationMatrix).projectNormalized(tanHalfFov, pointingMode)

                clipRect {
                    // Sky/ground fill: the horizon is a great circle, which this projection maps to a
                    // straight line, so a single half-plane clip of the viewport square against it is
                    // exact (not an approximation) and correctly accounts for device roll.
                    val viewportCorners = listOf(
                        Point2D(-1.0, -1.0), Point2D(1.0, -1.0), Point2D(1.0, 1.0), Point2D(-1.0, 1.0)
                    )
                    fun skyValueAt(p: Point2D) = skyValue(p.x, p.y, rotationMatrix, tanHalfFov, pointingMode)
                    fun fillPolygon(points: List<Point2D>, color: Color) {
                        if (points.isEmpty()) return
                        val path = Path().apply {
                            val first = pixelOf(points[0])
                            moveTo(first.x, first.y)
                            for (i in 1 until points.size) {
                                val pt = pixelOf(points[i])
                                lineTo(pt.x, pt.y)
                            }
                            close()
                        }
                        drawPath(path, color = color)
                    }

                    fillPolygon(clipPolygonToHalfPlane(viewportCorners, keepPositive = true, value = ::skyValueAt), SkyColor)
                    fillPolygon(clipPolygonToHalfPlane(viewportCorners, keepPositive = false, value = ::skyValueAt), GroundColor)

                    val horizonPts = (0..360 step 5).map { az -> project(AzEl(az.toDouble(), 0.0)) }
                    for (i in 0 until horizonPts.size - 1) {
                        val a = horizonPts[i]
                        val b = horizonPts[i + 1]
                        if (a.inFront && b.inFront) {
                            drawLine(HorizonColor, pixelOf(a), pixelOf(b), strokeWidth = 4f)
                        }
                    }

                    // Compass ticks every 45 degrees (N/NE/E/SE/S/SW/W/NW) along the horizon.
                    for ((az, label) in COMPASS_LABELS) {
                        val p = project(AzEl(az, 0.0))
                        if (p.inFront && abs(p.x) <= 1.05 && abs(p.y) <= 1.05) {
                            val pixel = pixelOf(p)
                            val isCardinal = label.length == 1
                            drawLine(
                                color = HorizonColor,
                                start = Offset(pixel.x, pixel.y - 8f),
                                end = Offset(pixel.x, pixel.y + 8f),
                                strokeWidth = if (isCardinal) 4f else 2f
                            )
                            val textLayout = textMeasurer.measure(
                                label,
                                style = TextStyle(
                                    color = Color.Black,
                                    fontSize = if (isCardinal) 16.sp else 12.sp,
                                    fontWeight = if (isCardinal) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            drawText(
                                textLayout,
                                topLeft = Offset(pixel.x - textLayout.size.width / 2f, pixel.y + 10f)
                            )
                        }
                    }

                    // Alt-az grid (sky only, 30-degree resolution): faint reference lines, kept
                    // unobtrusive since the horizon/compass ticks already carry the main cues.
                    // Azimuth meridians are great circles so they're safe to sample coarsely;
                    // elevation parallels are small circles (non-linear under this projection) so
                    // they need finer azimuth sampling, matching the horizon's own step of 5.
                    for (az in 0 until 360 step 30) {
                        val meridianPts = (0..90 step 10).map { el -> project(AzEl(az.toDouble(), el.toDouble())) }
                        for (i in 0 until meridianPts.size - 1) {
                            val a = meridianPts[i]
                            val b = meridianPts[i + 1]
                            if (a.inFront && b.inFront) {
                                drawLine(GridColor, pixelOf(a), pixelOf(b), strokeWidth = 1.5f)
                            }
                        }
                    }
                    for (el in listOf(30.0, 60.0)) {
                        val parallelPts = (0..360 step 5).map { az -> project(AzEl(az.toDouble(), el)) }
                        for (i in 0 until parallelPts.size - 1) {
                            val a = parallelPts[i]
                            val b = parallelPts[i + 1]
                            if (a.inFront && b.inFront) {
                                drawLine(GridColor, pixelOf(a), pixelOf(b), strokeWidth = 1.5f)
                            }
                        }
                    }

                    val trackPts = trackSamples.map(::project)
                    var lastSegmentStart: Offset? = null
                    var lastSegmentEnd: Offset? = null
                    for (i in 0 until trackPts.size - 1) {
                        val a = trackPts[i]
                        val b = trackPts[i + 1]
                        if (a.inFront && b.inFront) {
                            val pa = pixelOf(a)
                            val pb = pixelOf(b)
                            drawLine(TrackColor, pa, pb, strokeWidth = 5f)
                            lastSegmentStart = pa
                            lastSegmentEnd = pb
                        }
                    }
                    if (lastSegmentStart != null && lastSegmentEnd != null) {
                        drawTrackArrow(lastSegmentStart, lastSegmentEnd, TrackColor)
                    }

                    val startP = project(startAzEl)
                    if (startP.inFront) {
                        drawCircle(StartMarkerColor, radius = 12f, center = pixelOf(startP))
                    }

                    val currentP = project(currentAzEl)
                    if (currentP.inFront) {
                        drawCircle(currentColor, radius = 16f, center = pixelOf(currentP))
                    }

                    fun drawCelestialMarker(azEl: AzEl, label: String, color: Color) {
                        val p = project(azEl)
                        if (!p.inFront) return
                        val pixel = pixelOf(p)
                        drawCircle(color, radius = 13f, center = pixel)
                        drawCircle(Color.Black, radius = 13f, center = pixel, style = Stroke(width = 1.5f))
                        val textLayout = textMeasurer.measure(
                            label,
                            style = TextStyle(color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                        drawText(textLayout, topLeft = Offset(pixel.x - textLayout.size.width / 2f, pixel.y + 15f))
                    }

                    sunAzEl?.let { drawCelestialMarker(it, "Sun", SunColor) }
                    moonAzEl?.let { drawCelestialMarker(it, "Moon", MoonColor) }

                    drawCircle(ReticleColor, radius = 6f, center = Offset(cx, cy), style = Stroke(width = 2f))
                }
            }

            if (!startOnScreen) {
                EdgeArrow(angleDeg = startFrame.arrowAngleDeg(pointingMode), modifier = Modifier.fillMaxSize())
            }
        }

        Text(azimuthInstruction(azimuthDelta), style = MaterialTheme.typography.bodyLarge)
        Text(elevationInstruction(elevationDelta), style = MaterialTheme.typography.bodyLarge)

        if (aligned) {
            Text("ON TARGET", color = AlignedColor, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun EdgeArrow(angleDeg: Double, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val edgeRadius = size.minDimension / 2f * 0.9f
        val angleRad = Math.toRadians(angleDeg)
        val dx = sin(angleRad).toFloat()
        val dy = -cos(angleRad).toFloat()
        val arrowCenter = Offset(cx + dx * edgeRadius, cy + dy * edgeRadius)

        rotate(degrees = angleDeg.toFloat(), pivot = arrowCenter) {
            val path = Path().apply {
                moveTo(arrowCenter.x, arrowCenter.y - 20f)
                lineTo(arrowCenter.x - 14f, arrowCenter.y + 12f)
                lineTo(arrowCenter.x + 14f, arrowCenter.y + 12f)
                close()
            }
            drawPath(path, color = StartMarkerColor)
        }
    }
}

/** Draws a small arrowhead at [to], oriented along the direction from [from] to [to]. */
private fun DrawScope.drawTrackArrow(from: Offset, to: Offset, color: Color) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val len = sqrt(dx * dx + dy * dy)
    if (len < 1e-3f) return

    val ux = dx / len
    val uy = dy / len
    val perpX = -uy
    val perpY = ux

    val tipX = to.x + ux * 12f
    val tipY = to.y + uy * 12f
    val backX = to.x - ux * 10f
    val backY = to.y - uy * 10f

    val path = Path().apply {
        moveTo(tipX, tipY)
        lineTo(backX + perpX * 9f, backY + perpY * 9f)
        lineTo(backX - perpX * 9f, backY - perpY * 9f)
        close()
    }
    drawPath(path, color = color)
}

private fun azimuthInstruction(azimuthDelta: Double): String {
    val rounded = abs(azimuthDelta).roundToInt()
    if (rounded == 0) return "Azimuth aligned"
    val direction = if (azimuthDelta > 0) "right" else "left"
    return "Turn $direction ${rounded}°"
}

private fun elevationInstruction(elevationDelta: Double): String {
    val rounded = abs(elevationDelta).roundToInt()
    if (rounded == 0) return "Elevation aligned"
    val direction = if (elevationDelta > 0) "up" else "down"
    return "Tilt $direction ${rounded}°"
}

private fun angularDelta(targetDeg: Double, fromDeg: Double): Double {
    var diff = (targetDeg - fromDeg) % 360.0
    if (diff > 180.0) diff -= 360.0
    if (diff <= -180.0) diff += 360.0
    return diff
}
