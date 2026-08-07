package com.muyeon.app.webview

import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.RequiresApi
import com.muyeon.app.data.models.webview.WebViewResponse
import com.muyeon.app.ui.device_info.DeviceInfoViewModel
import com.muyeon.app.domain.models.device_info.DeviceInfo
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class DeviceInfoWebViewInterface(
    private val webView: WebView,
    private val deviceInfoViewModel: DeviceInfoViewModel
) {
    private val tag = "DeviceInfoBridge"
    private val gson: Gson = GsonBuilder().create()

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    @JavascriptInterface
    fun getDeviceInfo() {
        deviceInfoViewModel.getDeviceInfo { deviceInfo ->
            Log.d(tag, "📱 DeviceInfo 전달 - FCM Token: ${deviceInfo.deviceToken}")

            val response: WebViewResponse<List<DeviceInfo>> =
                WebViewResponse.success(listOf(deviceInfo))

            val jsonData = gson.toJson(response)
            val jsCode = "window.setDeviceInfo('$jsonData')"

            webView.post {
                webView.evaluateJavascript(jsCode, null)
            }
        }
    }

    @Suppress("unused")
    fun sendDeviceInfoToWeb(deviceInfo: DeviceInfo) {
        val response: WebViewResponse<List<DeviceInfo>> =
            WebViewResponse.success(listOf(deviceInfo))

        val jsonData = gson.toJson(response)
        val jsCode = "window.setDeviceInfo('$jsonData')"

        webView.post {
            webView.evaluateJavascript(jsCode, null)
        }
    }
}