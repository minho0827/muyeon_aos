package com.muyeon.app.webview

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.data.models.webview.WebViewResponse
import com.muyeon.app.data.repository.PermissionRepositoryImpl
import com.muyeon.app.domain.use_cases.PermissionUseCase
import com.muyeon.app.utils.Permission
import com.muyeon.app.utils.PermissionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.muyeon.app.R

class PermissionWebViewInterface(
    private val context: Context,
    private val webView: WebView,
    activity: ComponentActivity,
    private val onShowDialog: (
        title: String,
        content: String?,
        leftButtonText: String?,
        rightButtonText: String,
        buttonCount: Int,
        alignment: ContentAlignment,
        onLeftClick: () -> Unit,
        onRightClick: () -> Unit
    ) -> Unit
) {
    private val gson: Gson = GsonBuilder().create()
    private val permissionRepository = PermissionRepositoryImpl()
    private val permissionUseCase = PermissionUseCase(permissionRepository)

    private val prefs = context.getSharedPreferences(PermissionManager.PREFS_NAME, Context.MODE_PRIVATE)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showPermissionSettingsDialog(title: String, content: String) {
        onShowDialog(
            title,
            content,
            context.getString(R.string.cancel),
            context.getString(R.string.setting),
            2,
            ContentAlignment.Middle,
            {},
            {
                openAppSettings()
            }
        )
    }

    private fun sendCameraPermissionStatusToWeb(status: String) {
        val response = WebViewResponse.success(listOf(mapOf("status" to status)))
        val jsonResponse = gson.toJson(response)
        val jsCode = "window.setCameraPermission('$jsonResponse')"
        webView.post {
            webView.evaluateJavascript(jsCode, null)
        }
    }

    private fun sendMediaPermissionStatusToWeb(status: String) {
        val response = WebViewResponse.success(listOf(mapOf("status" to status)))
        val jsonResponse = gson.toJson(response)
        val jsCode = "window.setPhotoLibraryPermission('$jsonResponse')"
        webView.post {
            webView.evaluateJavascript(jsCode, null)
        }
    }

    private val cameraPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            val status = if (isGranted) com.muyeon.app.utils.Constants.AUTHORIZED else com.muyeon.app.utils.Constants.REJECTED
            sendCameraPermissionStatusToWeb(status)
        }

    private val mediaPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            val actualStatus = checkActualMediaPermissionStatus()
            val webStatus = when (actualStatus) {
                "GRANTED" -> com.muyeon.app.utils.Constants.AUTHORIZED
                "LIMITED" -> com.muyeon.app.utils.Constants.LIMITED
                else -> com.muyeon.app.utils.Constants.REJECTED
            }
            sendMediaPermissionStatusToWeb(webStatus)
        }

    private fun checkActualMediaPermissionStatus(): String {
        // 사진 선택은 권한이 필요 없는 시스템 Photo Picker 를 사용하므로 항상 GRANTED 로 보고한다.
        // (Google Play 사진/동영상 권한 정책 준수 - READ_MEDIA_IMAGES 미선언)
        return "GRANTED"
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

    private fun hasPhotoLibraryPermissions(): Boolean {
        val statusString = checkActualMediaPermissionStatus()
        return statusString == "GRANTED" || statusString == "LIMITED"
    }

    private fun getPhotoLibraryPermissionStatus(): String {
        val actualStatus = checkActualMediaPermissionStatus()
        return when (actualStatus) {
            "GRANTED" -> com.muyeon.app.utils.Constants.AUTHORIZED
            "LIMITED" -> com.muyeon.app.utils.Constants.LIMITED
            else -> com.muyeon.app.utils.Constants.REJECTED
        }
    }

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun requestMediaPermission() {
        // Photo Picker 는 권한이 필요 없으므로 권한 요청 없이 즉시 승인 상태를 웹에 통지한다.
        sendMediaPermissionStatusToWeb(com.muyeon.app.utils.Constants.AUTHORIZED)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @JavascriptInterface
    fun getCameraPermission() {
        val currentStatus = permissionUseCase.getCameraPermissionStatus(context)
        val hasPermission = currentStatus == Permission.Authorized || currentStatus == Permission.Limited

        val alreadyRequested = prefs.getBoolean(PermissionManager.KEY_CAMERA_REQUESTED, false)

        if (hasPermission) {
            val statusToSend = if (currentStatus == Permission.Authorized) com.muyeon.app.utils.Constants.AUTHORIZED else com.muyeon.app.utils.Constants.LIMITED
            sendCameraPermissionStatusToWeb(statusToSend)
        } else {
            if (!alreadyRequested) {
                requestCameraPermission()
            } else {
                showPermissionSettingsDialog(
                    context.getString(R.string.new_camera),
                    context.getString(R.string.camera_permission_content)
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @JavascriptInterface
    fun getPhotoLibraryPermission() {
        val hasPermission = hasPhotoLibraryPermissions()

        val alreadyRequested = prefs.getBoolean(PermissionManager.KEY_STORAGE_REQUESTED, false)

        if (hasPermission) {
            val status = getPhotoLibraryPermissionStatus()
            sendMediaPermissionStatusToWeb(status)
        } else {
            if (!alreadyRequested) {
                requestMediaPermission()
            } else {
                showPermissionSettingsDialog(
                    context.getString(R.string.new_gallery),
                    context.getString(R.string.gallery_permission_content)
                )
            }
        }
    }
}