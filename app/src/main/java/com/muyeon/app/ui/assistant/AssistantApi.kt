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

    /** 응답 코드 + JSON(오류 본문 포함) — AI 작성 흐름은 403 AI_PRO_REQUIRED 같은 코드를 화면에서 구분한다. */
    data class Res(val status: Int, val json: JSONObject?) {
        val ok get() = status in 200..299
        val code get() = json?.optString("code")?.takeIf { it.isNotEmpty() } ?: json?.optJSONObject("data")?.optString("code")
        val message get() = json?.optString("message")?.takeIf { it.isNotEmpty() && it != "null" }
    }

    suspend fun call(context: Context, method: String, path: String, body: JSONObject? = null): Res = withContext(Dispatchers.IO) {
        val b = builder(context, path) ?: return@withContext Res(401, null)
        val rb = (body ?: JSONObject()).toString().toRequestBody("application/json".toMediaType())
        val req = when (method) { "POST" -> b.post(rb); else -> b.get() }.build()
        runCatching {
            client.newCall(req).execute().use { res ->
                val txt = res.body?.string().orEmpty()
                Res(res.code, runCatching { JSONObject(txt) }.getOrNull())
            }
        }.getOrElse { Res(0, null) }
    }

    suspend fun catalog(context: Context): AssistantCatalog? = get(context, "/assistant/catalog")?.let(AssistantCatalog::from)
    suspend fun config(context: Context): AssistantConfig? = get(context, "/assistant/config")?.let(AssistantConfig::from)

    /** 자주 묻는 질문(공개 API, 토큰 불필요). */
    suspend fun faqs(): List<AssistantFaq>? = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url("$apiBase/faqs").build()).execute().use { res ->
                if (!res.isSuccessful) return@use null
                val arr = JSONArray(res.body?.string() ?: "[]")
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    AssistantFaq(o.optInt("id"), o.optString("category"), o.optString("question"), o.optString("answer").takeIf { it != "null" } ?: "")
                }
            }
        }.getOrNull()
    }

    /** 뱃지 숫자 { notifications, chat, quotes } (total 은 합계로 앱이 계산). */
    suspend fun badges(context: Context): Map<String, Int>? = get(context, "/assistant/badges")?.let { o ->
        mapOf("notifications" to o.optInt("notifications"), "chat" to o.optInt("chat"), "quotes" to o.optInt("quotes"))
    }

    /** 인기 검색어 — (활성 유형, 10개). */
    suspend fun popular(context: Context): Pair<String, List<String>>? = get(context, "/assistant/popular")?.let { o ->
        val arr = o.optJSONArray("items")
        o.optString("activeType") to (0 until (arr?.length() ?: 0)).map { arr!!.optString(it) }
    }

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
