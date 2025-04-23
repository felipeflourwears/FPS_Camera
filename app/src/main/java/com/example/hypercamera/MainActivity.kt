@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.hypercamera

import CameraPreview
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.hypercamera.ui.theme.HyperCameraTheme

import android.Manifest
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasRequiredPermissions()){
            ActivityCompat.requestPermissions(
                this, CAMERAX_PERMISSIONS, 0
            )
        }
        setContent {
            HyperCameraTheme {
                val scaffoldState = rememberBottomSheetScaffoldState()
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply{
                        setEnabledUseCases(
                            CameraController.IMAGE_CAPTURE or
                                CameraController.VIDEO_CAPTURE
                        )
                    }
                }

                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    sheetPeekHeight = 0.dp,
                    sheetContent = {

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
                    }

                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object{
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )
    }
}



