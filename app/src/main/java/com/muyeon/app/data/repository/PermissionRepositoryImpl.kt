package com.muyeon.app.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.muyeon.app.domain.repositories.PermissionRepository
import com.muyeon.app.utils.Permission

class PermissionRepositoryImpl : PermissionRepository {

    override fun getCameraPermissionStatus(context: Context): Permission =
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) Permission.Authorized
        else Permission.Rejected


    override fun getMediaPermissionStatus(context: Context): Permission {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                Permission.Authorized
            } else {
                val persisted = context.contentResolver.persistedUriPermissions
                    .count { it.isReadPermission }
                if (persisted > 0) {
                    Permission.Limited
                } else {
                    Permission.Rejected
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Permission.Authorized
            } else {
                Permission.Rejected
            }
        }
    }
}
