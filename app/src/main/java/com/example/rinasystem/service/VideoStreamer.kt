package com.example.rinasystem.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.rinasystem.data.ws.AriaWebSocket
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors


private const val TAG = "VideoStreamer"

/**
 * จับภาพจาก camera แบบ real-time แล้วส่งผ่าน WebSocket เป็น JPEG binary frames
 * ใช้ CameraX ImageAnalysis use case
 *
 * Resolution:
 *   480p  → 640x480
 *   720p  → 1280x720
 *   1080p → 1920x1080
 */
class VideoStreamer(
    private val context: Context,
    private val ariaWebSocket: AriaWebSocket,
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    var isActive: Boolean = false
        private set

    private var jpegQuality: Int = 60
    private var frameIntervalMs: Long = 1000L / 24  // 24fps default
    private var currentResolution: String = "720p"
    private var currentFps: Int = 24
    private var currentCameraSide: String = "back"

    fun start(
        lifecycleOwner: LifecycleOwner,
        resolution: String = "720p",
        fps: Int = 24,
        cameraSide: String = "back",
    ) {
        AriaLogBuffer.log(TAG, "start() called: res=$resolution fps=$fps side=$cameraSide isActive=$isActive")
        if (isActive) return
        isActive = true

        currentResolution = resolution
        currentFps = fps
        currentCameraSide = cameraSide
        frameIntervalMs = if (fps > 0) 1000L / fps else 41L
        jpegQuality = when (resolution) {
            "1080p" -> 80
            "480p" -> 40
            else -> 60  // 720p
        }

        val targetSize = when (resolution) {
            "1080p" -> Size(1920, 1080)
            "480p" -> Size(640, 480)
            else -> Size(1280, 720)  // 720p
        }

        val cameraSelector = if (cameraSide == "front") {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            cameraProvider = providerFuture.get()

            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(targetSize)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            var lastFrameMs = 0L
            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                val now = System.currentTimeMillis()
                if (now - lastFrameMs >= frameIntervalMs) {
                    lastFrameMs = now
                    processFrame(imageProxy)
                } else {
                    imageProxy.close()
                }
            }

            imageAnalysis = analysis
            try {
                val provider = cameraProvider
                if (provider == null) {
                    Log.e(TAG, "CameraProvider is null")
                    AriaLogBuffer.log(TAG, "ERROR: CameraProvider is null")
                    isActive = false
                    return@addListener
                }
                AriaLogBuffer.log(TAG, "unbindAll + bindToLifecycle...")
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, analysis)
                Log.i(TAG, "Camera streaming started: res=$resolution fps=$fps side=$cameraSide")
                AriaLogBuffer.log(TAG, "Camera bound OK: res=$resolution fps=$fps side=$cameraSide")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera: ${e.message}", e)
                AriaLogBuffer.log(TAG, "ERROR bindToLifecycle: ${e.message}")
                isActive = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private var frameCount = 0L

    private fun processFrame(imageProxy: ImageProxy) {
        try {
            val jpegBytes = imageProxy.toJpegBytes(jpegQuality)
            if (jpegBytes != null) {
                ariaWebSocket.sendVideoFrame(jpegBytes)
                frameCount++
                if (frameCount % 30 == 0L) {
                    Log.d(TAG, "Sent $frameCount frames (last: ${jpegBytes.size} bytes)")
                    AriaLogBuffer.log(TAG, "Sent $frameCount frames (${jpegBytes.size} bytes)")
                }
                if (frameCount == 1L) {
                    AriaLogBuffer.log(TAG, "First frame sent! ${jpegBytes.size} bytes")
                }
            } else {
                Log.w(TAG, "toJpegBytes returned null for frame ${imageProxy.width}x${imageProxy.height} format=${imageProxy.format}")
                AriaLogBuffer.log(TAG, "toJpegBytes null! ${imageProxy.width}x${imageProxy.height} fmt=${imageProxy.format}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Frame processing error: ${e.message}")
            AriaLogBuffer.log(TAG, "Frame error: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, cameraSide: String) {
        stop()
        start(lifecycleOwner, currentResolution, currentFps, cameraSide)
    }

    fun stop() {
        isActive = false
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageAnalysis = null
        Log.i(TAG, "Camera streaming stopped")
    }

    private fun ImageProxy.toJpegBytes(quality: Int): ByteArray? {
        return try {
            val rawBitmap = this.toBitmap()
            val rotation = this.imageInfo.rotationDegrees
            val bitmap = if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                rawBitmap.recycle()
                rotated
            } else {
                rawBitmap
            }
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            bitmap.recycle()
            bytes
        } catch (e: Exception) {
            Log.e(TAG, "toJpegBytes failed: ${e.message}")
            null
        }
    }
}
