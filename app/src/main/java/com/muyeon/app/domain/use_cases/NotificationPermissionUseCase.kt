package com.muyeon.app.domain.use_cases

import android.Manifest
import android.os.Build
import androidx.activity.result.ActivityResultLauncher

class RequestNotificationPermissionUseCase {

    fun execute(
        hasPermission: Boolean,
        launcher: ActivityResultLauncher<String>
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return when {
            hasPermission -> true
            else -> {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                false
            }
        }
    }
}
