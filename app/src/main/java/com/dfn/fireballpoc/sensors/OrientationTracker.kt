package com.dfn.fireballpoc.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks the phone's full device->world rotation matrix (row-major, 9 floats), the raw output of
 * the rotation-vector sensor. Deliberately exposes only this - not a derived "pointing" az/el -
 * since which physical axis counts as "pointing" is a UI-level choice (see
 * [com.dfn.fireballpoc.geometry.PointingMode]), not something the sensor layer should bake in.
 * Consumers call [com.dfn.fireballpoc.geometry.pointingAzEl] with the matrix and their chosen mode.
 */
class OrientationTracker(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _rotationMatrix = MutableStateFlow(FloatArray(9))
    val rotationMatrix: StateFlow<FloatArray> = _rotationMatrix.asStateFlow()

    private val rawMatrix = FloatArray(9)

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            SensorManager.getRotationMatrixFromVector(rawMatrix, event.values)
            // Defensive copy: SensorManager reuses `rawMatrix` for every event, so any emitted
            // array must be cloned or every past emission would silently mutate too.
            _rotationMatrix.value = rawMatrix.copyOf()
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
