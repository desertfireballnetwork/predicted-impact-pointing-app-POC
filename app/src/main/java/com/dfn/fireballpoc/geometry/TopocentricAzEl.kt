package com.dfn.fireballpoc.geometry

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

/** Azimuth (degrees, +E of due N, [0, 360)) and elevation (degrees, +up) as seen from an observer. */
data class AzEl(val azimuthDeg: Double, val elevationDeg: Double)

/**
 * Given two ECEF points, computes the azimuth/elevation which point from [s] (observer) to [p] (target).
 *
 * Direct port of RMS/Astrometry/Conversions.py ECEF2AltAz, itself sourced from
 * https://gis.stackexchange.com/a/58926 — kept as vector algebra (no explicit ENU rotation
 * matrix) to match the original, already-validated formula.
 */
fun ecefToAltAz(s: DoubleArray, p: DoubleArray): AzEl {
    val (sx, sy, sz) = s
    val (px, py, pz) = p

    val dx = px - sx
    val dy = py - sy
    val dz = pz - sz

    val elevationRad = PI / 2.0 - acos(
        (sx * dx + sy * dy + sz * dz) / sqrt((sx * sx + sy * sy + sz * sz) * (dx * dx + dy * dy + dz * dz))
    )
    val elevationDeg = Math.toDegrees(elevationRad)

    val cosAz = (-sz * sx * dx - sz * sy * dy + (sx * sx + sy * sy) * dz) / sqrt(
        (sx * sx + sy * sy) * (sx * sx + sy * sy + sz * sz) * (dx * dx + dy * dy + dz * dz)
    )
    val sinAz = (-sy * dx + sx * dy) / sqrt((sx * sx + sy * sy) * (dx * dx + dy * dy + dz * dz))

    val azimuthDeg = (Math.toDegrees(atan2(sinAz, cosAz)) + 360.0) % 360.0

    return AzEl(azimuthDeg, elevationDeg)
}
