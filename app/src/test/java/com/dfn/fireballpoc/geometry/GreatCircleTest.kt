package com.dfn.fireballpoc.geometry

import org.junit.Assert.assertEquals
import org.junit.Test

class GreatCircleTest {

    @Test
    fun `distance from a point to itself is zero`() {
        assertEquals(0.0, greatCircleDistanceKm(-31.95, 115.86, -31.95, 115.86), 1e-9)
    }

    @Test
    fun `distance between two equatorial points 90 degrees apart is a quarter of Earth's circumference`() {
        val distance = greatCircleDistanceKm(0.0, 0.0, 0.0, 90.0)
        assertEquals(10007.5, distance, 1.0)
    }

    @Test
    fun `bearing due east along the equator is 90 degrees`() {
        assertEquals(90.0, initialBearingDeg(0.0, 0.0, 0.0, 10.0), 1e-6)
    }

    @Test
    fun `bearing due north is 0 degrees`() {
        assertEquals(0.0, initialBearingDeg(0.0, 0.0, 10.0, 0.0), 1e-6)
    }

    @Test
    fun `destination point round trips through distance and bearing`() {
        val startLat = -31.95
        val startLon = 115.86
        val distanceKm = 342.0
        val bearingDeg = 217.0

        val (destLat, destLon) = destinationPoint(startLat, startLon, distanceKm, bearingDeg)

        assertEquals(distanceKm, greatCircleDistanceKm(startLat, startLon, destLat, destLon), 0.1)
        assertEquals(bearingDeg, initialBearingDeg(startLat, startLon, destLat, destLon), 0.1)
    }

    @Test
    fun `nearest compass label rounds to the closest 8-point direction`() {
        assertEquals("N", nearestCompassLabel(0.0))
        assertEquals("N", nearestCompassLabel(359.0))
        assertEquals("NE", nearestCompassLabel(44.0))
        assertEquals("E", nearestCompassLabel(90.0))
        assertEquals("S", nearestCompassLabel(180.0))
        assertEquals("NW", nearestCompassLabel(325.0))
    }
}
