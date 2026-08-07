package com.muyeon.app.webview

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.GsonBuilder
import com.muyeon.app.data.models.webview.WebViewResponse
import com.muyeon.app.utils.QrPageManager

class QrPageWebViewInterface(private val webView: WebView) {

    private val gson = GsonBuilder().create()

    @JavascriptInterface
    fun getQrPage() {
        android.util.Log.d("QR_DEBUG", "📤 getQrPage() 웹 요청 수신, QrPageManager.value=${QrPageManager.getValue()}")
        val response = if (QrPageManager.hasValue()) {
            val data = mapOf("qrPage" to QrPageManager.getValue()!!)
            WebViewResponse.success(listOf(data))
        } else {
            WebViewResponse.notFound(emptyList<Any>())
        }

        val jsonResponse = gson.toJson(response)
        android.util.Log.d("QR_DEBUG", "📤 getQrPage() 응답=$jsonResponse")
        webView.post {
            webView.evaluateJavascript("window.setQrPage('$jsonResponse')", null)
        }
    }

    @JavascriptInterface
    fun deleteQrPage() {
        android.util.Log.d("QR_DEBUG", "🗑 deleteQrPage() 웹 요청 수신 (qrPage 삭제)")
        QrPageManager.clear()
        val response = WebViewResponse.success(emptyList<Any>())
        val jsonResponse = gson.toJson(response)
        webView.post {
            webView.evaluateJavascript("window.deleteQrPageResponse('$jsonResponse')", null)
        }
    }
}
