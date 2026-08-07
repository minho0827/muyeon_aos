package com.muyeon.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit

class PermissionManager(
    private val activity: ComponentActivity,
    private val onAllPermissionsHandled: () -> Unit
) {
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>

    private var onShowExplanationDialog: ((Boolean) -> Unit)? = null

    fun initialize() {
        // Camera launcher
        cameraPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _ ->
            savePermissionRequested(KEY_CAMERA_REQUESTED)
            maybeRequestStoragePermission()
        }

        // Storage launcher
        storagePermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _ ->
            savePermissionRequested(KEY_STORAGE_REQUESTED)
            maybeRequestNotificationPermission()
        }

        // Notification launcher
        notificationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _ ->
            savePermissionRequested(KEY_NOTIF_REQUESTED)
            maybeRequestLocationPermission()
        }

        // Location launcher
        locationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _ ->
            savePermissionRequested(KEY_LOCATION_REQUESTED)
            onAllPermissionsHandled()
        }
    }

    /**
     * Set callback to control showing the explanation dialog from Compose
     */
    fun setExplanationDialogCallback(callback: (Boolean) -> Unit) {
        onShowExplanationDialog = callback
    }

    /**
     * Start the permission flow
     * If the explanation dialog has not been shown before -> show the dialog first
     * If it has been shown -> request permissions directly
     */
    fun startPermissionFlow() {
        val hasShownExplanation = getPrefs().getBoolean(KEY_EXPLANATION_SHOWN, false)

        if (!hasShownExplanation && hasAnyPermissionToRequest()) {
            // Explanation dialog hasn't been shown and there are permissions to request -> show dialog
            onShowExplanationDialog?.invoke(true)
        } else {
            // Explanation dialog already shown or no permissions left to request
            requestPermissionsSequentially()
        }
    }

    /**
     * Called when the user confirms the explanation dialog
     */
    fun onExplanationDialogConfirmed() {
        // Save that the explanation dialog has been shown
        saveExplanationShown()
        // Hide the dialog
        onShowExplanationDialog?.invoke(false)
        // Start requesting system permissions
        requestPermissionsSequentially()
    }

    /**
     * Check whether there is any permission left to request
     */
    private fun hasAnyPermissionToRequest(): Boolean {
        val cameraNeeded = !checkPermission(Manifest.permission.CAMERA) &&
                !getPrefs().getBoolean(KEY_CAMERA_REQUESTED, false)

        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val storageNeeded = !checkPermission(storagePermission) &&
                !getPrefs().getBoolean(KEY_STORAGE_REQUESTED, false)

        val notifNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !checkPermission(Manifest.permission.POST_NOTIFICATIONS) &&
                !getPrefs().getBoolean(KEY_NOTIF_REQUESTED, false)

        val locationNeeded = !checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                !getPrefs().getBoolean(KEY_LOCATION_REQUESTED, false)

        return cameraNeeded || storageNeeded || notifNeeded || locationNeeded
    }

    /**
     * Start requesting permissions in order
     */
    private fun requestPermissionsSequentially() {
        maybeRequestCameraPermission()
    }

    // 1. Camera Permission
    private fun maybeRequestCameraPermission() {
        val alreadyRequested = getPrefs().getBoolean(KEY_CAMERA_REQUESTED, false)
        val hasPermission = checkPermission(Manifest.permission.CAMERA)

        if (hasPermission || alreadyRequested) {
            maybeRequestStoragePermission()
            return
        }
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // 2. Storage/Image Permission
    private fun maybeRequestStoragePermission() {
        val alreadyRequested = getPrefs().getBoolean(KEY_STORAGE_REQUESTED, false)

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val hasPermission = checkPermission(permission)

        if (hasPermission || alreadyRequested) {
            maybeRequestNotificationPermission()
            return
        }
        storagePermissionLauncher.launch(permission)
    }

    // 3. Notification Permission
    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            maybeRequestLocationPermission()
            return
        }

        val alreadyRequested = getPrefs().getBoolean(KEY_NOTIF_REQUESTED, false)
        val hasPermission = checkPermission(Manifest.permission.POST_NOTIFICATIONS)

        if (hasPermission || alreadyRequested) {
            maybeRequestLocationPermission()
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // 4. Location Permission
    private fun maybeRequestLocationPermission() {
        val alreadyRequested = getPrefs().getBoolean(KEY_LOCATION_REQUESTED, false)
        val hasPermission = checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)

        if (hasPermission || alreadyRequested) {
            onAllPermissionsHandled()
            return
        }
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun getPrefs() = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun savePermissionRequested(key: String) {
        getPrefs().edit {
            putBoolean(key, true)
        }
    }

    private fun saveExplanationShown() {
        getPrefs().edit { putBoolean(KEY_EXPLANATION_SHOWN, true) }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val PREFS_NAME = "app_prefs"

        const val KEY_CAMERA_REQUESTED = "camera_requested_once"
        const val KEY_STORAGE_REQUESTED = "storage_requested_once"
        const val KEY_NOTIF_REQUESTED = "notif_requested_once"
        const val KEY_LOCATION_REQUESTED = "location_requested_once"
        const val KEY_EXPLANATION_SHOWN = "explanation_dialog_shown"
    }
}