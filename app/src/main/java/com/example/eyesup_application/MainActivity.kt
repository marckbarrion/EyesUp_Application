package com.example.eyesup_application

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.eyesup_application.Constants.LABELS_PATH
import com.example.eyesup_application.Constants.MODEL_PATH
import com.example.eyesup_application.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding  // Initialize view binding
    private val isFrontCamera = false  // Set the default camera to back

    private var preview: Preview? = null  // Camera preview use case
    private var imageAnalyzer: ImageAnalysis? = null  // Image analysis use case
    private var camera: Camera? = null  // Camera instance
    private var cameraProvider: ProcessCameraProvider? = null  // Camera provider instance
    private lateinit var detector: Detector  // Object detection instance

    private lateinit var cameraExecutor: ExecutorService  // Executor for camera operations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)  // Inflate the layout
        setContentView(binding.root)  // Set the content view to the inflated layout

        detector = Detector(baseContext, MODEL_PATH, LABELS_PATH, this)  // Initialize detector with model and labels
        detector.setup()  // Setup the detector

        if (allPermissionsGranted()) {
            startCamera()  // Start the camera if permissions are granted
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)  // Request necessary permissions
        }

        cameraExecutor = Executors.newSingleThreadExecutor()  // Initialize the camera executor
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)  // Get camera provider instance
        cameraProviderFuture.addListener({
            cameraProvider  = cameraProviderFuture.get()  // Assign the camera provider
            bindCameraUseCases()  // Bind camera use cases
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")  // Check for camera provider

        val rotation = binding.viewFinder.display.rotation  // Get display rotation

        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)  // Select back camera
            .build()

        preview =  Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)  // Set aspect ratio for preview
            .setTargetRotation(rotation)  // Set target rotation
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)  // Set aspect ratio for image analysis
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)  // Set backpressure strategy
            .setTargetRotation(binding.viewFinder.display.rotation)  // Set target rotation
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)  // Set output image format
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->  // Set analyzer for image analysis
            val bitmapBuffer =
                Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }  // Copy pixels to bitmap buffer
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())  // Rotate image according to rotation degrees

                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )  // Flip image for front camera
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            detector.detect(rotatedBitmap)  // Perform object detection on the rotated bitmap
        }

        cameraProvider.unbindAll()  // Unbind previous use cases

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )  // Bind preview and image analysis to lifecycle

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)  // Set the surface provider for preview
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)  // Log error if binding fails
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED  // Check if all permissions are granted
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        if (it[Manifest.permission.CAMERA] == true) { startCamera() }  // Start camera if permission is granted
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.clear()  // Clear detector resources
        cameraExecutor.shutdown()  // Shutdown camera executor
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()){
            startCamera()  // Restart camera on resume if permissions are granted
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)  // Request permissions if not granted
        }
    }

    companion object {
        private const val TAG = "Camera"  // Tag for logging
        private const val REQUEST_CODE_PERMISSIONS = 10  // Request code for permissions
        private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA
        ).toTypedArray()  // Array of required permissions
    }

    override fun onEmptyDetect() {
        binding.overlay.invalidate()  // Invalidate overlay on empty detection
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms"  // Display inference time
            binding.overlay.apply {
                setResults(boundingBoxes)  // Set detection results
                invalidate()  // Invalidate overlay to trigger redraw
            }
        }
    }
}
