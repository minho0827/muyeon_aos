package com.muyeon.app.ui.notification

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import com.muyeon.app.domain.use_cases.RequestNotificationPermissionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationViewModel(
    private val requestNotificationPermissionUseCase: RequestNotificationPermissionUseCase
) : ViewModel() {

    private val _isPermissionGranted = MutableStateFlow<Boolean?>(null)
    val isPermissionGranted: StateFlow<Boolean?> = _isPermissionGranted.asStateFlow()

    fun updatePermissionStatus(isGranted: Boolean) {
        _isPermissionGranted.value = isGranted
    }

    fun requestNotificationPermission(
        hasPermission: Boolean,
        launcher: ActivityResultLauncher<String>
    ) {
        val isGranted = requestNotificationPermissionUseCase.execute(
            hasPermission,
            launcher
        )
        if (isGranted) {
            _isPermissionGranted.value = true
        }
    }
}