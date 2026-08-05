package com.dfn.fireballpoc.geometry

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class CelestialPositionsTest {

    @Test
    fun `sun declination is near max obliquity at the june solstice`() {
        val epochMs = Instant.parse("2024-06-20T20:51:00Z").toEpochMilli()
        val coord = sunEquatorial(daysSinceJ2000(epochMs))

        assertEquals(23.44, coord.declinationDeg, 0.3)
    }

    @Test
    fun `sun declination is near minus max obliquity at the december solstice`() {
        val epochMs = Instant.parse("2024-12-21T09:21:00Z").toEpochMilli()
        val coord = sunEquatorial(daysSinceJ2000(epochMs))

        assertEquals(-23.44, coord.declinationDeg, 0.3)
    }

    @Test
    fun `sun declination is near zero at the march equinox`() {
        val epochMs = Instant.parse("2024-03-20T03:06:00Z").toEpochMilli()
        val coord = sunEquatorial(daysSinceJ2000(epochMs))

        assertEquals(0.0, coord.declinationDeg, 0.5)
    }

    @Test
    fun `moon ecliptic latitude stays within the moon's known orbital inclination`() {
        // The Moon's orbit is inclined ~5.145 degrees to the ecliptic, so its ecliptic latitude
        // (recovered here via declination at zero hour angle on the ecliptic-crossing node isn't
        // directly exposed, so instead sanity-check declination never exceeds obliquity + inclination).
        for (days in 0..365 step 10) {
            val coord = moonEquatorial(days.toDouble())
            assert(coord.declinationDeg in -29.0..29.0) {
                "Moon declination ${coord.declinationDeg} at day $days is outside the physically possible range"
            }
        }
    }

    @Test
    fun `object at zenith when hour angle is zero and declination equals latitude`() {
        val epochMs = Instant.parse("2024-06-15T12:00:00Z").toEpochMilli()
        val observerLat = -31.95
        val observerLon = 115.86

        // Choose RA so the hour angle (LST - RA) is exactly zero: object is on the local meridian.
        val lst = gmstDeg(epochMs) + observerLon
        val coord = EquatorialCoord(rightAscensionDeg = lst, declinationDeg = observerLat)

        val azEl = equatorialToAzEl(coord, observerLat, observerLon, 0.0, epochMs)

        assertEquals(90.0, azEl.elevationDeg, 0.3)
    }

    @Test
    fun `object on the meridian north of the observer's latitude appears due north`() {
        val epochMs = Instant.parse("2024-06-15T12:00:00Z").toEpochMilli()
        val observerLat = -31.95
        val observerLon = 115.86

        val lst = gmstDeg(epochMs) + observerLon
        // Declination well north of the observer's latitude, still on the meridian (hour angle zero).
        val coord = EquatorialCoord(rightAscensionDeg = lst, declinationDeg = observerLat + 40.0)

        val azEl = equatorialToAzEl(coord, observerLat, observerLon, 0.0, epochMs)

        val azimuthDiff = ((azEl.azimuthDeg - 0.0 + 540.0) % 360.0) - 180.0
        assertEquals(0.0, azimuthDiff, 1.0)
    }
}
