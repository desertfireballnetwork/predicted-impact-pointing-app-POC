package com.dfn.fireballpoc.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.dfn.fireballpoc.geometry.AzEl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.asin
import kotlin.math.atan2

/**
 * [pointing]: azimuth/elevation the back camera is aimed at (device -Z axis), see [OrientationTracker].
 * [rotationMatrix]: the full device->world (East-North-Up) rotation matrix (row-major, 9 floats)
 * for the same sensor sample, needed by [com.dfn.fireballpoc.ui.VirtualHorizonHud] to project
 * arbitrary sky points (and the horizon itself) into screen space, correctly accounting for roll.
 */
data class DeviceOrientation(val pointing: AzEl, val rotationMatrix: FloatArray)

/**
 * Tracks which direction the phone's back camera is physically pointing (i.e. the center of its
 * field of view, even though no camera preview is shown), as azimuth/elevation in the same
 * convention as the target track geometry (see [com.dfn.fireballpoc.geometry.AzEl]) - plus the
 * full rotation matrix for projecting other sky points onto the HUD.
 *
 * Rather than SensorManager.getOrientation()'s pitch/roll (whose sign conventions depend on
 * exactly how the phone is held), this reads the rotation matrix directly: the device's local
 * -Z axis (out of the back of the phone, where the camera points) transformed into the world
 * East-North-Up frame gives an unambiguous pointing az/el for any phone orientation. The HUD is
 * built around "aim your phone like you're about to take a photo of the target."
 */
class OrientationTracker(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _orientation = MutableStateFlow(DeviceOrientation(AzEl(0.0, 0.0), FloatArray(9)))
    val orientation: StateFlow<DeviceOrientation> = _orientation.asStateFlow()

    private val rotationMatrix = FloatArray(9)

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            // World-frame (East, North, Up) components of the device's local -Z axis (back camera).
            val east = -rotationMatrix[2]
            val north = -rotationMatrix[5]
            val up = (-rotationMatrix[8]).coerceIn(-1f, 1f)

            val azimuthDeg = (Math.toDegrees(atan2(east.toDouble(), north.toDouble())) + 360.0) % 360.0
            val elevationDeg = Math.toDegrees(asin(up.toDouble()))

            // Defensive copy: SensorManager reuses `rotationMatrix` for every event, so any
            // emitted array must be cloned or every past emission would silently mutate too.
            _orientation.value = DeviceOrientation(AzEl(azimuthDeg, elevationDeg), rotationMatrix.copyOf())
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
    }
}
