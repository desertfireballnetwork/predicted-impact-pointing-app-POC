package com.dfn.fireballpoc.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopocentricAzElTest {

    @Test
    fun `target directly overhead has elevation near 90 degrees`() {
        val observer = latLonAltToEcef(0.0, 0.0, 0.0)
        val target = latLonAltToEcef(0.0, 0.0, 1000.0)

        val result = ecefToAltAz(observer, target)

        assertEquals(90.0, result.elevationDeg, 1e-6)
    }

    @Test
    fun `target due north on the same meridian has azimuth near 0 degrees`() {
        val observer = latLonAltToEcef(0.0, 0.0, 0.0)
        val target = latLonAltToEcef(Math.toRadians(1.0), 0.0, 0.0)

        val result = ecefToAltAz(observer, target)

        assertEquals(0.0, result.azimuthDeg, 1.0)
    }

    @Test
    fun `target due east along the equator has azimuth near 90 degrees`() {
        val observer = latLonAltToEcef(0.0, 0.0, 0.0)
        val target = latLonAltToEcef(0.0, Math.toRadians(1.0), 0.0)

        val result = ecefToAltAz(observer, target)

        assertEquals(90.0, result.azimuthDeg, 1.0)
    }

    @Test
    fun `isVisible respects the elevation threshold`() {
        assertTrue(isVisible(1.5))
        assertTrue(!isVisible(0.5))
        assertTrue(!isVisible(1.0))
    }

    @Test
    fun `interpolateEcef returns endpoints at fraction 0 and 1, midpoint at 0_5`() {
        val start = doubleArrayOf(0.0, 0.0, 0.0)
        val end = doubleArrayOf(10.0, 20.0, 30.0)

        assertEquals(0.0, interpolateEcef(start, end, 0.0)[0], 1e-9)
        assertEquals(10.0, interpolateEcef(start, end, 1.0)[0], 1e-9)

        val mid = interpolateEcef(start, end, 0.5)
        assertEquals(5.0, mid[0], 1e-9)
        assertEquals(10.0, mid[1], 1e-9)
        assertEquals(15.0, mid[2], 1e-9)
    }

    @Test
    fun `trackAzEl sweeps target azimuth from start point toward end point`() {
        val observer = latLonAltToEcef(0.0, 0.0, 0.0)
        val start = latLonAltToEcef(0.0, Math.toRadians(1.0), 5000.0)
        val end = latLonAltToEcef(Math.toRadians(1.0), 0.0, 5000.0)

        val atStart = trackAzEl(observer, start, end, 0.0)
        val atEnd = trackAzEl(observer, start, end, 1.0)

        assertEquals(90.0, atStart.azimuthDeg, 1.0)
        assertEquals(0.0, atEnd.azimuthDeg, 1.0)
    }
}
