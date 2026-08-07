package com.muyeon.app.domain.repositories

import android.content.Context
import com.muyeon.app.utils.Permission

interface PermissionRepository {
    fun getCameraPermissionStatus(context: Context): Permission
    fun getMediaPermissionStatus(context: Context): Permission
}
