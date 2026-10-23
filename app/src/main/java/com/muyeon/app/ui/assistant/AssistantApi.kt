package com.muyeon.app.ui.assistant

import android.content.Context
import com.muyeon.app.BuildConfig
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.withActiveType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** AI 도우미 서버 호출 — 실패하면 null(창 안에서 안내). 활성 유형 헤더(withActiveType)를 붙인다. */
object AssistantApi {
    private val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
    private val apiBase get() = BuildConfig.API_BASE_URL + "/api"

    private fun builder(context: Context, path: String): Request.Builder? {
        val token = TokenManager.getAccessToken(context)
        if (token.isNullOrEmpty()) return null
        return Request.Builder().url(apiBase + path).addHeader("Authorization", "Bearer $token").withActiveType()
    }

    private suspend fun get(context: Context, path: String): JSONObject? = withContext(Dispatchers.IO) {
        val req = builder(context, path)?.build() ?: return@withContext null
        runCatching {
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use null
                res.body?.string()?.takeIf { it.isNotBlank() }?.let { JSONObject(it) }
            }
        }.getOrNull()
    }

    suspend fun catalog(context: Context): AssistantCatalog? = get(context, "/assistant/catalog")?.let(AssistantCatalog::from)
    suspend fun config(context: Context): AssistantConfig? = get(context, "/assistant/config")?.let(AssistantConfig::from)

    /** 검색 기록 묶음 — 실패해도 조용히. */
    fun logSearches(context: Context, rows: List<Pair<String, String?>>) {
        if (rows.isEmpty()) return
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val arr = JSONArray()
            rows.take(20).forEach { (q, id) -> arr.put(JSONObject().put("query", q).apply { if (id != null) put("clickedItemId", id) }) }
            val body = JSONObject().put("rows", arr).put("platform", "AOS").toString().toRequestBody("application/json".toMediaType())
            val req = builder(app, "/assistant/search-logs")?.post(body)?.build() ?: return@launch
            runCatching { client.newCall(req).execute().close() }
        }
    }
}
