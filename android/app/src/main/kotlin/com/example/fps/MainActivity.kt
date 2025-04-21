package com.example.fps

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

class MainActivity: FlutterActivity() {
    private val CHANNEL = "native_video_saver"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
            call, result ->
                if (call.method == "saveVideoToGallery") {
                    val path = call.argument<String>("path")
                    if (path != null) {
                        val saved = saveVideo(path)
                        result.success(saved)
                    } else {
                        result.error("NO_PATH", "Path is null", null)
                    }
                }
        }
    }

    private fun saveVideo(filePath: String): Boolean {
        val videoFile = File(filePath)
        if (!videoFile.exists()) return false

        val resolver = applicationContext.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/MyApp")
        }

        val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        videoUri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                FileInputStream(videoFile).use { inputStream ->
                    inputStream.copyTo(outputStream as OutputStream)
                }
            }
            return true
        }

        return false
    }
}
