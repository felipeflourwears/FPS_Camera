package com.example.hypercamera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel: ViewModel() {

    private val _bitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val bitmaps = _bitmaps.asStateFlow()
    private val _shutterSpeed = MutableStateFlow(0.5f)  // 0.5 es el valor por defecto (en el medio)
    val shutterSpeed = _shutterSpeed.asStateFlow()

    fun onTakePhoto(bitmap: Bitmap) {
        _bitmaps.value += bitmap
    }
    fun updateShutterSpeed(value: Float) {
        _shutterSpeed.value = value
    }
}