package com.muyeon.app.domain.models.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

fun interface PermissionCallback {
    fun onPermissionResult(result: PermissionResult)
}
@Suppress("unused")
class PermissionHandler(
    activity: Any,
    private val callback: PermissionCallback
) {

    companion object {
        const val PERMISSION_REQUEST_CODE = 1
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private const val PREFS_NAME = "location_permissions"
        private const val KEY_HAS_ASKED_PERMISSION = "has_asked_location_permission"
        private const val KEY_GRANTED_ONCE = "granted_once"
    }

    private val context: Context = activity as Context
    private val androidActivity: Activity = activity as Activity

    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun checkPermissions(): PermissionResult {
        val allGranted = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            val isGrantedOnce = sharedPrefs.getBoolean(KEY_GRANTED_ONCE, false)
            return if (isGrantedOnce) {
                PermissionResult.GRANTED_ONCE
            } else {
                PermissionResult.GRANTED_PERMANENTLY
            }
        }

        val hasAskedBefore = sharedPrefs.getBoolean(KEY_HAS_ASKED_PERMISSION, false)
        val shouldShowRationale = REQUIRED_PERMISSIONS.any {
            ActivityCompat.shouldShowRequestPermissionRationale(androidActivity, it)
        }

        return when {
            !hasAskedBefore -> PermissionResult.DENIED_WITH_RATIONALE
            shouldShowRationale -> PermissionResult.DENIED_WITH_RATIONALE
            else -> PermissionResult.DENIED_PERMANENTLY
        }
    }

    fun requestPermissions() {
        val isFirstRequest = !sharedPrefs.getBoolean(KEY_HAS_ASKED_PERMISSION, false)

        sharedPrefs.edit {
            putBoolean(KEY_HAS_ASKED_PERMISSION, true)
                .putBoolean(KEY_GRANTED_ONCE, isFirstRequest)
        }

        ActivityCompat.requestPermissions(
            androidActivity,
            REQUIRED_PERMISSIONS,
            PERMISSION_REQUEST_CODE
        )
    }

    fun handlePermissionResult(
        requestCode: Int,
        grantResults: IntArray
    ): Boolean {
        if (requestCode != PERMISSION_REQUEST_CODE) return false

        when {
            grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED } -> {
                val isGrantedOnce = sharedPrefs.getBoolean(KEY_GRANTED_ONCE, false)
                callback.onPermissionResult(
                    if (isGrantedOnce) {
                        PermissionResult.GRANTED_ONCE
                    } else {
                        PermissionResult.GRANTED_PERMANENTLY
                    }
                )
                if (isGrantedOnce) {
                    resetPermissionState()
                }
            }
            grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_DENIED } -> {
                val shouldShowRationale = REQUIRED_PERMISSIONS.any {
                    ActivityCompat.shouldShowRequestPermissionRationale(androidActivity, it)
                }

                callback.onPermissionResult(
                    if (shouldShowRationale) {
                        PermissionResult.DENIED_WITH_RATIONALE
                    } else {
                        PermissionResult.DENIED_PERMANENTLY
                    }
                )
            }
            else -> {
                callback.onPermissionResult(PermissionResult.ERROR)
            }
        }
        return true
    }

    private fun resetPermissionState() {
        sharedPrefs.edit {
            remove(KEY_HAS_ASKED_PERMISSION)
                .remove(KEY_GRANTED_ONCE)
        }
    }

    fun hasAskedPermissionBefore(): Boolean {
        return sharedPrefs.getBoolean(KEY_HAS_ASKED_PERMISSION, false)
    }
}