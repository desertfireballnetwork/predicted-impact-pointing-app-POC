package com.dfn.fireballpoc.geometry

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** A sky direction expressed in the device's own local axes (X=right, Y=top edge, Z=toward user's face). */
data class DeviceFrame(val x: Double, val y: Double, val z: Double)

/** Normalized view coordinates: x/y in [-1, 1] is inside the field of view; [inFront] is false behind the device. */
data class NormalizedPoint(val x: Double, val y: Double, val inFront: Boolean)

/**
 * Transforms a world sky direction (az/el) into the device's local frame, given the device->world
 * rotation matrix from [android.hardware.SensorManager.getRotationMatrixFromVector] (row-major,
 * 9 floats). Since that matrix is orthogonal, the inverse (world -> device) is its transpose:
 * deviceVec = R^T * worldVec.
 */
fun worldToDeviceFrame(azDeg: Double, elDeg: Double, rotationMatrix: FloatArray): DeviceFrame {
    val azRad = Math.toRadians(azDeg)
    val elRad = Math.toRadians(elDeg)
    val east = cos(elRad) * sin(azRad)
    val north = cos(elRad) * cos(azRad)
    val up = sin(elRad)

    val x = rotationMatrix[0] * east + rotationMatrix[3] * north + rotationMatrix[6] * up
    val y = rotationMatrix[1] * east + rotationMatrix[4] * north + rotationMatrix[7] * up
    val z = rotationMatrix[2] * east + rotationMatrix[5] * north + rotationMatrix[8] * up
    return DeviceFrame(x.toDouble(), y.toDouble(), z.toDouble())
}

/**
 * Perspective-projects into normalized view coordinates for a given half field of view (radians).
 * The boresight is the device's -Z axis (back camera direction), so a point is "in front" when z < 0.
 */
fun DeviceFrame.projectNormalized(tanHalfFov: Double): NormalizedPoint {
    val inFront = z < 0.0
    val depth = (-z).coerceAtLeast(1e-4)
    return NormalizedPoint((x / depth) / tanHalfFov, (y / depth) / tanHalfFov, inFront)
}

/**
 * Direction (clockwise degrees from "up") to rotate the phone to bring this point toward the
 * center of view. Unlike [projectNormalized], this stays well-behaved even when the point is
 * directly behind the device (z >= 0), where the perspective divide is meaningless.
 */
fun DeviceFrame.arrowAngleDeg(): Double = Math.toDegrees(atan2(x, y))

/**
 * Sign of the world "up" component of the ray through normalized screen point (nx, ny): positive
 * is sky, negative is ground, zero is exactly on the horizon. Because the astronomical horizon is
 * a great circle, and this projection maps great circles to straight lines (a property of
 * gnomonic/perspective projection), this is an exact *linear* function of (nx, ny) - the horizon
 * line is genuinely straight in this view, not just approximately so.
 */
fun skyValue(nx: Double, ny: Double, rotationMatrix: FloatArray, tanHalfFov: Double): Double {
    return tanHalfFov * (rotationMatrix[6] * nx + rotationMatrix[7] * ny) - rotationMatrix[8]
}

/** A 2D point, used for the generic polygon clip below (kept independent of any UI toolkit's Offset type). */
data class Point2D(val x: Double, val y: Double)

/**
 * Sutherland-Hodgman clip of a convex [polygon] against a single half-plane defined by [value]:
 * keeps the region where value >= 0 (if [keepPositive]) or value <= 0 (otherwise), inserting an
 * interpolated point wherever an edge crosses value == 0. Used to split the HUD viewport into its
 * sky and ground portions along the (straight) horizon line.
 */
fun clipPolygonToHalfPlane(polygon: List<Point2D>, keepPositive: Boolean, value: (Point2D) -> Double): List<Point2D> {
    if (polygon.isEmpty()) return polygon

    val result = mutableListOf<Point2D>()
    for (i in polygon.indices) {
        val curr = polygon[i]
        val next = polygon[(i + 1) % polygon.size]
        val currValue = value(curr)
        val nextValue = value(next)
        val currInside = if (keepPositive) currValue >= 0.0 else currValue <= 0.0
        val nextInside = if (keepPositive) nextValue >= 0.0 else nextValue <= 0.0

        if (currInside) result.add(curr)
        if (currInside != nextInside) {
            val t = currValue / (currValue - nextValue)
            result.add(Point2D(curr.x + t * (next.x - curr.x), curr.y + t * (next.y - curr.y)))
        }
    }
    return result
}
