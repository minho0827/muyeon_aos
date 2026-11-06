package com.muyeon.app.ui.quote

import com.muyeon.app.BuildConfig
import com.muyeon.app.webview.withActiveType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** 저장된 '메시지+금액' 묶음 하나. */
data class AutoQuoteTemplate(val id: Int, val title: String?, val message: String, val price: String?)

/** ★ 2026-11-06 지금 안 나가는 이유 하나 — 서버가 고치는 순서대로 준다. */
data class AutoQuoteBlocker(val code: String, val title: String, val action: Action?) {
    data class Action(val label: String, val nativeAction: String?, val webPath: String?)
}

/** ★ 2026-11-06 GET /status — 스위치·실제 발송 여부·이번 달 한도·막힌 이유를 한 번에. */
data class AutoQuoteStatus(
    val switchOn: Boolean,
    val activeId: Int?,
    val activeTitle: String?,
    val templateCount: Int,
    val sending: Boolean,
    val quota: Quota?,
    val blockers: List<AutoQuoteBlocker>,
) {
    /** limit < 0 = 무제한. */
    data class Quota(val limit: Int, val used: Int, val remaining: Int?)
}

/** 스위치 저장 결과 — 메시지가 없어서 못 켠 경우를 따로 안다(400 NO_TEMPLATE). */
enum class AutoQuoteSwitchResult { OK, NO_TEMPLATE, FAILED }

/** /api/chat/my/auto-quote — iOS AutoQuoteTemplatesViewModel 의 엔드포인트와 동일. */
class AutoQuoteApi(private val token: String?) {
    private val client = OkHttpClient()
    private val base = BuildConfig.API_BASE_URL + "/api/chat/my/auto-quote"

    /** 응답 코드와 본문. 네트워크 오류면 null. */
    private suspend fun call(path: String, method: String, body: JSONObject? = null): Pair<Int, String>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = when {
                    body != null -> body.toString().toRequestBody(JSON)
                    method != "GET" && method != "DELETE" -> "".toRequestBody(JSON)
                    else -> null
                }
                val req = Request.Builder().url(base + path).method(method, payload)
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }.withActiveType()
                    .apply { if (body != null) addHeader("Content-Type", "application/json") }
                    .build()
                client.newCall(req).execute().use { res -> res.code to res.body?.string().orEmpty() }
            }.getOrNull()
        }

    /** 성공(2xx)일 때만 본문. */
    private suspend fun fire(path: String, method: String, body: JSONObject? = null): String? =
        call(path, method, body)?.takeIf { it.first in 200..299 }?.second

    /** ★ 2026-11-06 상태 한 번에. 실패면 null(빈 화면이 아니라 오류 화면). */
    suspend fun status(): AutoQuoteStatus? = fire("/status", "GET")?.let { text ->
        runCatching {
            val o = JSONObject(text)
            val active = o.optJSONObject("activeTemplate")
            val q = o.optJSONObject("quota")
            AutoQuoteStatus(
                switchOn = o.optBoolean("switchOn"),
                activeId = active?.intOrNull("id"),
                activeTitle = active?.stringOrNull("title"),
                templateCount = o.optInt("templateCount"),
                sending = o.optBoolean("sending"),
                quota = q?.let { AutoQuoteStatus.Quota(it.optInt("limit"), it.optInt("used"), it.intOrNull("remaining")) },
                blockers = o.optJSONArray("blockers")?.map { b ->
                    AutoQuoteBlocker(
                        code = b.optString("code"),
                        title = b.optString("title"),
                        action = b.optJSONObject("action")?.let { a ->
                            AutoQuoteBlocker.Action(a.optString("label"), a.stringOrNull("nativeAction"), a.stringOrNull("webPath"))
                        },
                    )
                } ?: emptyList(),
            )
        }.getOrNull()
    }

    /** ★ 2026-11-06 실패면 null — 빈 목록과 구분한다. */
    suspend fun templates(): List<AutoQuoteTemplate>? =
        fire("/templates", "GET")?.let { text ->
            runCatching {
                JSONArray(text).map { o ->
                    AutoQuoteTemplate(o.optInt("id"), o.stringOrNull("title"), o.optString("message"), o.stringOrNull("price"))
                }
            }.getOrNull()
        }

    /** ★ 2026-11-06 큰 스위치 — 켜기만/끄기만. 선택이 없으면 서버가 첫 메시지를 고른다. */
    suspend fun setEnabled(on: Boolean): AutoQuoteSwitchResult {
        val (code, text) = call("", "PUT", JSONObject().put("autoQuoteEnabled", on)) ?: return AutoQuoteSwitchResult.FAILED
        if (code in 200..299) return AutoQuoteSwitchResult.OK
        val noTemplate = code == 400 && runCatching { JSONObject(text).optString("code") == "NO_TEMPLATE" }.getOrDefault(false)
        return if (noTemplate) AutoQuoteSwitchResult.NO_TEMPLATE else AutoQuoteSwitchResult.FAILED
    }

    /** 고르기만 한다(★ 2026-11-06 스위치는 건드리지 않는다). */
    suspend fun activate(id: Int): Boolean = fire("/templates/$id/activate", "POST") != null

    /** 사용 중인 걸 지우면 서버가 다음 것을 고르거나(스위치 유지), 남은 게 없으면 끈다. */
    suspend fun remove(id: Int): Boolean = fire("/templates/$id", "DELETE") != null

    suspend fun save(id: Int?, title: String, message: String, price: String): Boolean {
        val body = JSONObject().put("title", title).put("message", message).put("price", price)
        return if (id != null) fire("/templates/$id", "PUT", body) != null
        else fire("/templates", "POST", body) != null
    }

    private companion object { val JSON = "application/json".toMediaType() }
}
