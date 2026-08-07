package com.muyeon.app.utils

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

class FcmSender(private val helper: ServiceAccountHelper) {
    private val client = OkHttpClient()

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun sendToTokens(
        title: String,
        body: String,
        dataMap: Map<String, String>,
        tokens: List<String>
    ): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            val accessToken = helper.getAccessToken() ?: return@withContext Pair(false, "Failed to obtain access token")
            val projectId = helper.getProjectId()
            val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

            val sb = StringBuilder()
            var anyOk = false

            // base data (title/body for client to build notification)
            val baseData = HashMap<String, String>()
            baseData["title"] = title
            baseData["body"] = body
            baseData.putAll(dataMap)

            tokens.forEach { token ->
                val message = JSONObject()
                val messageInner = JSONObject()

                messageInner.put("token", token)

                // data-only payload with a unique notification_id
                val dataObj = JSONObject()
                baseData.forEach { (k, v) -> dataObj.put(k, v) }
                val notifId = UUID.randomUUID().toString()
                dataObj.put("notification_id", notifId)
                messageInner.put("data", dataObj)

                // android options
                val androidObj = JSONObject()
                androidObj.put("priority", "HIGH")
                androidObj.put("ttl", "3600s")
                androidObj.put("direct_boot_ok", true)
                messageInner.put("android", androidObj)

                message.put("message", messageInner)

                val payloadStr = message.toString()
                // append payload to sb for debug
                sb.append("payload=").append(payloadStr.take(300)).append("...; ")

                val bodyReq = payloadStr.toRequestBody("application/json; charset=utf-8".toMediaType())
                val req = Request.Builder()
                    .url(url)
                    .post(bodyReq)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json; UTF-8")
                    .build()

                client.newCall(req).execute().use { resp ->
                    val code = resp.code
                    val respBody = resp.body?.string()
                    sb.append("token=").append(token.take(8)).append(" code=").append(code)
                    if (!respBody.isNullOrEmpty()) sb.append(" body=").append(respBody)
                    sb.append("; ")
                    if (code in 200..299) anyOk = true
                }
            }

            Pair(anyOk, sb.toString())
        }
    }
}
