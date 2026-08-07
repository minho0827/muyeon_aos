package com.muyeon.app.ui.permission_media

import android.content.Context
import androidx.lifecycle.ViewModel
import com.muyeon.app.domain.models.media.PermissionType
import com.muyeon.app.utils.Permission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.lifecycle.viewModelScope
import com.muyeon.app.domain.use_cases.PermissionUseCase
import kotlinx.coroutines.launch

class PermissionViewModel(
    private val requestPermissionUseCase: PermissionUseCase
) : ViewModel() {

    private val _cameraPermission = MutableStateFlow(Permission.Unknown)
    val cameraPermission: StateFlow<Permission> = _cameraPermission.asStateFlow()

    private val _mediaPermission = MutableStateFlow(Permission.Unknown)
    val mediaPermission: StateFlow<Permission> = _mediaPermission.asStateFlow()

    private val _requestPermission = MutableSharedFlow<PermissionType?>()
    val requestPermission: SharedFlow<PermissionType?> = _requestPermission.asSharedFlow()

    fun onCameraClick() {
        viewModelScope.launch {
            _requestPermission.emit(PermissionType.Camera)
        }
    }

    fun onGalleryClick() {
        viewModelScope.launch {
            _requestPermission.emit(PermissionType.Gallery)
        }
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _cameraPermission.value = if (granted) Permission.Authorized else Permission.Rejected
    }

    fun onMediaPermissionResult(context: Context) {
        val (cameraStatus, mediaStatus) = requestPermissionUseCase(context)
        _mediaPermission.value = mediaStatus
        _cameraPermission.value = cameraStatus
    }

    @Suppress("unused")
    fun checkPermissionsOnResume(context: Context) {
        val (cameraStatus, mediaStatus) = requestPermissionUseCase(context)
        _cameraPermission.value = cameraStatus
        _mediaPermission.value = mediaStatus
    }

    fun onRequestHandled() {
        viewModelScope.launch {
            _requestPermission.emit(null)
        }
    }
}