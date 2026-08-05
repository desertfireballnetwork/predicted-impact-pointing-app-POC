package com.dfn.fireballpoc.geometry

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * WGS84 ellipsoid constants and geodetic <-> ECEF conversion.
 * Ported from RMS/Astrometry/Conversions.py (latLonAlt2ECEF) to keep the
 * validated formula rather than re-deriving it.
 */
object Wgs84 {
    const val EQUATORIAL_RADIUS_M = 6378137.0
    const val POLAR_RADIUS_M = 6356752.314245
    val E2 = 1.0 - (POLAR_RADIUS_M / EQUATORIAL_RADIUS_M) * (POLAR_RADIUS_M / EQUATORIAL_RADIUS_M)
}

/**
 * Converts geodetic coordinates to Earth-Centered Earth-Fixed (ECEF) coordinates.
 *
 * @param latRad latitude in radians (+north)
 * @param lonRad longitude in radians (+east)
 * @param hM elevation in meters (WGS84)
 * @return [x, y, z] ECEF coordinates in meters
 */
fun latLonAltToEcef(latRad: Double, lonRad: Double, hM: Double): DoubleArray {
    val n = Wgs84.EQUATORIAL_RADIUS_M / sqrt(1.0 - Wgs84.E2 * sin(latRad) * sin(latRad))

    val x = (n + hM) * cos(latRad) * cos(lonRad)
    val y = (n + hM) * cos(latRad) * sin(lonRad)
    val z = ((1.0 - Wgs84.E2) * n + hM) * sin(latRad)

    return doubleArrayOf(x, y, z)
}
