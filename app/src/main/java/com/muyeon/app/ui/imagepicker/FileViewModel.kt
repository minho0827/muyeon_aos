package com.muyeon.app.ui.imagepicker


import android.net.Uri
import androidx.lifecycle.ViewModel
import com.muyeon.app.data.models.file.FileImageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FileViewModel : ViewModel() {
    private val _allowedImages = MutableStateFlow<List<Uri>>(emptyList())
    val allowedImages: StateFlow<List<Uri>> = _allowedImages.asStateFlow()

    private val _showImagePicker = MutableStateFlow(false)
    val showImagePicker: StateFlow<Boolean> = _showImagePicker.asStateFlow()

    private var fileInfoToSend: FileImageInfo? = null

    fun updateAllowedImages(uris: List<Uri>) {
        _allowedImages.value = uris
    }

    fun showPicker() {
        _showImagePicker.value = true
    }

    fun hidePicker() {
        _showImagePicker.value = false
        // Clear any pending state when the picker is dismissed
        fileInfoToSend = null
    }

    @Suppress("unused")
    fun setFileInfoForPicker(fileInfo: FileImageInfo) {
        fileInfoToSend = fileInfo
    }
}