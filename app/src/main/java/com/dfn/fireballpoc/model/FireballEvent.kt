package com.dfn.fireballpoc.model

/**
 * A simulated fireball/re-entry event: a track from a start point to an end point, over time.
 * Lat/lon in degrees, heights in meters (WGS84), times as epoch milliseconds.
 */
data class FireballEvent(
    val identifier: String,
    val type: EventType,
    val startLatDeg: Double,
    val startLonDeg: Double,
    val startHeightM: Double,
    val endLatDeg: Double,
    val endLonDeg: Double,
    val endHeightM: Double,
    val startTimeEpochMs: Long,
    val durationSeconds: Double
) {
    val endTimeEpochMs: Long get() = startTimeEpochMs + (durationSeconds * 1000).toLong()
}
