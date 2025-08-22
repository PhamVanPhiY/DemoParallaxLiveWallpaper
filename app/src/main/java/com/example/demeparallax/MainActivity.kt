package com.example.demeparallax

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Các layer parallax (từ xa đến gần)
    private lateinit var backgroundLayer: ImageView      // Layer 1 - Xa nhất (di chuyển nhanh nhất)
    private lateinit var mountainLayer: ImageView       // Layer 2 - Núi xa
    private lateinit var treesLayer: ImageView          // Layer 3 - Cây cối
    private lateinit var foregroundLayer: ImageView     // Layer 4 - Gần nhất (di chuyển chậm nhất)

    // Vị trí hiện tại của mỗi layer
    private var currentX = 0f
    private var currentY = 0f

    // Kích thước màn hình
    private var screenWidth = 0
    private var screenHeight = 0

    // Độ nhạy và tốc độ di chuyển cho từng layer
    private val sensitivity = 12f
    private val backgroundSpeed = 1.5f      // Nhanh nhất
    private val mountainSpeed = 1.0f        // Trung bình
    private val treesSpeed = 0.6f          // Chậm hơn
    private val foregroundSpeed = 0.3f      // Chậm nhất

    // Giới hạn di chuyển
    private var maxMoveX = 200f
    private var maxMoveY = 100f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Khởi tạo các ImageView layers
        backgroundLayer = findViewById(R.id.backgroundLayer)
        mountainLayer = findViewById(R.id.mountainLayer)
        treesLayer = findViewById(R.id.treesLayer)
        foregroundLayer = findViewById(R.id.foregroundLayer)

        // Khởi tạo SensorManager và Accelerometer
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Lấy kích thước màn hình
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels

        // Thiết lập parallax layers
        setupParallaxLayers()
    }

    private fun setupParallaxLayers() {
        // Tính toán giới hạn di chuyển dựa trên kích thước màn hình
        maxMoveX = screenWidth * 0.15f  // 15% chiều rộng màn hình
        maxMoveY = screenHeight * 0.08f // 8% chiều cao màn hình

        // Đặt vị trí ban đầu ở giữa
        currentX = 0f
        currentY = 0f
        updateAllLayers()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // Lấy giá trị accelerometer
                val x = it.values[0] // Nghiêng trái/phải
                val y = it.values[1] // Nghiêng lên/xuống

                // Tính toán vị trí mới với smoothing
                val targetX = currentX - (x * sensitivity)
                val targetY = currentY + (y * sensitivity)

                // Áp dụng smoothing (lerp) để chuyển động mượt mà
                val smoothing = 0.08f
                currentX += (targetX - currentX) * smoothing
                currentY += (targetY - currentY) * smoothing

                // Giới hạn di chuyển
                currentX = max(-maxMoveX, min(currentX, maxMoveX))
                currentY = max(-maxMoveY, min(currentY, maxMoveY))

                // Cập nhật tất cả các layer
                updateAllLayers()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Không cần xử lý
    }

    private fun updateAllLayers() {
        // Cập nhật từng layer với tốc độ khác nhau để tạo hiệu ứng 3D

        // Background layer - Di chuyển nhanh nhất (xa nhất)
        backgroundLayer.translationX = currentX * backgroundSpeed
        backgroundLayer.translationY = currentY * backgroundSpeed

        // Mountain layer - Di chuyển trung bình
        mountainLayer.translationX = currentX * mountainSpeed
        mountainLayer.translationY = currentY * mountainSpeed

        // Trees layer - Di chuyển chậm hơn
        treesLayer.translationX = currentX * treesSpeed
        treesLayer.translationY = currentY * treesSpeed

        // Foreground layer - Di chuyển chậm nhất (gần nhất)
        foregroundLayer.translationX = currentX * foregroundSpeed
        foregroundLayer.translationY = currentY * foregroundSpeed
    }
}