package com.dfn.fireballpoc.geometry

/** Whether a target at [elevationDeg] is above the observer's horizon by more than [thresholdDeg]. */
fun isVisible(elevationDeg: Double, thresholdDeg: Double = 1.0): Boolean = elevationDeg > thresholdDeg

/** Linearly interpolates between two ECEF points by [fraction] in [0, 1]. */
fun interpolateEcef(startEcef: DoubleArray, endEcef: DoubleArray, fraction: Double): DoubleArray {
    val f = fraction.coerceIn(0.0, 1.0)
    return DoubleArray(3) { i -> startEcef[i] + (endEcef[i] - startEcef[i]) * f }
}

/**
 * Az/el of the simulated track as seen by [observerEcef], at a given [fraction] of elapsed time
 * between the event's start and end points (0 = start point, 1 = end point). This sweeps the
 * target across the sky over time without modelling full trajectory physics.
 */
fun trackAzEl(observerEcef: DoubleArray, startEcef: DoubleArray, endEcef: DoubleArray, fraction: Double): AzEl {
    val target = interpolateEcef(startEcef, endEcef, fraction)
    return ecefToAltAz(observerEcef, target)
}
