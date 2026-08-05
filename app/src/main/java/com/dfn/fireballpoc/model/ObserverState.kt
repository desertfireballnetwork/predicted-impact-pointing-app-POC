package com.dfn.fireballpoc.model

/** Observer's current position, in degrees/meters (WGS84), as reported by GPS. */
data class ObserverState(
    val latDeg: Double,
    val lonDeg: Double,
    val altitudeM: Double
)
