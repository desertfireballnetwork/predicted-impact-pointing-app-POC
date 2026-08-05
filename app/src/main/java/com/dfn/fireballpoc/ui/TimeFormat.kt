package com.dfn.fireballpoc.ui

/** Formats a non-negative duration in milliseconds as HH:MM:SS. */
fun formatHms(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
