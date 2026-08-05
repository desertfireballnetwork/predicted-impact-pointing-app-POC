package com.dfn.fireballpoc.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.tan

/**
 * Rotation matrix for a phone held vertically, back camera aimed due north, level (elevation 0):
 * device +X (right) = East, device +Y (top edge) = Up, device +Z (toward user's face) = South.
 * Columns are the device axes expressed in the world (East, North, Up) frame; row-major storage.
 */
private val LEVEL_FACING_NORTH = floatArrayOf(
    1f, 0f, 0f,
    0f, 0f, -1f,
    0f, 1f, 0f
)

/**
 * Rotation matrix for a phone lying flat, screen facing straight up, top edge pointing due north:
 * device +X (right) = East, device +Y (top edge) = North, device +Z (out of screen) = Up. This is
 * simply the identity matrix. Used for [PointingMode.LONG_EDGE] tests, analogous to
 * [LEVEL_FACING_NORTH] for the camera-axis tests.
 */
private val EDGE_LEVEL_FACING_NORTH = floatArrayOf(
    1f, 0f, 0f,
    0f, 1f, 0f,
    0f, 0f, 1f
)

class ProjectionTest {

    @Test
    fun `pointing direction itself projects to boresight (zero, zero)`() {
        val frame = worldToDeviceFrame(0.0, 0.0, LEVEL_FACING_NORTH)
        val projected = frame.projectNormalized(tan(Math.toRadians(35.0)), PointingMode.CAMERA_BACK)

        assertTrue(projected.inFront)
        assertEquals(0.0, projected.x, 1e-9)
        assertEquals(0.0, projected.y, 1e-9)
    }

    @Test
    fun `point above boresight projects upward (positive y)`() {
        // 20 degrees above the level-north boresight, same azimuth.
        val frame = worldToDeviceFrame(0.0, 20.0, LEVEL_FACING_NORTH)
        val projected = frame.projectNormalized(tan(Math.toRadians(35.0)), PointingMode.CAMERA_BACK)

        assertTrue(projected.inFront)
        assertEquals(0.0, projected.x, 1e-9)
        assertTrue("expected positive y (up) but was ${projected.y}", projected.y > 0.0)
    }

    @Test
    fun `point east of boresight projects to the right (positive x)`() {
        val frame = worldToDeviceFrame(90.0, 0.0, LEVEL_FACING_NORTH)
        val projected = frame.projectNormalized(tan(Math.toRadians(35.0)), PointingMode.CAMERA_BACK)

        assertTrue(projected.inFront)
        assertTrue("expected positive x (right) but was ${projected.x}", projected.x > 0.0)
        assertEquals(0.0, projected.y, 1e-9)
    }

    @Test
    fun `point outside the field of view is off-screen but still in front`() {
        // 60 degrees above boresight is well outside a 70-degree (35-degree half) field of view.
        val frame = worldToDeviceFrame(0.0, 60.0, LEVEL_FACING_NORTH)
        val projected = frame.projectNormalized(tan(Math.toRadians(35.0)), PointingMode.CAMERA_BACK)

        assertTrue(projected.inFront)
        assertTrue("expected |y| > 1 (off screen) but was ${projected.y}", kotlin.math.abs(projected.y) > 1.0)
    }

    @Test
    fun `point directly behind the device is not in front`() {
        // Due south (180 degrees from a north-facing boresight) is directly behind.
        val frame = worldToDeviceFrame(180.0, 0.0, LEVEL_FACING_NORTH)
        val projected = frame.projectNormalized(tan(Math.toRadians(35.0)), PointingMode.CAMERA_BACK)

        assertTrue(!projected.inFront)
    }

    @Test
    fun `arrow angle points toward a target that is east of boresight`() {
        val frame = worldToDeviceFrame(90.0, 0.0, LEVEL_FACING_NORTH)

        // 90 degrees clockwise from "up" is "right", matching the target being due east.
        assertEquals(90.0, frame.arrowAngleDeg(PointingMode.CAMERA_BACK), 1e-6)
    }

    @Test
    fun `arrow angle points toward a target that is above boresight`() {
        val frame = worldToDeviceFrame(0.0, 45.0, LEVEL_FACING_NORTH)

        assertEquals(0.0, frame.arrowAngleDeg(PointingMode.CAMERA_BACK), 1e-6)
    }

    @Test
    fun `sky value is zero exactly at boresight when level`() {
        val tanHalfFov = tan(Math.toRadians(35.0))
        assertEquals(0.0, skyValue(0.0, 0.0, LEVEL_FACING_NORTH, tanHalfFov, PointingMode.CAMERA_BACK), 1e-9)
    }

    @Test
    fun `sky value is positive above and negative below the horizon`() {
        val tanHalfFov = tan(Math.toRadians(35.0))
        assertTrue(skyValue(0.0, 1.0, LEVEL_FACING_NORTH, tanHalfFov, PointingMode.CAMERA_BACK) > 0.0)
        assertTrue(skyValue(0.0, -1.0, LEVEL_FACING_NORTH, tanHalfFov, PointingMode.CAMERA_BACK) < 0.0)
    }

    @Test
    fun `pointingAzEl for camera mode matches the level-north test matrix`() {
        val azEl = pointingAzEl(LEVEL_FACING_NORTH, PointingMode.CAMERA_BACK)
        assertEquals(0.0, azEl.azimuthDeg, 1e-6)
        assertEquals(0.0, azEl.elevationDeg, 1e-6)
    }

    // --- LONG_EDGE mode: analogous cases to the CAMERA_BACK ones above, using a matrix where the
    // --- top edge (not the back camera) is level and facing north.

    @Test
    fun `edge mode pointing direction projects to boresight (zero, zero)`() {
        val frame = worldToDeviceFrame(0.0, 0.0, EDGE_LEVEL_FACING_NORTH)
        val projected = frame.projectNormalized(tan(Math.toRadians(35.0)), PointingMode.LONG_EDGE)

        assertTrue(projected.inFront)
        assertEquals(0.0, projected.x, 1e-9)
        assertEquals(0.0, projected.y, 1e-9)
    }

    @Test
    fun `edge mode point above boresight projects upward`() {
        val frame = worldToDeviceFrame(0.0, 20.0, EDGE_LEVEL_FACING_NORTH)
        val projected = frame.projectNormalized(tan(Math.toRadians(35.0)), PointingMode.LONG_EDGE)

        assertTrue(projected.inFront)
        assertTrue("expected positive y (up) but was ${projected.y}", projected.y > 0.0)
    }

    @Test
    fun `edge mode point east of boresight projects to the right`() {
        val frame = worldToDeviceFrame(90.0, 0.0, EDGE_LEVEL_FACING_NORTH)
        val projected = frame.projectNormalized(tan(Math.toRadians(35.0)), PointingMode.LONG_EDGE)

        assertTrue(projected.inFront)
        assertTrue("expected positive x (right) but was ${projected.x}", projected.x > 0.0)
    }

    @Test
    fun `edge mode point directly behind is not in front`() {
        val frame = worldToDeviceFrame(180.0, 0.0, EDGE_LEVEL_FACING_NORTH)
        val projected = frame.projectNormalized(tan(Math.toRadians(35.0)), PointingMode.LONG_EDGE)

        assertTrue(!projected.inFront)
    }

    @Test
    fun `edge mode pointingAzEl matches the edge-level-north test matrix`() {
        val azEl = pointingAzEl(EDGE_LEVEL_FACING_NORTH, PointingMode.LONG_EDGE)
        assertEquals(0.0, azEl.azimuthDeg, 1e-6)
        assertEquals(0.0, azEl.elevationDeg, 1e-6)
    }

    @Test
    fun `camera mode on the edge-level matrix points straight down`() {
        // Phone flat, screen up: the back camera faces straight into the table (elevation -90),
        // not north - confirms the mode parameter actually changes which axis is read.
        val azEl = pointingAzEl(EDGE_LEVEL_FACING_NORTH, PointingMode.CAMERA_BACK)
        assertEquals(-90.0, azEl.elevationDeg, 1e-6)
    }

    @Test
    fun `edge mode sky value is positive above and negative below the horizon`() {
        val tanHalfFov = tan(Math.toRadians(35.0))
        assertTrue(skyValue(0.0, 1.0, EDGE_LEVEL_FACING_NORTH, tanHalfFov, PointingMode.LONG_EDGE) > 0.0)
        assertTrue(skyValue(0.0, -1.0, EDGE_LEVEL_FACING_NORTH, tanHalfFov, PointingMode.LONG_EDGE) < 0.0)
    }

    @Test
    fun `clipping a square to the right half plane keeps only x greater than zero`() {
        val square = listOf(Point2D(-1.0, -1.0), Point2D(1.0, -1.0), Point2D(1.0, 1.0), Point2D(-1.0, 1.0))

        val rightHalf = clipPolygonToHalfPlane(square, keepPositive = true) { it.x }

        assertTrue(rightHalf.all { it.x >= -1e-9 })
        assertTrue(rightHalf.any { it.x > 0.9 })
        // Clipped polygon should include the two original right-edge corners plus two new
        // intersection points at x = 0 where the clip line crosses the top and bottom edges.
        assertEquals(4, rightHalf.size)
        assertTrue(rightHalf.count { kotlin.math.abs(it.x) < 1e-9 } == 2)
    }

    @Test
    fun `clipping a square entirely on the keep side returns it unchanged`() {
        val square = listOf(Point2D(1.0, -1.0), Point2D(2.0, -1.0), Point2D(2.0, 1.0), Point2D(1.0, 1.0))

        val result = clipPolygonToHalfPlane(square, keepPositive = true) { it.x }

        assertEquals(4, result.size)
    }

    @Test
    fun `clipping a square entirely on the discard side returns empty`() {
        val square = listOf(Point2D(-2.0, -1.0), Point2D(-1.0, -1.0), Point2D(-1.0, 1.0), Point2D(-2.0, 1.0))

        val result = clipPolygonToHalfPlane(square, keepPositive = true) { it.x }

        assertTrue(result.isEmpty())
    }
}
