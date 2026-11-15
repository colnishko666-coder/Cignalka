package com.example.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _isSupported = MutableStateFlow(accelerometer != null)
    val isSupported: StateFlow<Boolean> = _isSupported.asStateFlow()

    private val _currentGForce = MutableStateFlow(1.0f)
    val currentGForce: StateFlow<Float> = _currentGForce.asStateFlow()

    private var lastShakeTimestamp = 0L
    private var lastPeakTimestamp = 0L
    private var peakCount = 0

    fun start() {
        val sensor = accelerometer ?: return
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        peakCount = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        if (values.size < 3) return

        val x = values[0] / SensorManager.GRAVITY_EARTH
        val y = values[1] / SensorManager.GRAVITY_EARTH
        val z = values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(x * x + y * y + z * z)
        _currentGForce.value = gForce

        val now = System.currentTimeMillis()
        if (gForce > 2.35f) {
            if (now - lastPeakTimestamp > 500L) {
                peakCount = 0
            }
            lastPeakTimestamp = now
            peakCount++
            if (peakCount >= 2 && now - lastShakeTimestamp > 2_500L) {
                lastShakeTimestamp = now
                peakCount = 0
                onShakeDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
