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
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Các layer parallax (từ xa đến gần)
    private lateinit var backgroundLayer: ImageView      // Layer 1 - Xa nhất
    private lateinit var mountainLayer: ImageView       // Layer 2 - Núi xa
    private lateinit var treesLayer: ImageView          // Layer 3 - Cây cối
    private lateinit var foregroundLayer: ImageView     // Layer 4 - Gần nhất

    // Vị trí hiện tại của mỗi layer
    private var currentX = 0f
    private var currentY = 0f

    // Kích thước màn hình
    private var screenWidth = 0
    private var screenHeight = 0

    // Matrix cho từng layer
    private val backgroundMatrix = Matrix()
    private val mountainMatrix = Matrix()
    private val treesMatrix = Matrix()
    private val foregroundMatrix = Matrix()

    // Thông tin ảnh cho việc tính toán giới hạn
    private var backgroundImageWidth = 0f
    private var backgroundImageHeight = 0f
    private var mountainImageWidth = 0f
    private var mountainImageHeight = 0f
    private var treesImageWidth = 0f
    private var treesImageHeight = 0f
    private var foregroundImageWidth = 0f
    private var foregroundImageHeight = 0f

    // Độ nhạy và tốc độ di chuyển cho từng layer
    private val sensitivity = 8f
    private val backgroundSpeed = 6f      // Background - di chuyển nhanh nhất
    private val mountainSpeed = 5.5f        // Mountain - di chuyển trung bình
    private val treesSpeed = 5.5f          // Trees - di chuyển chậm hơn
    private val foregroundSpeed = 3f     // Foreground - di chuyển chậm nhất

    // Scale factor cho từng layer (để có thể di chuyển)
    private val backgroundScale = 1.3f
    private val mountainScale = 1.2f
    private val treesScale = 1.15f
    private val foregroundScale = 1.1f

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
        // TẤT CẢ các layer đều dùng Matrix
        backgroundLayer.scaleType = ImageView.ScaleType.MATRIX
        mountainLayer.scaleType = ImageView.ScaleType.MATRIX
        treesLayer.scaleType = ImageView.ScaleType.MATRIX
        foregroundLayer.scaleType = ImageView.ScaleType.MATRIX

        // Đợi layout hoàn thành để setup matrix cho tất cả layers
        backgroundLayer.post {
            setupAllMatrices()
            updateAllLayers()
        }

        // Đặt vị trí ban đầu ở giữa
        currentX = 0f
        currentY = 0f
    }

    private fun setupAllMatrices() {
        setupLayerMatrix(backgroundLayer, backgroundMatrix, backgroundScale) { width, height ->
            backgroundImageWidth = width
            backgroundImageHeight = height
        }

        setupLayerMatrix(mountainLayer, mountainMatrix, mountainScale) { width, height ->
            mountainImageWidth = width
            mountainImageHeight = height
        }

        setupLayerMatrix(treesLayer, treesMatrix, treesScale) { width, height ->
            treesImageWidth = width
            treesImageHeight = height
        }

        setupLayerMatrix(foregroundLayer, foregroundMatrix, foregroundScale) { width, height ->
            foregroundImageWidth = width
            foregroundImageHeight = height
        }
    }

    private fun setupLayerMatrix(
        imageView: ImageView,
        matrix: Matrix,
        scale: Float,
        onImageSizeCalculated: (Float, Float) -> Unit
    ) {
        val drawable = imageView.drawable ?: return

        // Lấy kích thước của ImageView
        val viewWidth = imageView.width.toFloat()
        val viewHeight = imageView.height.toFloat()

        // Lấy kích thước gốc của ảnh
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()

        // Tính scale để ảnh phủ kín view và có thể di chuyển
        val scaleX = (viewWidth * scale) / imageWidth
        val scaleY = (viewHeight * scale) / imageHeight
        val finalScale = max(scaleX, scaleY)

        // Tính vị trí để center ảnh
        val scaledImageWidth = imageWidth * finalScale
        val scaledImageHeight = imageHeight * finalScale
        val translateX = (viewWidth - scaledImageWidth) / 2f
        val translateY = (viewHeight - scaledImageHeight) / 2f

        // Thiết lập matrix
        matrix.reset()
        matrix.postScale(finalScale, finalScale)
        matrix.postTranslate(translateX, translateY)

        imageView.imageMatrix = matrix

        // Lưu thông tin kích thước
        onImageSizeCalculated(scaledImageWidth, scaledImageHeight)
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
        // Tất cả layers đều dùng Matrix với tốc độ khác nhau
        updateLayerMatrix(
            backgroundLayer,
            backgroundMatrix,
            backgroundScale,
            backgroundImageWidth,
            backgroundImageHeight,
            currentX * backgroundSpeed,
            currentY * backgroundSpeed
        )

        updateLayerMatrix(
            mountainLayer,
            mountainMatrix,
            mountainScale,
            mountainImageWidth,
            mountainImageHeight,
            currentX * mountainSpeed,
            currentY * mountainSpeed
        )

        updateLayerMatrix(
            treesLayer,
            treesMatrix,
            treesScale,
            treesImageWidth,
            treesImageHeight,
            currentX * treesSpeed,
            currentY * treesSpeed
        )

        updateLayerMatrix(
            foregroundLayer,
            foregroundMatrix,
            foregroundScale,
            foregroundImageWidth,
            foregroundImageHeight,
            currentX * foregroundSpeed,
            currentY * foregroundSpeed
        )
    }

    private fun updateLayerMatrix(
        imageView: ImageView,
        matrix: Matrix,
        scale: Float,
        scaledImageWidth: Float,
        scaledImageHeight: Float,
        moveX: Float,
        moveY: Float
    ) {
        val drawable = imageView.drawable ?: return
        val viewWidth = imageView.width.toFloat()
        val viewHeight = imageView.height.toFloat()
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()

        // Tính scale
        val scaleX = (viewWidth * scale) / imageWidth
        val scaleY = (viewHeight * scale) / imageHeight
        val finalScale = max(scaleX, scaleY)

        // Tính vị trí center ban đầu
        val baseCenterX = (viewWidth - scaledImageWidth) / 2f
        val baseCenterY = (viewHeight - scaledImageHeight) / 2f

        // Tính giới hạn di chuyển
        val maxMoveX = (scaledImageWidth - viewWidth) / 2f
        val maxMoveY = (scaledImageHeight - viewHeight) / 2f

        // Giới hạn di chuyển
        val limitedMoveX = max(-maxMoveX, min(moveX, maxMoveX))
        val limitedMoveY = max(-maxMoveY, min(moveY, maxMoveY))

        // Đảo ngược hướng di chuyển để tạo hiệu ứng parallax tự nhiên
        val finalX = baseCenterX - limitedMoveX
        val finalY = baseCenterY - limitedMoveY

        // Thiết lập matrix
        matrix.reset()
        matrix.postScale(finalScale, finalScale)
        matrix.postTranslate(finalX, finalY)

        imageView.imageMatrix = matrix
        imageView.invalidate()
    }
}