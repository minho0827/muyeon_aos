package com.muyeon.app.domain.use_cases

import android.content.Context
import com.muyeon.app.domain.repositories.PermissionRepository
import com.muyeon.app.utils.Permission

class PermissionUseCase(
    private val permissionRepository: PermissionRepository
) {
    operator fun invoke(context: Context): Pair<Permission, Permission> {
        val cameraStatus = permissionRepository.getCameraPermissionStatus(context)
        val mediaStatus = permissionRepository.getMediaPermissionStatus(context)
        return Pair(cameraStatus, mediaStatus)
    }
    fun getCameraPermissionStatus(context: Context): Permission {
        return permissionRepository.getCameraPermissionStatus(context)
    }

    @Suppress("unused")
    fun getMediaPermissionStatus(context: Context): Permission {
        return permissionRepository.getMediaPermissionStatus(context)
    }
}