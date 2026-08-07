package com.muyeon.app.ui.permission_media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.muyeon.app.domain.use_cases.PermissionUseCase

class PermissionViewModelFactory(
    private val requestPermissionUseCase: PermissionUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PermissionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PermissionViewModel(requestPermissionUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
