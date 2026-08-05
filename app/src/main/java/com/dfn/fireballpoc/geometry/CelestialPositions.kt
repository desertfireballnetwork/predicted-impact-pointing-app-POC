package com.dfn.fireballpoc.geometry

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Right ascension / declination, in degrees, equinox-of-date (no precession correction - fine at this precision). */
data class EquatorialCoord(val rightAscensionDeg: Double, val declinationDeg: Double)

/** Treated as "very far away" so only direction matters; deliberately ignores parallax (negligible for
 *  the Sun, up to ~1 degree for the Moon - acceptable given the HUD's 5-degree alignment tolerance). */
private const val FAR_DISTANCE_M = 1.496e11 // ~1 AU

fun daysSinceJ2000(epochMs: Long): Double = epochMs / 86_400_000.0 - 10_957.5

private fun normalizeDeg(deg: Double): Double = ((deg % 360.0) + 360.0) % 360.0

/**
 * Greenwich Mean Sidereal Time, in degrees. Standard low-precision linear formula (e.g. Meeus /
 * USNO), accurate to a fraction of a second of time over centuries - far more than this app needs.
 */
fun gmstDeg(epochMs: Long): Double {
    val d = daysSinceJ2000(epochMs)
    return normalizeDeg(280.46061837 + 360.98564736629 * d)
}

/**
 * Low-precision solar position (~0.01 degree), the standard NOAA/Michalsky-style formula: mean
 * longitude + equation-of-center correction for ecliptic longitude, then straight to equatorial
 * coordinates assuming zero ecliptic latitude (the Sun is on the ecliptic by definition).
 */
fun sunEquatorial(daysSinceJ2000: Double): EquatorialCoord {
    val d = daysSinceJ2000
    val meanLonDeg = normalizeDeg(280.460 + 0.9856474 * d)
    val meanAnomalyDeg = normalizeDeg(357.528 + 0.9856003 * d)
    val meanAnomalyRad = Math.toRadians(meanAnomalyDeg)

    val eclipticLonDeg = normalizeDeg(
        meanLonDeg + 1.915 * sin(meanAnomalyRad) + 0.020 * sin(2 * meanAnomalyRad)
    )
    val obliquityDeg = 23.439 - 0.0000004 * d

    return equatorialFromEcliptic(eclipticLonDeg, 0.0, obliquityDeg)
}

/**
 * Low-precision lunar position (truncated Meeus-style periodic series - the dominant ~12
 * longitude and ~8 latitude terms), accurate to roughly a few tenths of a degree. The Moon's
 * orbit is perturbed enough that a bare mean-position formula (like the Sun's) would be off by
 * several degrees, so this includes the largest correction terms instead of just the leading one.
 */
fun moonEquatorial(daysSinceJ2000: Double): EquatorialCoord {
    val d = daysSinceJ2000

    val meanLonDeg = normalizeDeg(218.316 + 13.176396 * d)
    val elongationDeg = normalizeDeg(297.850 + 12.190749 * d) // mean elongation from the Sun
    val sunAnomalyDeg = normalizeDeg(357.528 + 0.9856003 * d)
    val moonAnomalyDeg = normalizeDeg(134.963 + 13.064993 * d)
    val argLatDeg = normalizeDeg(93.272 + 13.229350 * d) // argument of latitude

    fun r(deg: Double) = Math.toRadians(deg)

    val lonDeg = normalizeDeg(
        meanLonDeg +
            6.289 * sin(r(moonAnomalyDeg)) -
            1.274 * sin(r(2 * elongationDeg - moonAnomalyDeg)) +
            0.658 * sin(r(2 * elongationDeg)) -
            0.186 * sin(r(sunAnomalyDeg)) -
            0.059 * sin(r(2 * elongationDeg - 2 * moonAnomalyDeg)) -
            0.057 * sin(r(2 * elongationDeg - sunAnomalyDeg - moonAnomalyDeg)) +
            0.053 * sin(r(2 * elongationDeg + moonAnomalyDeg)) +
            0.046 * sin(r(2 * elongationDeg - sunAnomalyDeg)) +
            0.041 * sin(r(moonAnomalyDeg - sunAnomalyDeg)) -
            0.035 * sin(r(elongationDeg)) -
            0.031 * sin(r(moonAnomalyDeg + sunAnomalyDeg)) -
            0.015 * sin(r(2 * argLatDeg - moonAnomalyDeg)) +
            0.011 * sin(r(moonAnomalyDeg - 4 * elongationDeg))
    )

    val latDeg =
        5.128 * sin(r(argLatDeg)) +
            0.281 * sin(r(moonAnomalyDeg + argLatDeg)) +
            0.278 * sin(r(moonAnomalyDeg - argLatDeg)) +
            0.173 * sin(r(2 * elongationDeg - argLatDeg)) +
            0.055 * sin(r(2 * elongationDeg - moonAnomalyDeg + argLatDeg)) +
            0.046 * sin(r(2 * elongationDeg - moonAnomalyDeg - argLatDeg)) +
            0.033 * sin(r(2 * elongationDeg + argLatDeg)) +
            0.017 * sin(r(2 * moonAnomalyDeg + argLatDeg))

    val obliquityDeg = 23.439 - 0.0000004 * d
    return equatorialFromEcliptic(lonDeg, latDeg, obliquityDeg)
}

private fun equatorialFromEcliptic(eclipticLonDeg: Double, eclipticLatDeg: Double, obliquityDeg: Double): EquatorialCoord {
    val lonRad = Math.toRadians(eclipticLonDeg)
    val latRad = Math.toRadians(eclipticLatDeg)
    val oblRad = Math.toRadians(obliquityDeg)

    val raDeg = Math.toDegrees(
        atan2(sin(lonRad) * cos(oblRad) - tan(latRad) * sin(oblRad), cos(lonRad))
    )
    val decDeg = Math.toDegrees(
        asin(sin(latRad) * cos(oblRad) + cos(latRad) * sin(oblRad) * sin(lonRad))
    )
    return EquatorialCoord(normalizeDeg(raDeg), decDeg)
}

/**
 * Az/el of an equatorial-coordinate object as seen from an observer at a given moment. Converts
 * RA/Dec to an Earth-Centered Inertial unit vector, rotates it into ECEF by Greenwich Mean
 * Sidereal Time (the standard equinox-based ECI->ECEF rotation), places it [FAR_DISTANCE_M] away
 * from the observer along that direction, and reuses the already-validated [ecefToAltAz] rather
 * than a separate hour-angle spherical-trig formula (a common source of sign-convention bugs).
 */
fun equatorialToAzEl(
    coord: EquatorialCoord,
    observerLatDeg: Double,
    observerLonDeg: Double,
    observerAltM: Double,
    epochMs: Long
): AzEl {
    val gmstRad = Math.toRadians(gmstDeg(epochMs))

    val raRad = Math.toRadians(coord.rightAscensionDeg)
    val decRad = Math.toRadians(coord.declinationDeg)
    val xEci = cos(decRad) * cos(raRad)
    val yEci = cos(decRad) * sin(raRad)
    val zEci = sin(decRad)

    val xEcef = xEci * cos(gmstRad) + yEci * sin(gmstRad)
    val yEcef = -xEci * sin(gmstRad) + yEci * cos(gmstRad)
    val zEcef = zEci

    val observerEcef = latLonAltToEcef(Math.toRadians(observerLatDeg), Math.toRadians(observerLonDeg), observerAltM)
    val farPoint = doubleArrayOf(
        observerEcef[0] + FAR_DISTANCE_M * xEcef,
        observerEcef[1] + FAR_DISTANCE_M * yEcef,
        observerEcef[2] + FAR_DISTANCE_M * zEcef
    )
    return ecefToAltAz(observerEcef, farPoint)
}

fun sunAzEl(observerLatDeg: Double, observerLonDeg: Double, observerAltM: Double, epochMs: Long): AzEl =
    equatorialToAzEl(sunEquatorial(daysSinceJ2000(epochMs)), observerLatDeg, observerLonDeg, observerAltM, epochMs)

fun moonAzEl(observerLatDeg: Double, observerLonDeg: Double, observerAltM: Double, epochMs: Long): AzEl =
    equatorialToAzEl(moonEquatorial(daysSinceJ2000(epochMs)), observerLatDeg, observerLonDeg, observerAltM, epochMs)
