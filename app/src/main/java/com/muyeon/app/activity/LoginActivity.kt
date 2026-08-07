package com.muyeon.app.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class LoginActivity : ComponentActivity() {

    companion object {
        const val EXTRA_IP_ERROR = "EXTRA_IP_ERROR"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)

        val raw = com.muyeon.app.utils.Constants.getBaseUrl(this)
            .trim()
            .trimEnd('/')
        val loginUrl = when {
            raw.startsWith("http://", true) ||
                    raw.startsWith("https://", true) -> raw
            else -> "http://$raw"
        }

        val webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled  = true
                domStorageEnabled  = true
                // 교차 앱 스크립팅(Cross-App Scripting) 방지
                mixedContentMode   = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
            }
            webViewClient = object : android.webkit.WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?
                ): Boolean {
                    // http/https 만 WebView 에서 로드, 그 외 스킴은 외부 앱으로 (OAuth https 는 그대로 동작)
                    val url = request?.url ?: return true
                    val scheme = url.scheme?.lowercase()
                    if (scheme == "http" || scheme == "https") {
                        return false
                    }
                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, url))
                        true
                    } catch (e: Exception) {
                        true
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                    return true
                }
            }
        }

        setContentView(webView)

        lifecycleScope.launch {
            val ok = isUrlReachable(loginUrl)
            if (ok) {
                webView.loadUrl(loginUrl)
            } else {
                showErrorAndReturnConfig()
            }
        }
    }

    private fun showErrorAndReturnConfig() {
        startActivity(
            Intent(this, IpConnectActivity::class.java)
                .putExtra(EXTRA_IP_ERROR, "Invalid IP Address")
        )
        finish()
    }

    private suspend fun isUrlReachable(
        url: String,
        timeoutSec: Long = 2
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(timeoutSec, TimeUnit.SECONDS)
                .readTimeout(timeoutSec, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url(url)
                .head()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error checking URL", e)
            false
        }
    }
}
