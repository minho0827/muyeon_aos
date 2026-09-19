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

/** 알림 설정 1줄. locked = 끌 수 없는 것(인증·소속·공지처럼 놓치면 곤란한 알림). */
data class NotificationCategoryPref(
    val key: String,
    val label: String,
    val desc: String,
    val locked: Boolean,
    val enabled: Boolean,
)

data class NotificationPrefs(
    val pushEnabled: Boolean,
    val categories: List<NotificationCategoryPref>,
) {
    companion object {
        fun from(o: JSONObject): NotificationPrefs = NotificationPrefs(
            pushEnabled = o.optBoolean("pushEnabled", true),
            categories = o.optJSONArray("categories").let { arr ->
                (0 until (arr?.length() ?: 0)).map { i ->
                    val c = arr!!.getJSONObject(i)
                    NotificationCategoryPref(
                        key = c.optString("key"),
                        label = c.optString("label"),
                        desc = c.optString("desc"),
                        locked = c.optBoolean("locked", false),
                        enabled = c.optBoolean("enabled", true),
                    )
                }
            },
        )
    }
}

class NotificationApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    /** 커서 페이징 — cursor 는 직전 페이지 마지막 id.
     *  category 는 서버가 거른다(클라에서 거르면 받아 온 페이지 안에서만 걸러져
     *  무한스크롤과 만나 목록이 빈 것처럼 보인다). */
    suspend fun list(
        cursor: Int?,
        limit: Int = 20,
        unreadOnly: Boolean = false,
        category: String? = null,
    ): Result<List<AppNotification>> {
        val q = buildList {
            add("limit=$limit")
            cursor?.let { add("cursor=$it") }
            if (unreadOnly) add("unreadOnly=true")
            if (!category.isNullOrEmpty()) add("category=$category")
        }
        return call("/notifications?" + q.joinToString("&"))
            .map { JSONArray(it.ifBlank { "[]" }).map(AppNotification::from) }
    }

    /** 응답이 JSON 객체가 아니라 **숫자 하나**다(iOS decode(Int.self)). */
    suspend fun unreadCount(): Int =
        call("/notifications/unread-count").getOrNull()?.trim()?.toIntOrNull() ?: 0

    suspend fun markRead(id: Int): Result<Unit> = call("/notifications/$id/read", "PATCH").map { }

    suspend fun markAllRead(): Result<Unit> = call("/notifications/read-all", "PATCH").map { }

    // ── 알림(푸시) 설정 — GET/PATCH /me/notification-prefs ──
    //  ⚠️ 관심조건 알림(me/alert-prefs)과 다른 것이다. 저쪽은 "어떤 공고를 받을지",
    //   이쪽은 "어떤 알림을 푸시로 받을지". 카테고리 라벨·설명은 서버가 함께 내려준다 —
    //   앱이 문구를 들고 있으면 카테고리가 늘 때마다 스토어 심사를 다시 받아야 한다.
    suspend fun prefs(): Result<NotificationPrefs> =
        call("/me/notification-prefs").map { NotificationPrefs.from(JSONObject(it)) }

    /** 부분 저장 — 바꾼 것만 보낸다(서버가 나머지를 유지). */
    suspend fun updatePrefs(
        pushEnabled: Boolean? = null,
        category: Pair<String, Boolean>? = null,
    ): Result<NotificationPrefs> {
        val body = JSONObject()
        pushEnabled?.let { body.put("pushEnabled", it) }
        category?.let { (key, on) -> body.put("categories", JSONObject().put(key, on)) }
        return call("/me/notification-prefs", "PATCH", body.toString())
            .map { NotificationPrefs.from(JSONObject(it)) }
    }

    private suspend fun call(
        path: String,
        method: String = "GET",
        body: String? = null,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = if (method != "GET") (body ?: "").toRequestBody(JSON) else null
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
