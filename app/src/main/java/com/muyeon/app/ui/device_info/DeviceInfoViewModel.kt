package com.muyeon.app.ui.device_info

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.domain.models.device_info.DeviceInfo
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.core.content.edit

@SuppressLint("StaticFieldLeak")
open class DeviceInfoViewModel(
    application: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val context = application.applicationContext
    private val _deviceInfo = MutableLiveData<DeviceInfo>()
    open val deviceInfo: LiveData<DeviceInfo> = _deviceInfo

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(com.muyeon.app.utils.Constants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    @SuppressLint("HardwareIds")
    open fun getDeviceInfo(onInfoReady: (DeviceInfo) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val packageName = context.packageName
                val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
                val appVersion = packageInfo.versionName ?: "unknown"

                val androidId = Settings.Secure.getString(
                    context.contentResolver, Settings.Secure.ANDROID_ID
                ) ?: "unknown"

                val model = Build.MODEL

                val deviceName = Settings.Global.getString(
                    context.contentResolver, Settings.Global.DEVICE_NAME
                ) ?: model

                val platform = "Android"
                val systemVersion = Build.VERSION.RELEASE

                val deviceToken = getFcmToken()

                // Get permission statuses
                val cameraStatus = getCameraPermissionStatus()
                val galleryStatus = getGalleryPermissionStatus()
                val locationStatus = getLocationPermissionStatus()
                val pushNotificationStatus = getPushNotificationPermissionStatus()

                val info = DeviceInfo(
                    deviceId       = androidId,
                    deviceName     = deviceName,
                    deviceModel    = model,
                    systemName     = platform,
                    systemVersion  = systemVersion,
                    appVersion     = appVersion,
                    deviceToken    = deviceToken,
                    cameraStatus   = cameraStatus,
                    galleryStatus  = galleryStatus,
                    locationStatus = locationStatus,
                    pushNotificationStatus = pushNotificationStatus
                )

                withContext(mainDispatcher) {
                    onInfoReady(info)
                    _deviceInfo.value = info
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    open fun getDeviceInfo() {
        getDeviceInfo {}
    }

    private suspend fun getFcmToken(): String {
        return withContext(ioDispatcher) {
            try {
                val cachedToken = prefs.getString(com.muyeon.app.utils.Constants.KEY_FCM_TOKEN, null)
                if (!cachedToken.isNullOrEmpty()) {
                    return@withContext cachedToken
                }

                val token = FirebaseMessaging.getInstance().token.await()

                prefs.edit { putString(com.muyeon.app.utils.Constants.KEY_FCM_TOKEN, token) }

                return@withContext token
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "unknown"
            }
        }
    }

    private fun getCameraPermissionStatus(): String {
        return when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> com.muyeon.app.utils.Constants.AUTHORIZED
            else -> com.muyeon.app.utils.Constants.REJECTED
        }
    }

    private fun getGalleryPermissionStatus(): String {
        val actualStatus = checkActualMediaPermissionStatus()
        return when (actualStatus) {
            "GRANTED" -> com.muyeon.app.utils.Constants.AUTHORIZED
            "LIMITED" -> com.muyeon.app.utils.Constants.LIMITED
            else -> com.muyeon.app.utils.Constants.REJECTED
        }
    }

    private fun checkActualMediaPermissionStatus(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED -> {
                    "GRANTED"
                }
                hasPartialMediaAccess() -> {
                    "LIMITED"
                }
                else -> {
                    "DENIED"
                }
            }
        } else {
            when (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                PackageManager.PERMISSION_GRANTED -> "GRANTED"
                else -> "DENIED"
            }
        }
    }

    private fun hasPartialMediaAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                ContextCompat.checkSelfPermission(
                    context,
                    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
                ) == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val cursor = context.contentResolver.query(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(android.provider.MediaStore.Images.Media._ID),
                    null, null, null
                )
                val hasAccess = cursor != null && cursor.count > 0
                cursor?.close()
                hasAccess
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    private fun getLocationPermissionStatus(): String {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return when {
            hasFineLocation || hasCoarseLocation -> com.muyeon.app.utils.Constants.AUTHORIZED
            else -> com.muyeon.app.utils.Constants.REJECTED
        }
    }

    private fun getPushNotificationPermissionStatus(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> com.muyeon.app.utils.Constants.AUTHORIZED
                else -> com.muyeon.app.utils.Constants.REJECTED
            }
        } else {
            com.muyeon.app.utils.Constants.AUTHORIZED
        }
    }

    @Suppress("unused")
    open fun refreshFcmToken(onTokenRefreshed: (String) -> Unit = {}) {
        viewModelScope.launch(ioDispatcher) {
            try {
                FirebaseMessaging.getInstance().deleteToken().await()

                val newToken = FirebaseMessaging.getInstance().token.await()

                prefs.edit { putString(com.muyeon.app.utils.Constants.KEY_FCM_TOKEN, newToken) }

                withContext(mainDispatcher) {
                    _deviceInfo.value?.let { currentInfo ->
                        _deviceInfo.value = currentInfo.copy(deviceToken = newToken)
                    }
                    onTokenRefreshed(newToken)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}