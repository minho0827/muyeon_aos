package com.muyeon.app.webview

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.RequiresApi
import com.muyeon.app.data.models.webview.WebViewResponse
import com.muyeon.app.utils.FcmSender
import com.muyeon.app.utils.ServiceAccountHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class PushNotificationInterface(private val webView: WebView) {
    private val uiHandler = Handler(Looper.getMainLooper())
    private val helper = ServiceAccountHelper()
    private val sender = FcmSender(helper)

    @RequiresApi(Build.VERSION_CODES.O)
    @JavascriptInterface
    fun getPushNotification(payloadJson: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val root = JSONObject(payloadJson)
                val dataObject = if (root.has("action") && root.has("data")) {
                    root.getJSONObject("data")
                } else {
                    root
                }

                val title = dataObject.optString("title", "")
                val body = dataObject.optString("body", "")
                val notificationType = dataObject.optString("notification_type", "")
                val notificationUrl = dataObject.optString("notification_url", "")

                val customObj = if (dataObject.has("custom_data")) {
                    dataObject.optJSONObject("custom_data")
                } else {
                    null
                }

                // ---- Parse incoming tokens into a mutable list ----
                val tokensList = mutableListOf<String>()
                if (dataObject.has("tokens")) {
                    when (val raw = dataObject.get("tokens")) {
                        is JSONArray -> {
                            for (i in 0 until raw.length()) {
                                val t = raw.optString(i, "").trim()
                                if (t.isNotEmpty()) tokensList.add(t)
                            }
                        }
                        is String -> {
                            raw.trim().takeIf { it.isNotEmpty() }?.let { tokensList.add(it) }
                        }
                        else -> {
                            val s = raw.toString().trim()
                            if (s.isNotEmpty()) tokensList.add(s)
                        }
                    }
                }

                val prefs = webView.context.getSharedPreferences(com.muyeon.app.utils.Constants.PREFS_NAME, 0)
                val currentToken = prefs.getString(com.muyeon.app.utils.Constants.KEY_FCM_TOKEN, null)?.trim()

                if (!currentToken.isNullOrEmpty()) {
                    val contains = tokensList.any { it == currentToken }
                    if (!contains) {
                        tokensList.add(currentToken)
                    }
                }

                // Deduplicate while preserving order
                val dedupedTokens = LinkedHashSet<String>()
                tokensList.forEach { if (it.isNotEmpty()) dedupedTokens.add(it) }

                val finalTokens = dedupedTokens.toList()

                if (finalTokens.isEmpty()) {
                    runOnUi { sendResponseToWeb(WebViewResponse.failed(emptyList<String>())) }
                    return@launch
                }

                // Prepare data map
                val dataMap = mutableMapOf<String, String>()
                if (notificationType.isNotEmpty()) dataMap["notification_type"] = notificationType
                if (notificationUrl.isNotEmpty()) dataMap["notification_url"] = notificationUrl

                if (customObj != null) {
                    val keys = customObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val v = customObj.get(k)
                        dataMap[k] = v.toString()
                    }
                }

                val (success) = sender.sendToTokens(title, body, dataMap, finalTokens)

                if (success) {
                    runOnUi { sendResponseToWeb(WebViewResponse.success(emptyList<String>())) }
                } else {
                    runOnUi { sendResponseToWeb(WebViewResponse.failed(emptyList<String>())) }
                }
            } catch (e: Exception) {
                Log.d("PushNotificationInterface", e.toString())
                runOnUi { sendResponseToWeb(WebViewResponse.failed(emptyList<String>())) }
            }
        }
    }

    private fun runOnUi(action: () -> Unit) {
        uiHandler.post { action() }
    }

    /**
     * Convert WebViewResponse to JSON and call window.setPushNotification(responseData) in the page.
     * The page should implement window.setPushNotification to receive the object.
     */
    private fun sendResponseToWeb(response: WebViewResponse<*>) {
        val json = JSONObject().apply {
            put("status", response.status)
            put("message", response.message)
            val dataObj = when (val d = response.data) {
                is Collection<*> -> JSONArray(d)
                is JSONArray -> d
                else -> if (d == null) JSONArray() else JSONArray().put(d)
            }
            put("data", dataObj)
        }

        val jsonStr = json.toString()

        val js = "try{ if(typeof window.setPushNotification === 'function') window.setPushNotification($jsonStr); }catch(e){}"

        webView.post {
            webView.evaluateJavascript(js, null)
        }
    }

}
