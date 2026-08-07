package com.muyeon.app.ui.device_info

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DeviceInfoViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceInfoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeviceInfoViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
