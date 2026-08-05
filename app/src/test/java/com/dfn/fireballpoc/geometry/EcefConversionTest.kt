package com.dfn.fireballpoc.geometry

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class EcefConversionTest {

    @Test
    fun `equator prime meridian maps to equatorial radius on x axis`() {
        val ecef = latLonAltToEcef(0.0, 0.0, 0.0)
        assertEquals(Wgs84.EQUATORIAL_RADIUS_M, ecef[0], 1e-6)
        assertEquals(0.0, ecef[1], 1e-6)
        assertEquals(0.0, ecef[2], 1e-6)
    }

    @Test
    fun `north pole maps to polar radius on z axis`() {
        val ecef = latLonAltToEcef(Math.toRadians(90.0), 0.0, 0.0)
        assertEquals(0.0, ecef[0], 1e-3)
        assertEquals(0.0, ecef[1], 1e-3)
        assertEquals(Wgs84.POLAR_RADIUS_M, ecef[2], 1e-3)
    }

    @Test
    fun `altitude adds straight up at the equator`() {
        val ecef = latLonAltToEcef(0.0, 0.0, 1000.0)
        assertEquals(Wgs84.EQUATORIAL_RADIUS_M + 1000.0, ecef[0], 1e-6)
    }

    /**
     * Kotlin port of Conversions.py AER2ECEF, used only as a test oracle to round-trip
     * az/el/range through both directions and catch sign/axis errors in ecefToAltAz.
     */
    private fun aerToEcef(azimDeg: Double, elevDeg: Double, r: Double, latDeg: Double, lonDeg: Double, altM: Double): DoubleArray {
        val obs = latLonAltToEcef(Math.toRadians(latDeg), Math.toRadians(lonDeg), altM)

        val slat = sin(Math.toRadians(latDeg))
        val slon = sin(Math.toRadians(lonDeg))
        val clat = cos(Math.toRadians(latDeg))
        val clon = cos(Math.toRadians(lonDeg))

        val azimRad = Math.toRadians(azimDeg)
        val elevRad = Math.toRadians(elevDeg)

        val south = -r * cos(elevRad) * cos(azimRad)
        val east = r * cos(elevRad) * sin(azimRad)
        val zenith = r * sin(elevRad)

        val x = obs[0] + (slat * clon * south) + (-slon * east) + (clat * clon * zenith)
        val y = obs[1] + (slat * slon * south) + (clon * east) + (clat * slon * zenith)
        val z = obs[2] + (-clat * south) + (slat * zenith)

        return doubleArrayOf(x, y, z)
    }

    /**
     * Tolerance here is looser than the other geometry tests because AER2ECEF's local frame is
     * built from the true ellipsoid-normal (geodetic) "up" direction, while ECEF2AltAz's zenith
     * is the radial direction from Earth's center through the observer (geocentric). These two
     * "up" directions diverge by up to ~0.2 degrees at mid-latitudes on WGS84 - a known property
     * of the original Conversions.py functions, not a porting error. Fine for this app's ~5 degree
     * HUD alignment tolerance.
     */
    @Test
    fun `az el range round trips through aer to ecef and back via ecefToAltAz`() {
        val latDeg = -31.95
        val lonDeg = 115.86
        val altM = 20.0
        val observerEcef = latLonAltToEcef(Math.toRadians(latDeg), Math.toRadians(lonDeg), altM)

        val cases = listOf(
            Triple(45.0, 30.0, 50000.0),
            Triple(180.0, 60.0, 100000.0),
            Triple(270.0, 10.0, 20000.0),
            Triple(0.0, 89.0, 5000.0)
        )

        for ((azimDeg, elevDeg, range) in cases) {
            val targetEcef = aerToEcef(azimDeg, elevDeg, range, latDeg, lonDeg, altM)
            val result = ecefToAltAz(observerEcef, targetEcef)

            val azimuthDiff = ((result.azimuthDeg - azimDeg + 540.0) % 360.0) - 180.0
            assertEquals("azimuth for case $azimDeg/$elevDeg", 0.0, azimuthDiff, 0.3)
            assertEquals("elevation for case $azimDeg/$elevDeg", elevDeg, result.elevationDeg, 0.3)
        }
    }
}
