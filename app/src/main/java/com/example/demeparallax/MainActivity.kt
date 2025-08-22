package com.example.demeparallax

import android.content.Context
import android.graphics.Matrix
import android.graphics.drawable.Drawable
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
    private lateinit var backgroundLayer: ImageView      // Layer 1 - Xa nhất (dùng Matrix)
    private lateinit var mountainLayer: ImageView       // Layer 2 - Núi xa (dùng translation)
    private lateinit var treesLayer: ImageView          // Layer 3 - Cây cối (dùng translation)
    private lateinit var foregroundLayer: ImageView     // Layer 4 - Gần nhất (dùng translation)

    // Vị trí hiện tại của mỗi layer
    private var currentX = 0f
    private var currentY = 0f

    // Kích thước màn hình
    private var screenWidth = 0
    private var screenHeight = 0

    // Matrix chỉ cho background layer
    private val backgroundMatrix = Matrix()

    // Thông tin ảnh cho việc tính toán giới hạn
    private var backgroundImageWidth = 0f
    private var backgroundImageHeight = 0f

    // Độ nhạy và tốc độ di chuyển cho từng layer
    private val sensitivity = 8f
    private val backgroundSpeed = 3f      // Background dùng Matrix
    private val mountainSpeed = 2f        // Mountain dùng translation
    private val treesSpeed = 1f          // Trees dùng translation
    private val foregroundSpeed = 0.8f     // Foreground dùng translation

    // Scale factor cho background (để có thể di chuyển)
    private val backgroundScale = 1.3f

    // Giới hạn di chuyển cho các layer translation
    private var maxMoveX = 100f
    private var maxMoveY = 60f

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
        // CHỈ background layer dùng Matrix
        backgroundLayer.scaleType = ImageView.ScaleType.MATRIX

        // Các layer khác giữ nguyên scaleType từ XML
        // mountainLayer, treesLayer, foregroundLayer giữ scaleType gốc

        // Đợi layout hoàn thành để setup background matrix
        backgroundLayer.post {
            setupBackgroundMatrix()
            updateAllLayers()
        }

        // Tính giới hạn di chuyển cho translation layers
        maxMoveX = screenWidth * 0.08f   // 8% chiều rộng màn hình
        maxMoveY = screenHeight * 0.05f  // 5% chiều cao màn hình

        // Đặt vị trí ban đầu ở giữa
        currentX = 0f
        currentY = 0f
    }

    private fun setupBackgroundMatrix() {
        val drawable = backgroundLayer.drawable ?: return

        // Lấy kích thước của ImageView
        val viewWidth = backgroundLayer.width.toFloat()
        val viewHeight = backgroundLayer.height.toFloat()

        // Lấy kích thước gốc của ảnh
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()

        // Tính scale để ảnh phủ kín view và có thể di chuyển
        val scaleX = (viewWidth * backgroundScale) / imageWidth
        val scaleY = (viewHeight * backgroundScale) / imageHeight
        val scale = max(scaleX, scaleY)

        // Tính vị trí để center ảnh
        val scaledImageWidth = imageWidth * scale
        val scaledImageHeight = imageHeight * scale
        val translateX = (viewWidth - scaledImageWidth) / 2f
        val translateY = (viewHeight - scaledImageHeight) / 2f

        // Thiết lập matrix
        backgroundMatrix.reset()
        backgroundMatrix.postScale(scale, scale)
        backgroundMatrix.postTranslate(translateX, translateY)

        backgroundLayer.imageMatrix = backgroundMatrix

        // Lưu thông tin để tính giới hạn
        backgroundImageWidth = scaledImageWidth
        backgroundImageHeight = scaledImageHeight
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
                val x = it.values[0]
                val y = it.values[1]

                val targetX = currentX - (x * sensitivity)
                val targetY = currentY + (y * sensitivity)

                val smoothing = 0.08f
                currentX += (targetX - currentX) * smoothing
                currentY += (targetY - currentY) * smoothing

                updateAllLayers()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Không cần xử lý
    }

    private fun updateAllLayers() {
        // Background layer - dùng Matrix (không có giới hạn đen)
        updateBackgroundMatrix(currentX * backgroundSpeed, currentY * backgroundSpeed)

        // Các layer khác - dùng translation với giới hạn
        val mountainMoveX = max(-maxMoveX * 0.8f, min(currentX * mountainSpeed, maxMoveX * 0.8f))
        val mountainMoveY = max(-maxMoveY * 0.8f, min(currentY * mountainSpeed, maxMoveY * 0.8f))
        mountainLayer.translationX = mountainMoveX
        mountainLayer.translationY = mountainMoveY

        val treesMoveX = max(-maxMoveX * 0.6f, min(currentX * treesSpeed, maxMoveX * 0.6f))
        val treesMoveY = max(-maxMoveY * 0.6f, min(currentY * treesSpeed, maxMoveY * 0.6f))
        treesLayer.translationX = treesMoveX
        treesLayer.translationY = treesMoveY

        val foregroundMoveX = max(-maxMoveX * 0.4f, min(currentX * foregroundSpeed, maxMoveX * 0.4f))
        val foregroundMoveY = max(-maxMoveY * 0.4f, min(currentY * foregroundSpeed, maxMoveY * 0.4f))
        foregroundLayer.translationX = foregroundMoveX
        foregroundLayer.translationY = foregroundMoveY
    }

    private fun updateBackgroundMatrix(moveX: Float, moveY: Float) {
        val drawable = backgroundLayer.drawable ?: return
        val viewWidth = backgroundLayer.width.toFloat()
        val viewHeight = backgroundLayer.height.toFloat()
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()

        // Tính scale
        val scaleX = (viewWidth * backgroundScale) / imageWidth
        val scaleY = (viewHeight * backgroundScale) / imageHeight
        val scale = max(scaleX, scaleY)

        // Tính vị trí center ban đầu
        val scaledImageWidth = imageWidth * scale
        val scaledImageHeight = imageHeight * scale
        val baseCenterX = (viewWidth - scaledImageWidth) / 2f
        val baseCenterY = (viewHeight - scaledImageHeight) / 2f

        // Tính giới hạn di chuyển
        val maxBackgroundMoveX = (scaledImageWidth - viewWidth) / 2f
        val maxBackgroundMoveY = (scaledImageHeight - viewHeight) / 2f

        // Giới hạn di chuyển
        val limitedMoveX = max(-maxBackgroundMoveX, min(moveX, maxBackgroundMoveX))
        val limitedMoveY = max(-maxBackgroundMoveY, min(moveY, maxBackgroundMoveY))

        // Áp dụng di chuyển
        val finalX = baseCenterX + limitedMoveX
        val finalY = baseCenterY + limitedMoveY

        // Thiết lập matrix
        backgroundMatrix.reset()
        backgroundMatrix.postScale(scale, scale)
        backgroundMatrix.postTranslate(finalX, finalY)

        backgroundLayer.imageMatrix = backgroundMatrix
        backgroundLayer.invalidate() // Đảm bảo vẽ lại
    }
}