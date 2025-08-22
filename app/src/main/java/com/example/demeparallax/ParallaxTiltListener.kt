package com.example.demeparallax

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.View
import kotlin.math.*

class ParallaxTiltListener(
    private val view: View,
    private val maxOffsetX: Float = 60f,   // giảm một chút để mượt hơn
    private val maxOffsetY: Float = 60f,
    private val sensitivity: Float = 25f    // độ nhạy (độ)
) : SensorEventListener {

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    // Để làm mượt chuyển động
    private var smoothPitch = 0f
    private var smoothRoll = 0f
    private val smoothingFactor = 0.15f  // tăng một chút để responsive hơn

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            // Chuyển rotation vector thành rotation matrix
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            // Lấy orientation từ rotation matrix
            SensorManager.getOrientation(rotationMatrix, orientation)

            // orientation[0] = azimuth (xoay quanh trục Z) - không dùng
            // orientation[1] = pitch (ngửa/cúi - xoay quanh trục X)
            // orientation[2] = roll (nghiêng trái/phải - xoay quanh trục Y)

            val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

            // Áp dụng low-pass filter để làm mượt
            smoothPitch = smoothPitch + smoothingFactor * (pitch - smoothPitch)
            smoothRoll = smoothRoll + smoothingFactor * (roll - smoothRoll)

            // Chuẩn hóa về [-1, 1] dựa trên sensitivity
            val normalizedX = (smoothRoll / sensitivity).coerceIn(-1f, 1f)
            val normalizedY = (smoothPitch / sensitivity).coerceIn(-1f, 1f)

            // Tính toán translation
            // Đảo ngược X để có cảm giác tự nhiên (nghiêng phải thì ảnh di chuyển trái)
            val translationX = -normalizedX * maxOffsetX
            val translationY = normalizedY * maxOffsetY

            // Áp dụng animation mượt
            view.animate()
                .translationX(translationX)
                .translationY(translationY)
                .setDuration(50) // animation ngắn để responsive
                .start()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Có thể log accuracy để debug
        when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> println("Sensor accuracy: HIGH")
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> println("Sensor accuracy: MEDIUM")
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> println("Sensor accuracy: LOW")
            SensorManager.SENSOR_STATUS_UNRELIABLE -> println("Sensor accuracy: UNRELIABLE")
        }
    }
}

// Fallback listener cho thiết bị không có rotation vector sensor
class AccelerometerParallaxListener(
    private val view: View,
    private val maxOffsetX: Float = 60f,
    private val maxOffsetY: Float = 60f,
    private val sensitivity: Float = 8f  // nhạy hơn với accelerometer
) : SensorEventListener {

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var smoothPitch = 0f
    private var smoothRoll = 0f
    private val smoothingFactor = 0.2f

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            }
        }

        // Tính orientation từ accelerometer + magnetometer
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

            // Smoothing
            smoothPitch = smoothPitch + smoothingFactor * (pitch - smoothPitch)
            smoothRoll = smoothRoll + smoothingFactor * (roll - smoothRoll)

            // Normalize và apply
            val normalizedX = (smoothRoll / sensitivity).coerceIn(-1f, 1f)
            val normalizedY = (smoothPitch / sensitivity).coerceIn(-1f, 1f)

            val translationX = -normalizedX * maxOffsetX
            val translationY = normalizedY * maxOffsetY

            view.animate()
                .translationX(translationX)
                .translationY(translationY)
                .setDuration(50)
                .start()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
