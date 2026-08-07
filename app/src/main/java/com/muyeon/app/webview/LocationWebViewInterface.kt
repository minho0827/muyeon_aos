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
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.data.models.webview.WebViewResponse
import com.muyeon.app.domain.models.location.LocationResult
import com.muyeon.app.domain.repositories.LocationRepository
import com.muyeon.app.utils.PermissionManager
import com.muyeon.app.utils.WebMessageStatus
import com.google.gson.Gson
import com.google.gson.GsonBuilder

data class LocationResultForWeb(
    val latitude: String? = "Unknown",
    val longitude: String? = "Unknown",
    val status: String? = "Reject"
)

class LocationWebViewInterface(
    private val context: Context,
    private val webView: WebView,
    private val locationProvider: LocationRepository,
    private val activity: ComponentActivity,
    private val onShowDialog: (
        title: String,
        content: String?,
        leftText: String?,
        rightText: String,
        buttonCount: Int,
        alignment: ContentAlignment,
        onLeft: () -> Unit,
        onRight: () -> Unit
    ) -> Unit
) {
    private val gson: Gson = GsonBuilder().serializeNulls().create()

    private val prefs by lazy {
        context.getSharedPreferences(PermissionManager.PREFS_NAME, Context.MODE_PRIVATE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @JavascriptInterface
    fun getGpsInfor() {
        if (hasLocationPermissions()) {
            getLocationData()
        } else {
            val everRequested = prefs.getBoolean(PermissionManager.KEY_LOCATION_REQUESTED, false)

            if (everRequested) {
                showPermissionDeniedDialog()
            } else {
                requestLocationPermissions()
            }
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showPermissionDeniedDialog() {
        onShowDialog(
            context.getString(R.string.new_location),
            context.getString(R.string.permission_location_content),
            context.getString(R.string.cancel),
            context.getString(R.string.setting),
            2,
            ContentAlignment.Middle,
            {
                sendPermissionDeniedToWeb()
            },
            {
                openAppSettings()
            }
        )
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            getLocationData()
        } else {
            sendPermissionDeniedToWeb()
        }
    }

    private fun sendPermissionDeniedToWeb() {
        val locationData = LocationResultForWeb(
            latitude = "Unknown",
            longitude = "Unknown",
            status = "Reject"
        )

        val dataAsList = listOf(locationData)

        val webViewResponse: WebViewResponse<List<LocationResultForWeb>> = WebViewResponse(
            data = dataAsList,
            status = WebMessageStatus.FAILED.value,
            message = WebMessageStatus.FAILED.message
        )

        val jsonData = gson.toJson(webViewResponse)
        val jsCode = "window.setGpsInfor('$jsonData')"
        webView.post {
            webView.evaluateJavascript(jsCode, null)
        }
    }

    private fun getLocationData() {
        locationProvider.requestLocation { result ->
            val locationData = LocationResultForWeb(
                latitude = if (result.latitude != null) result.latitude.toString() else "Unknown",
                longitude = if (result.longitude != null) result.longitude.toString() else "Unknown",
                status = result.status
            )

            val dataAsList = listOf(locationData)

            val webViewResponse: WebViewResponse<List<LocationResultForWeb>> = if (result.status == "Forever") {
                WebViewResponse.success(dataAsList)
            } else {
                WebViewResponse.failed(dataAsList)
            }

            val jsonData = gson.toJson(webViewResponse)
            val jsCode = "window.setGpsInfor('$jsonData')"
            webView.post {
                webView.evaluateJavascript(jsCode, null)
            }
        }
    }

    @Suppress("unused")
    fun onLocationResult(result: LocationResult) {
        val locationData = LocationResultForWeb(
            latitude = result.latitude.toString(),
            longitude = result.longitude.toString(),
            status = result.status
        )
        val response = WebViewResponse.success(listOf(locationData))
        val jsonData = gson.toJson(response)
        val jsCode = "window.setGpsInfor('$jsonData')"

        webView.post {
            webView.evaluateJavascript(jsCode, null)
        }
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 101
    }
}