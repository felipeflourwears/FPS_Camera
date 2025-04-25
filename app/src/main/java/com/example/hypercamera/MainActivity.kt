@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.hypercamera

import CameraPreview
import SliderExample
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.hypercamera.ui.theme.HyperCameraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private var recording: Recording? = null
    private val isRecordingState = mutableStateOf(false)
    private val recordingTimeState = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(this, CAMERAX_PERMISSIONS, 0)
        }

        val viewModel: MainViewModel by viewModels()

        setContent {
            HyperCameraTheme {
                val scope = rememberCoroutineScope()
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE)
                    }
                }
                val bitmaps by viewModel.bitmaps.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(controller = controller, modifier = Modifier.fillMaxSize())

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!isRecordingState.value) {
                            SliderExample(
                                minValue = 1f / 62f,
                                maxValue = 1f / 40f,
                                initialValue = 1f / 50f,
                                onValueChange = { newValue ->
                                    // Puedes mapear esto a un valor de shutter speed real si lo deseas
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        IconButton(
                            onClick = { recordVideo(controller) },
                            modifier = Modifier
                                .padding(8.dp)
                                .size(64.dp)
                                .shadow(elevation = 8.dp, shape = CircleShape)
                                .background(color = Color.Red, shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Grabar video",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    if (isRecordingState.value) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 48.dp)
                                .background(Color(0x99000000), shape = MaterialTheme.shapes.medium)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Recording: ${recordingTimeState.value}s",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun recordVideo(controller: LifecycleCameraController) {
        if (recording != null) {
            recording?.stop()
            recording = null
            isRecordingState.value = false
            return
        }

        if (!hasRequiredPermissions()) return

        val outputFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "my-recording-${System.currentTimeMillis()}.mp4"
        )

        recordingTimeState.value = 0
        isRecordingState.value = true

        lifecycleScope.launch {
            while (isRecordingState.value) {
                delay(1000)
                recordingTimeState.value += 1
            }
        }

        recording = controller.startRecording(
            FileOutputOptions.Builder(outputFile).build(),
            AudioConfig.create(true),
            ContextCompat.getMainExecutor(applicationContext)
        ) { event ->
            if (event is VideoRecordEvent.Finalize) {
                isRecordingState.value = false
                if (event.hasError()) {
                    recording?.close()
                    recording = null
                    Toast.makeText(applicationContext, "Video Capture Failed", Toast.LENGTH_LONG).show()
                } else {
                    addVideoToGallery(outputFile)
                    Toast.makeText(applicationContext, "Video Capture Succeeded", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun addVideoToGallery(file: File) {
        MediaScannerConnection.scanFile(
            applicationContext,
            arrayOf(file.absolutePath),
            null
        ) { path, uri ->
            Log.d("MainActivity", "Video added to gallery: $path")
        }
    }

    private fun takePhoto(controller: LifecycleCameraController, onPhotoTaken: (Bitmap) -> Unit) {
        if (!hasRequiredPermissions()) return

        controller.takePicture(
            ContextCompat.getMainExecutor(applicationContext),
            object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                        postScale(-1f, 1f)
                    }

                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )
                    onPhotoTaken(rotatedBitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("Camera", "Couldn't take photo: ", exception)
                }
            }
        )
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.READ_MEDIA_VIDEO
            permissions += Manifest.permission.READ_MEDIA_IMAGES
        } else {
            permissions += Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val CAMERAX_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
}
