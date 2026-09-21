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

/**
 * 알림 종류 칩 1개 — 서버가 내 활동유형 기준으로 골라 내려준다.
 *
 * ⚠️ 카테고리 목록·라벨·순서를 앱에 두지 말 것. 표는 서버(common/notification-category.ts)에
 *    하나뿐이고, 알림 설정 화면과 같은 표를 쓴다. 앱이 표를 들면 알림 타입이 하나 늘 때마다
 *    iOS·AOS·웹이 서로 다른 말을 한다.
 */
data class NotiCategory(
    val key: String,
    val label: String,
    val total: Int,
    val unread: Int,
    val muted: Boolean, // 이 종류 푸시를 꺼 둔 상태(알림함에는 계속 쌓인다)
) {
    companion object {
        const val ALL = "ALL" // '전체' 칩 — 서버에 category 를 안 보내는 값

        fun from(o: JSONObject) = NotiCategory(
            key = o.optString("key"),
            label = o.optString("label"),
            total = o.optInt("total"),
            unread = o.optInt("unread"),
            muted = o.optBoolean("muted", false),
        )
    }
}

/**
 * 알림 수신 설정의 한 종류. 서버가 목록·라벨·설명·잠금/동의 여부를 준다.
 *
 * ⚠️ 종류 표를 앱에 만들지 말 것. 알림함 칩과 **같은 표**이고 원본은 백엔드 코드다
 *    (common/notification-category.ts). 앱이 표를 들면 칩·웹 설정과 즉시 어긋난다.
 */
data class NotiPrefCategory(
    val key: String,
    val label: String,
    val desc: String,
    val locked: Boolean,   // 끌 수 없는 종류(지원 결과·계정 등)
    val optIn: Boolean,    // 켜야 받는 종류(광고성) — 사전 동의가 필요하다
    val enabled: Boolean,
) {
    companion object {
        fun from(o: JSONObject) = NotiPrefCategory(
            key = o.optString("key"),
            label = o.optString("label"),
            desc = o.optString("desc"),
            locked = o.optBoolean("locked", false),
            optIn = o.optBoolean("optIn", false),
            enabled = o.optBoolean("enabled", true),
        )
    }
}

/** GET/PATCH /me/notification-prefs 응답. */
data class NotiPrefs(val pushEnabled: Boolean, val categories: List<NotiPrefCategory>) {
    companion object {
        fun from(o: JSONObject) = NotiPrefs(
            pushEnabled = o.optBoolean("pushEnabled", true),
            categories = o.optJSONArray("categories")?.map(NotiPrefCategory::from) ?: emptyList(),
        )
    }
}

class NotificationApi(private val token: String?, private val activeType: String = "GENERAL") {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    /** 커서 페이징 — cursor 는 직전 페이지 마지막 id. */
    suspend fun list(
        cursor: Int?,
        limit: Int = 20,
        unreadOnly: Boolean = false,
        category: String = NotiCategory.ALL,
    ): Result<List<AppNotification>> {
        val q = buildList {
            add("limit=$limit")
            cursor?.let { add("cursor=$it") }
            if (unreadOnly) add("unreadOnly=true")
            categoryParam(category)?.let { add("category=$it") }
        }
        return call("/notifications?" + q.joinToString("&"))
            .map { JSONArray(it.ifBlank { "[]" }).map(AppNotification::from) }
    }

    /** 종류 칩. 실패해도 화면을 막지 않는다 — 칩 없이 목록만 그린다. */
    suspend fun categories(): List<NotiCategory> {
        val body = call("/notifications/categories").getOrNull() ?: return emptyList()
        return runCatching {
            JSONObject(body).optJSONArray("items")?.map(NotiCategory::from) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    /** "ALL"(전체)은 파라미터를 안 보낸다 — 서버가 모르는 값으로 받아 빈 목록을 주지 않게. */
    private fun categoryParam(category: String): String? =
        category.takeIf { it.isNotBlank() && it != NotiCategory.ALL }

    /** 응답이 JSON 객체가 아니라 **숫자 하나**다(iOS decode(Int.self)). */
    suspend fun unreadCount(): Int =
        call("/notifications/unread-count").getOrNull()?.trim()?.toIntOrNull() ?: 0

    suspend fun markRead(id: Int): Result<Unit> = call("/notifications/$id/read", "PATCH").map { }

    /**
     * 모두 읽음. 종류 칩이 걸려 있으면 그 종류만 읽는다.
     *  화면에 '대타'만 띄운 채 전부가 읽히면 보지도 못한 알림을 잃는다(읽음은 개별 취소가 없다).
     */
    suspend fun markAllRead(category: String = NotiCategory.ALL): Result<Unit> {
        val q = categoryParam(category)?.let { "?category=$it" } ?: ""
        return call("/notifications/read-all$q", "PATCH").map { }
    }

    /** 알림 수신 설정 조회. */
    suspend fun prefs(): Result<NotiPrefs> =
        call("/me/notification-prefs").mapCatching { NotiPrefs.from(JSONObject(it)) }

    /** 부분 저장 — 보낸 항목만 바뀐다(전체 스위치 / 종류 하나). */
    suspend fun updatePrefs(body: JSONObject): Result<NotiPrefs> =
        call("/me/notification-prefs", "PATCH", body.toString())
            .mapCatching { NotiPrefs.from(JSONObject(it)) }

    private suspend fun call(path: String, method: String = "GET", json: String? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = if (method != "GET") (json ?: "").toRequestBody(JSON) else null
                val req = Request.Builder().url(apiBase + path).method(method, payload)
                    .addHeader("Content-Type", "application/json")
                    // 활동유형 — 서버가 이 헤더로 칩 구성을 정한다(iOS AppNetwork 와 같은 계약).
                    .addHeader("X-Active-Type", activeType)
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
