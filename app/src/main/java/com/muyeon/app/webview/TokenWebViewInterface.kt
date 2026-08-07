package com.muyeon.app.webview

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.muyeon.app.data.models.webview.WebViewResponse
import com.muyeon.app.utils.TokenManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

class TokenWebViewInterface(private val context: Context, private val webView: WebView) {

    private val gson: Gson = GsonBuilder().create()

    @JavascriptInterface
    fun getToken() {
        val response: WebViewResponse<out List<Any>>

        val accessToken = TokenManager.getAccessToken(context)
        val refreshToken = TokenManager.getRefreshToken(context)


        val tokenMap: Map<String, String> = mapOf(
            "accessToken" to (accessToken ?: ""), "refreshToken" to (refreshToken ?: "")
        )
        val dataList: List<Map<String, String>> = listOf(tokenMap)

        response = if (accessToken != null && refreshToken != null) {
            WebViewResponse.success(dataList)
        } else {
            WebViewResponse.notAuthenticated(emptyList())
        }

        val jsonResponse = gson.toJson(response)

        val jsCode = "window.setToken('$jsonResponse')"
        webView.post {
            webView.evaluateJavascript(jsCode, null)
        }
    }

    @JavascriptInterface
    fun deleteToken() {
        val response: WebViewResponse<out List<Any>>

        TokenManager.clearTokens(context)

        val accessToken = TokenManager.getAccessToken(context)
        val refreshToken = TokenManager.getRefreshToken(context)

        response = if (accessToken.isNullOrEmpty() && refreshToken.isNullOrEmpty()) {
            WebViewResponse.success(emptyList())
        } else {
            WebViewResponse.failed(emptyList())
        }

        val jsonResponse = gson.toJson(response)
        Log.d("TokenWebViewInterface", "Sending JSON to WebView: $jsonResponse")

        val jsCode = "window.deleteTokenResponse('$jsonResponse')"
        webView.post {
            webView.evaluateJavascript(jsCode, null)
        }
    }

    @JavascriptInterface
    fun saveToken(jsonData: String) {
        try {
            val messageType = object : TypeToken<Map<String, Any>>() {}.type
            val message: Map<String, Any> = Gson().fromJson(jsonData, messageType)

            val rawData = message["data"]
            val data = (rawData as? Map<*, *>)?.mapNotNull {
                val key = it.key as? String
                val value = it.value as? String
                if (key != null && value != null) key to value else null
            }?.toMap() ?: emptyMap()

            val accessToken = data["accessToken"] ?: ""
            val refreshToken = data["refreshToken"] ?: ""

            val response = if (accessToken.isNotEmpty() && refreshToken.isNotEmpty()) {
                TokenManager.saveTokens(context, accessToken, refreshToken)
                WebViewResponse.success(emptyList())
            } else {
                WebViewResponse.serverError(emptyList<Any>())
            }

            val jsonResponse = gson.toJson(response)
            val jsCode = "window.saveTokenResponse('$jsonResponse')"
            webView.post {
                webView.evaluateJavascript(jsCode, null)
            }
        } catch (e: Exception) {
            Log.e("TokenWebViewInterface", "Error parsing JSON for saveToken: ${e.message}", e)
        }
    }

    @Suppress("unused")
    fun sendTokenToWeb(accessToken: String?, refreshToken: String?) {
        val response: WebViewResponse<out List<Any>>

        val tokenMap: Map<String, String> = mapOf(
            "accessToken" to (accessToken ?: ""), "refreshToken" to (refreshToken ?: "")
        )
        val dataList: List<Map<String, String>> = listOf(tokenMap)

        response = if (accessToken != null && refreshToken != null) {
            WebViewResponse.success(dataList)
        } else {
            WebViewResponse.notAuthenticated(emptyList())
        }

        val jsonResponse = gson.toJson(response)

        val jsCode = "window.setToken('$jsonResponse')"
        webView.post {
            webView.evaluateJavascript(jsCode, null)
        }
    }

    @Suppress("unused")
    fun sendDeleteTokenResultToWeb(isSuccessful: Boolean) {
        val response: WebViewResponse<out List<Any>> = if (isSuccessful) {
            WebViewResponse.success(emptyList())
        } else {
            WebViewResponse.failed(emptyList())
        }

        val jsonResponse = gson.toJson(response)
        val jsCode = "window.deleteTokenResponse('$jsonResponse')"

        webView.post {
            webView.evaluateJavascript(jsCode, null)
        }
    }
}