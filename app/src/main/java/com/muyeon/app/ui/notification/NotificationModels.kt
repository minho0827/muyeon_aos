package com.muyeon.app.ui.notification

import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.quote.map
import com.muyeon.app.ui.quote.stringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * 알림 목록 — iOS `NotificationListView.swift` 의 모델·서비스 이식.
 *  커서 페이징 + 안읽음 필터 + 읽음 처리.
 */
data class AppNotification(
    val id: Int,
    val type: String,
    val title: String,
    val body: String?,
    val linkUrl: String?,
    val data: Map<String, String>,
    val isRead: Boolean,
    val createdAt: String,
) {
    val relativeTime: String get() = QuoteUi.relativeTime(createdAt)

    companion object {
        fun from(o: JSONObject): AppNotification {
            val d = o.optJSONObject("data")
            return AppNotification(
                id = o.optInt("id"),
                type = o.optString("type"),
                title = o.optString("title"),
                body = o.stringOrNull("body"),
                linkUrl = o.stringOrNull("linkUrl"),
                data = d?.keys()?.asSequence()?.associateWith { d.optString(it) } ?: emptyMap(),
                isRead = o.optBoolean("isRead", false),
                createdAt = o.optString("createdAt"),
            )
        }
    }
}

class NotificationApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    /** 커서 페이징 — cursor 는 직전 페이지 마지막 id. */
    suspend fun list(cursor: Int?, limit: Int = 20, unreadOnly: Boolean = false): Result<List<AppNotification>> {
        val q = buildList {
            add("limit=$limit")
            cursor?.let { add("cursor=$it") }
            if (unreadOnly) add("unreadOnly=true")
        }
        return call("/notifications?" + q.joinToString("&"))
            .map { JSONArray(it.ifBlank { "[]" }).map(AppNotification::from) }
    }

    /** 응답이 JSON 객체가 아니라 **숫자 하나**다(iOS decode(Int.self)). */
    suspend fun unreadCount(): Int =
        call("/notifications/unread-count").getOrNull()?.trim()?.toIntOrNull() ?: 0

    suspend fun markRead(id: Int): Result<Unit> = call("/notifications/$id/read", "PATCH").map { }

    suspend fun markAllRead(): Result<Unit> = call("/notifications/read-all", "PATCH").map { }

    private suspend fun call(path: String, method: String = "GET"): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = if (method != "GET") "".toRequestBody(JSON) else null
                val req = Request.Builder().url(apiBase + path).method(method, payload)
                    .addHeader("Content-Type", "application/json")
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                client.newCall(req).execute().use { res ->
                    val text = res.body?.string().orEmpty()
                    if (!res.isSuccessful) throw IllegalStateException("요청에 실패했어요.")
                    text
                }
            }
        }

    private companion object { val JSON = "application/json".toMediaType() }
}
