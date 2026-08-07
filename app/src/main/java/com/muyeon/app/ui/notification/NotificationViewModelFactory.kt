package com.muyeon.app.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.muyeon.app.domain.use_cases.RequestNotificationPermissionUseCase

class NotificationViewModelFactory(
    private val requestNotificationPermissionUseCase: RequestNotificationPermissionUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(requestNotificationPermissionUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
