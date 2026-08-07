package com.muyeon.app.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.muyeon.app.data.models.webview.WebViewResponse
import com.muyeon.app.activity.QrScannerActivity
import com.muyeon.app.data.models.qr.QrResponse
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlin.jvm.java

class ScanQRWebViewInterface(private val context: Context, private val webView: WebView) {
    private val gson: Gson = GsonBuilder().create()

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: ScanQRWebViewInterface? = null

        fun getInstance(): ScanQRWebViewInterface? = instance

        fun setInstance(scanInterface: ScanQRWebViewInterface?) {
            instance = scanInterface
        }
    }

    @JavascriptInterface
    fun getScanQR() {
        setInstance(this)
        startQrScannerActivity()
    }

    private fun startQrScannerActivity() {
        val intent = Intent(context, QrScannerActivity::class.java)
        context.startActivity(intent)
    }

    fun sendQRResultToWeb(data: QrResponse?, success: Int) {
        val response: WebViewResponse<out List<Any>>

        val dataList = listOf(
            mapOf(
                "rsvSeq" to data?.rsvSeq,
                "mbr_seq" to data?.mbrSeq,
                "cf_fd_seq" to data?.cfFdSeq,
                "rsv_result_cd" to data?.rsvResultCd,
                "result_msg" to data?.resultMsg,
                "result_cd" to data?.resultCd
            )
        )

        response = when (success) {
            200 -> {
                WebViewResponse.success(dataList)
            }
            404 -> {
                WebViewResponse.notFound(dataList)
            }
            else -> {
                WebViewResponse.notAuthenticated(dataList)
            }
        }

        val jsonResponse = gson.toJson(response)
        val jsCode = "window.setScanQR('$jsonResponse')"

        if (webView.context is Activity && (webView.context as Activity).isFinishing) {
            return
        }

        webView.post {
            webView.evaluateJavascript(jsCode, null)
        }
    }
}
