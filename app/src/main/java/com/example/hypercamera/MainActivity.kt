@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.hypercamera

import CameraPreview
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hypercamera.ui.theme.HyperCameraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope


class MainActivity : ComponentActivity() {

    private var recording: Recording? = null

    // Estado para saber si está grabando
    private val isRecordingState = mutableStateOf(false)

    // Estado para el tiempo de grabación
    private val recordingTimeState = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                this, CAMERAX_PERMISSIONS, 0
            )
        }
        setContent {
            HyperCameraTheme {
                val scope = rememberCoroutineScope()
                val scaffoldState = rememberBottomSheetScaffoldState()
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(
                            CameraController.IMAGE_CAPTURE or
                                    CameraController.VIDEO_CAPTURE
                        )
                    }
                }
                val viewModel = viewModel<MainViewModel>()
                val bitmaps by viewModel.bitmaps.collectAsState()

                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    sheetPeekHeight = 0.dp,
                    sheetContent = {
                        PhotoBottomSheetContent(
                            bitmaps = bitmaps,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                ){ padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ){
                        CameraPreview(
                            controller = controller,
                            modifier = Modifier
                                .fillMaxSize()
                        )

                        IconButton(
                            onClick = {
                                controller.cameraSelector =
                                    if(controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA){
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    } else CameraSelector.DEFAULT_BACK_CAMERA
                            },
                            modifier = Modifier
                                .offset(16.dp, 16.dp)
                        ){
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera"
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ){
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        scaffoldState.bottomSheetState.expand()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Photo,
                                    contentDescription = "Open Gallery"
                                )
                            }
                            IconButton(
                                onClick = {
                                    takePhoto(
                                        controller = controller,
                                        onPhotoTaken = viewModel::onTakePhoto
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Take a photo"
                                )
                            }
                            IconButton(
                                onClick = {
                                    recordVideo(controller)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Record video"
                                )
                            }
                        }

                        // Mostrar el tiempo de grabación y mensaje de grabación
                        if (isRecordingState.value) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 48.dp)
                                    .background(Color(0x99000000), shape = MaterialTheme.shapes.medium) // Fondo semi-transparente
                                    .padding(horizontal = 16.dp, vertical = 8.dp) // Relleno alrededor del texto
                            ) {
                                Text(
                                    text = "Recording: ${recordingTimeState.value}s",
                                    color = Color.White, // Texto blanco para contraste
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold, // Negritas para mayor impacto
                                        fontSize = 18.sp // Tamaño de fuente ajustado
                                    )
                                )
                            }
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

        if (!hasRequiredPermissions()) {
            return
        }

        // Guardar el archivo en el directorio de películas
        val outputFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "my-recording-${System.currentTimeMillis()}.mp4"
        )

        val scope = lifecycleScope // Usa esto para lanzar una corrutina
        recordingTimeState.value = 0
        isRecordingState.value = true

        // Inicia un contador del tiempo
        scope.launch {
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
            when (event) {
                is VideoRecordEvent.Finalize -> {
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
    }

    private fun addVideoToGallery(file: File) {
        val context = applicationContext
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            null,
            object : MediaScannerConnection.OnScanCompletedListener {
                override fun onScanCompleted(path: String?, uri: Uri?) {
                    Log.d("MainActivity", "Video added to gallery: $path")
                }
            }
        )
    }

    private fun takePhoto(
        controller: LifecycleCameraController,
        onPhotoTaken: (Bitmap) -> Unit
    ) {
        if (!hasRequiredPermissions()) {
            return
        }
        controller.takePicture(
            ContextCompat.getMainExecutor(applicationContext),
            object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

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
                    super.onError(exception)
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
            permissions += "android.permission.READ_MEDIA_VIDEO"
            permissions += "android.permission.READ_MEDIA_IMAGES"
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
