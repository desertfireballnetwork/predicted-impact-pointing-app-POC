package com.dfn.fireballpoc.geometry

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_KM = 6371.0

/** Great-circle (haversine) distance between two points on Earth's surface, in kilometers. */
fun greatCircleDistanceKm(lat1Deg: Double, lon1Deg: Double, lat2Deg: Double, lon2Deg: Double): Double {
    val lat1Rad = Math.toRadians(lat1Deg)
    val lat2Rad = Math.toRadians(lat2Deg)
    val dLatRad = Math.toRadians(lat2Deg - lat1Deg)
    val dLonRad = Math.toRadians(lon2Deg - lon1Deg)

    val sinHalfLat = sin(dLatRad / 2.0)
    val sinHalfLon = sin(dLonRad / 2.0)
    val a = sinHalfLat * sinHalfLat + cos(lat1Rad) * cos(lat2Rad) * sinHalfLon * sinHalfLon
    val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    return EARTH_RADIUS_KM * c
}

/** Initial great-circle bearing (degrees, +E of due N) to travel from point 1 toward point 2. */
fun initialBearingDeg(lat1Deg: Double, lon1Deg: Double, lat2Deg: Double, lon2Deg: Double): Double {
    val lat1Rad = Math.toRadians(lat1Deg)
    val lat2Rad = Math.toRadians(lat2Deg)
    val dLonRad = Math.toRadians(lon2Deg - lon1Deg)

    val y = sin(dLonRad) * cos(lat2Rad)
    val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLonRad)
    return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
}

/** Great-circle destination point (spherical-Earth approximation) given a start point, distance, and bearing. */
fun destinationPoint(latDeg: Double, lonDeg: Double, distanceKm: Double, bearingDeg: Double): Pair<Double, Double> {
    val latRad = Math.toRadians(latDeg)
    val lonRad = Math.toRadians(lonDeg)
    val bearingRad = Math.toRadians(bearingDeg)
    val angularDistance = distanceKm / EARTH_RADIUS_KM

    val lat2Rad = asin(sin(latRad) * cos(angularDistance) + cos(latRad) * sin(angularDistance) * cos(bearingRad))
    val lon2Rad = lonRad + atan2(
        sin(bearingRad) * sin(angularDistance) * cos(latRad),
        cos(angularDistance) - sin(latRad) * sin(lat2Rad)
    )

    val lat2Deg = Math.toDegrees(lat2Rad)
    val lon2Deg = ((Math.toDegrees(lon2Rad) + 540.0) % 360.0) - 180.0
    return lat2Deg to lon2Deg
}

private val COMPASS_POINTS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

/** Rounds a bearing to the nearest 8-point compass direction (N, NE, E, ...). */
fun nearestCompassLabel(bearingDeg: Double): String {
    val normalized = ((bearingDeg % 360.0) + 360.0) % 360.0
    val index = Math.round(normalized / 45.0).toInt() % 8
    return COMPASS_POINTS[index]
}
