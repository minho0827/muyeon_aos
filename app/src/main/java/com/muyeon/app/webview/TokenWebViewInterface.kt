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
        // ★ 2026-10-30 로그아웃·강제 로그아웃 — 소켓도 끊는다. 남겨 두면 옛 토큰으로 옛 회원 방(user:id)에 붙어 있다.
        com.muyeon.app.chat.socket.ChatSocketManager.disconnect()

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
                // ★ 2026-11-08 로그인 직후 처리는 앱 로그인 화면과 한 곳(AuthSession) — 강제 로그아웃 다시 받기 · 소켓 새 토큰 ·
                //  서버 활성유형(X-Active-Type) · 못 보낸 구글 구매 · 아래 깔린 앱 로그인 화면 닫기.
                webView.post { com.muyeon.app.auth.AuthSession.onWebLogin(context) }
                // ★ 2026-11-03 로그인 완료 이벤트 — 네이티브 화면에서 [로그인]으로 왔으면 그 화면·동작을 이어서 연다.
                webView.post { (context as? WebViewActivity)?.onLoggedIn() }
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