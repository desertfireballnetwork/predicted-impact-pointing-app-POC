package com.dfn.fireballpoc.model

import com.dfn.fireballpoc.geometry.destinationPoint
import com.dfn.fireballpoc.geometry.latLonAltToEcef
import java.time.LocalDate
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.random.Random

/**
 * Generates plausible fake [FireballEvent]s for the auto-alert feed, centered near the observer
 * (rather than fully globally random) so events are sometimes actually visible: the horizon
 * distance for a 100km-altitude point is only ~1100km, so a fully random placement anywhere on
 * Earth would almost never be visible, defeating the point of a live test feed.
 */
object EventGenerator {
    /** Time from generation to the event's simulated start. */
    const val LEAD_TIME_MS = 90_000L

    /** How long before the event start to fire the "look up soon" notification. */
    const val NOTIFY_LEAD_MS = 60_000L

    /** How often a new event is generated, regardless of whether the previous one has finished. */
    const val CYCLE_MS = 120_000L

    /**
     * Max distance from the observer to the event's start point. Kept well inside the
     * geometric horizon distance for a 100km-altitude point (~1129km) so generated events have
     * a real chance of clearing the 1-degree elevation threshold instead of almost always
     * being below the horizon.
     */
    private const val MAX_START_OFFSET_KM = 500.0

    /** Entry angle floor: a near-zero slope produces a degenerate/huge ground track (tan -> 0). */
    private const val MIN_REENTRY_ANGLE_DEG = 0.5

    private data class Params(
        val startHeightKm: Double,
        val endHeightKm: Double,
        val entryAngleRangeDeg: ClosedFloatingPointRange<Double>,
        val speedRangeKmS: ClosedFloatingPointRange<Double>
    )

    // Natural fireball: steep-ish entry, ablates roughly 100km down to 20km, typical meteor speed.
    private val FIREBALL_PARAMS = Params(
        startHeightKm = 100.0,
        endHeightKm = 20.0,
        entryAngleRangeDeg = 5.0..85.0,
        speedRangeKmS = 15.0..30.0
    )

    // Artificial re-entry: shallow/grazing entry, visible higher up, roughly orbital decay speed.
    private val REENTRY_PARAMS = Params(
        startHeightKm = 100.0,
        endHeightKm = 50.0,
        entryAngleRangeDeg = MIN_REENTRY_ANGLE_DEG..5.0,
        speedRangeKmS = 7.0..8.0
    )

    fun generate(observer: ObserverState?, type: EventType, random: Random = Random.Default): FireballEvent {
        val params = if (type == EventType.FIREBALL) FIREBALL_PARAMS else REENTRY_PARAMS

        // Fall back to the form's default anchor if no GPS fix is available yet.
        val anchorLat = observer?.latDeg ?: -29.0
        val anchorLon = observer?.lonDeg ?: 122.0

        val (startLat, startLon) = destinationPoint(
            anchorLat, anchorLon,
            distanceKm = random.nextDouble(0.0, MAX_START_OFFSET_KM),
            bearingDeg = random.nextDouble(0.0, 360.0)
        )

        val entryAngleDeg = random.nextDouble(params.entryAngleRangeDeg.start, params.entryAngleRangeDeg.endInclusive)
        val heightDropKm = params.startHeightKm - params.endHeightKm
        val groundDistanceKm = heightDropKm / tan(Math.toRadians(entryAngleDeg))

        val (endLat, endLon) = destinationPoint(
            startLat, startLon,
            distanceKm = groundDistanceKm,
            bearingDeg = random.nextDouble(0.0, 360.0)
        )

        val startEcef = latLonAltToEcef(Math.toRadians(startLat), Math.toRadians(startLon), params.startHeightKm * 1000.0)
        val endEcef = latLonAltToEcef(Math.toRadians(endLat), Math.toRadians(endLon), params.endHeightKm * 1000.0)
        val slantDistanceKm = ecefDistanceKm(startEcef, endEcef)

        val speedKmS = random.nextDouble(params.speedRangeKmS.start, params.speedRangeKmS.endInclusive)
        val durationSeconds = slantDistanceKm / speedKmS

        return FireballEvent(
            identifier = generateIdentifier(type, random),
            type = type,
            startLatDeg = startLat,
            startLonDeg = startLon,
            startHeightM = params.startHeightKm * 1000.0,
            endLatDeg = endLat,
            endLonDeg = endLon,
            endHeightM = params.endHeightKm * 1000.0,
            startTimeEpochMs = System.currentTimeMillis() + LEAD_TIME_MS,
            durationSeconds = durationSeconds
        )
    }

    /**
     * A plausible-looking but entirely made-up designation, distinct in style per type: a
     * date-coded network designation for fireballs, a COSPAR-style international designator for
     * re-entries. Public so [com.dfn.fireballpoc.ui.FormScreen]'s manual entry path can reuse it.
     */
    fun generateIdentifier(type: EventType, random: Random = Random.Default): String {
        return if (type == EventType.FIREBALL) {
            val today = LocalDate.now()
            val datePart = "%04d%02d%02d".format(today.year, today.monthValue, today.dayOfMonth)
            val suffix = ('A'..'Z').random(random)
            "FB$datePart$suffix"
        } else {
            val year = LocalDate.now().year
            val launchNumber = random.nextInt(1, 999)
            val piece = ('A'..'Z').random(random)
            "%d-%03d%s".format(year, launchNumber, piece)
        }
    }

    private fun ecefDistanceKm(a: DoubleArray, b: DoubleArray): Double {
        val dx = a[0] - b[0]
        val dy = a[1] - b[1]
        val dz = a[2] - b[2]
        return sqrt(dx * dx + dy * dy + dz * dz) / 1000.0
    }
}
