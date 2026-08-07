package com.muyeon.app.webview

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.data.models.webview.WebViewResponse
import com.muyeon.app.ui.notification.NotificationViewModel
import com.muyeon.app.utils.PermissionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class NotificationWebViewInterface(
    private val context: Context,
    private val webView: WebView,
    private val notificationViewModel: NotificationViewModel,
    private val permissionLauncher: ActivityResultLauncher<String>,
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

    private val prefs = context.getSharedPreferences(PermissionManager.PREFS_NAME, Context.MODE_PRIVATE)

    init {
        val lifecycleScope = webView.findViewTreeLifecycleOwner()?.lifecycleScope
        lifecycleScope?.let { scope ->
            notificationViewModel.isPermissionGranted
                .onEach { isGranted ->
                    if (isGranted != null) {
                        val response: WebViewResponse<out List<Any>> = if (isGranted) {
                            WebViewResponse.success(listOf(mapOf("status" to com.muyeon.app.utils.Constants.AUTHORIZED)))
                        } else {
                            WebViewResponse.failed(listOf(mapOf("status" to com.muyeon.app.utils.Constants.REJECTED)))
                        }
                        val jsonData = gson.toJson(response)
                        Log.d("NotificationWebViewInterface", "Sending JSON: $jsonData")
                        val jsCode = "window.setNotificationPermission('$jsonData')"
                        webView.post {
                            webView.evaluateJavascript(jsCode, null)
                        }
                    }
                }
                .launchIn(scope)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @JavascriptInterface
    fun getNotificationPermission() {
        requestNotificationPermission()
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        val response = if (hasPermission) {
            WebViewResponse.success(listOf(mapOf("status" to com.muyeon.app.utils.Constants.AUTHORIZED)))
        } else {
            WebViewResponse.failed(listOf(mapOf("status" to com.muyeon.app.utils.Constants.REJECTED)))
        }
        val jsonData = gson.toJson(response)
        webView.post {
            webView.evaluateJavascript("window.setNotificationPermission('$jsonData')", null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestNotificationPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        val alreadyRequested = prefs.getBoolean(PermissionManager.KEY_NOTIF_REQUESTED, false)

        if (hasPermission) {
            showSettingsDialog()
        } else {
            if (!alreadyRequested) {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    context as ComponentActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                )

                notificationViewModel.requestNotificationPermission(
                    false,
                    permissionLauncher
                )
            } else {
                showSettingsDialog()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showSettingsDialog() {
        onShowDialog(
            context.getString(R.string.new_notification),
            context.getString(R.string.permission_notification_content),
            context.getString(R.string.cancel),
            context.getString(R.string.setting),
            2,
            ContentAlignment.Middle,
            {},
            {
                openNotificationSettings()
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }
}