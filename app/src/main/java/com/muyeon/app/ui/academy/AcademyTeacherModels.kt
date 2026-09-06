package com.muyeon.app.ui.academy

import com.muyeon.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * 학원 ↔ 강사 소속 모델 + REST — iOS `AcademyTeacherModels.swift` 1:1.
 *  서버: /academy-teachers (code, code/reissue, join, mine, invites, :id/approve|deny|cancel|leave)
 *
 * ⚠️ invite/accept/reject 는 백엔드 admin 전용이라 앱에서 쓰지 않는다.
 *   앱 흐름은 "학원 코드 → 강사가 신청 → 학원이 승인" 한 가지뿐(iOS 주석과 동일).
 */
data class AcademyTeacher(
    val id: Int,
    val status: String?,        // REQUESTED | INVITED | ACTIVE | DENIED | LEFT
    val via: String?,
    val message: String?,
    val userId: Int?,
    val userName: String?,
    val userImage: String?,
    val genres: List<String>?,
) {
    val displayName: String get() = userName ?: "이름 없음"
    val genreLine: String get() = (genres ?: emptyList()).joinToString(" · ")

    companion object {
        fun from(o: JSONObject): AcademyTeacher {
            val u = o.optJSONObject("user")
            val g = u?.optJSONArray("genres")
            return AcademyTeacher(
                id = o.optInt("id"),
                status = o.optString("status").ifEmpty { null },
                via = o.optString("via").ifEmpty { null },
                message = o.optString("message").ifEmpty { null },
                userId = u?.optInt("id"),
                userName = u?.optString("name")?.ifEmpty { null },
                userImage = u?.optString("image")?.ifEmpty { null },
                genres = g?.let { arr -> (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotEmpty() } },
            )
        }
    }
}

object AcademyTeacherStatus {
    fun label(s: String?): String = when (s) {
        "REQUESTED" -> "신청"
        "INVITED" -> "강사 수락 대기"
        "ACTIVE" -> "소속"
        "DENIED" -> "거절됨"
        "LEFT" -> "해지됨"
        else -> ""
    }

    /** 대기(승인 필요) / 소속 분리 — 웹 AcademyTeachers·AcademyInvites 와 동일 기준. */
    fun isWaiting(s: String?) = s == "REQUESTED" || s == "INVITED"
    fun isActive(s: String?) = s == "ACTIVE"
}

class AcademyTeacherApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"
    private val json = "application/json; charset=utf-8".toMediaType()

    suspend fun myCode(): Result<String> = call("/academy-teachers/code").map { it.asObject().optString("code") }

    /** 코드 재발급 — 이전 코드는 즉시 무효, 기존 소속은 유지된다. */
    suspend fun reissueCode(): Result<String> =
        call("/academy-teachers/code/reissue", "POST").map { it.asObject().optString("code") }

    suspend fun mine(): Result<List<AcademyTeacher>> = list("/academy-teachers/mine")

    suspend fun invites(): Result<List<AcademyTeacher>> = list("/academy-teachers/invites")

    /** 학원 코드로 소속 신청. 신청 단계에선 아무 권한도 생기지 않는다(승인 필요). */
    suspend fun joinByCode(code: String): Result<Unit> =
        call("/academy-teachers/join", "POST", JSONObject().put("code", code)).map { }

    suspend fun approve(id: Int): Result<Unit> = call("/academy-teachers/$id/approve", "POST").map { }

    suspend fun deny(id: Int): Result<Unit> = call("/academy-teachers/$id/deny", "POST").map { }

    suspend fun cancel(id: Int): Result<Unit> = call("/academy-teachers/$id/cancel", "POST").map { }

    /** 소속 해지 — 학원·강사 모두 가능. 기존 레슨은 유지된다. */
    suspend fun leave(id: Int): Result<Unit> = call("/academy-teachers/$id/leave", "POST").map { }

    private suspend fun list(path: String): Result<List<AcademyTeacher>> =
        call(path).map { body ->
            val arr = JSONArray(body.raw.ifBlank { "[]" })
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(AcademyTeacher::from) }
        }

    @JvmInline
    value class Body(val raw: String) {
        fun asObject(): JSONObject = if (raw.isBlank()) JSONObject() else JSONObject(raw)
    }

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Result<Body> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload: RequestBody? = when {
                    body != null -> body.toString().toRequestBody(json)
                    method != "GET" -> "".toRequestBody(json)   // POST 는 본문 필수
                    else -> null
                }
                val req = Request.Builder().url(apiBase + path).method(method, payload)
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                client.newCall(req).execute().use { res ->
                    val text = res.body?.string().orEmpty()
                    if (!res.isSuccessful) {
                        // 서버 message 를 그대로 올린다 — 코드 오타·권한 안내가 여기 담긴다.
                        val msg = runCatching { JSONObject(text).optString("message") }.getOrNull()
                        error(msg?.ifEmpty { null } ?: "요청에 실패했어요.")
                    }
                    Body(text)
                }
            }
        }
}
