package com.ultracam.pro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ultracam.pro.databinding.ActivityMainBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalCamera2Interop::class)
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private var cameraControl: CameraControl? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) { startCamera() } 
        else { ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10) }

        binding.btnCapture.setOnClickListener { takeOptimizedPhoto() }
        setupTapToFocus()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
            val imageAnalysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            
            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val buffer = imageProxy.planes[0].buffer
                val data = ByteArray(buffer.remaining())
                buffer.get(data)
                val r = IntArray(256); val g = IntArray(256); val b = IntArray(256)
                var i = 0
                while (i < data.size - 4) {
                    r[data[i].toInt() and 0xFF]++; g[data[i+1].toInt() and 0xFF]++; b[data[i+2].toInt() and 0xFF]
                    i += 16
                }
                runOnUiThread { binding.histogramView.updateHistogram(r, g, b) }
                imageProxy.close()
            }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, imageAnalysis)
                cameraControl = camera.cameraControl
                val hardwareLevel = Camera2CameraInfo.from(camera.cameraInfo).getCameraCharacteristic(android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                binding.txtStatus.text = "Hardware Level: $hardwareLevel | 100% Optimizado"
            } catch (exc: Exception) { Log.e("UltraCam", "Error", exc) }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupTapToFocus() {
        binding.viewFinder.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val factory = binding.viewFinder.meteringPointFactory
                cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(factory.createPoint(event.x, event.y)).build())
                true
            } else false
        }
    }

    private fun takeOptimizedPhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(externalMediaDirs.firstOrNull()?.let { File(it, "UltraCam").apply { mkdirs() } } ?: filesDir, "ULTRA_${System.currentTimeMillis()}.jpg")
        imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(photoFile).build(), ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) { Toast.makeText(baseContext, "¡Foto guardada!", Toast.LENGTH_SHORT).show() }
            override fun onError(exc: ImageCaptureException) { Log.e("UltraCam", "Error", exc) }
        })
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    override fun onDestroy() { super.onDestroy(); cameraExecutor.shutdown() }
}
