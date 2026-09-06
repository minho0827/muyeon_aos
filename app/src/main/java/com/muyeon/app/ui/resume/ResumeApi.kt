package com.muyeon.app.ui.resume

import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.quote.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * 이력서/공개프로필/지원자 REST — iOS `ResumeService`(ResumeModels.swift) 1:1.
 *  엔드포인트·페이로드 키를 iOS 와 동일하게 유지(서버 무변경).
 */
class ResumeApi(internal val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    // ── 이력서 CRUD ──

    /** GET /resumes?roleIntent= — 강사/무용수 이력서를 섞지 않는다(iOS ResumeService.list). */
    suspend fun list(mode: ResumeMode = ResumeMode.TEACHER): Result<List<ResumeListItem>> =
        call("/resumes?roleIntent=${mode.roleIntent}")
            .map { JSONArray(it.ifBlank { "[]" }).map(ResumeListItem::from) }

    suspend fun getOne(id: Int): Result<ResumeDetail> =
        call("/resumes/$id").map { ResumeDetail.from(JSONObject(it)) }

    /**
     * 저장 — 서버가 data 를 spread 병합하므로 편집 화면의 전체 스냅샷을 보낸다.
     *  ResumeData.toJson() 이 raw(미지 키 포함) 위에 아는 키만 덮어쓴다.
     */
    suspend fun save(
        id: Int?,
        title: String,
        data: ResumeData,
        mode: ResumeMode = ResumeMode.TEACHER,
        publishSeek: Boolean = true,
    ): Result<Int> {
        val path = if (id != null) "/resumes/$id" else "/resumes"
        val method = if (id != null) "PATCH" else "POST"
        val tagged = data.copy(roleIntent = mode.roleIntent)
        val body = JSONObject()
            .put("title", title)
            .put("data", tagged.toJson())
            .put("publishSeek", publishSeek)
            .put("roleIntent", mode.roleIntent)
        return call(path, method, body).map { JSONObject(it.ifBlank { "{}" }).optInt("id", id ?: 0) }
    }

    suspend fun setDefault(id: Int): Result<Unit> = call("/resumes/$id/default", "PATCH").map { }

    suspend fun remove(id: Int): Result<Unit> = call("/resumes/$id", "DELETE").map { }

    // ── 구직 프로필(열람 알림) — iOS ResumeService.get/setProfileViewAlert ──

    /** GET /auth/me/profile → viewAlert. 실패 시 iOS 와 같이 true(기본 켜짐). */
    suspend fun getProfileViewAlert(): Boolean =
        call("/auth/me/profile").getOrNull()
            ?.let { runCatching { JSONObject(it).optBoolean("viewAlert", true) }.getOrNull() } ?: true

    suspend fun setProfileViewAlert(enabled: Boolean): Result<Unit> =
        call("/auth/me/profile", "PATCH", JSONObject().put("viewAlert", enabled)).map { }

    // ── 공개범위 ──

    /** GET /resumes/visibility → { flags, profileHidden }. */
    suspend fun getVisibility(): Result<Pair<FieldVisibilityFlags, Boolean>> =
        call("/resumes/visibility").map { text ->
            val o = JSONObject(text.ifBlank { "{}" })
            FieldVisibilityFlags.from(o.optJSONObject("flags")) to o.optBoolean("profileHidden", false)
        }

    suspend fun setVisibility(flags: FieldVisibilityFlags, profileHidden: Boolean? = null): Result<Unit> {
        val body = flags.toJson()
        if (profileHidden != null) body.put("profileHidden", profileHidden)
        return call("/resumes/visibility", "PATCH", body).map { }
    }

    // ── UI 안내 플래그(계정 기준 1회 노출) — 웹 tutorialFlags 와 동일 저장소 ──

    suspend fun uiFlagSeen(key: String): Boolean =
        call("/auth/me/ui-flags").getOrNull()
            ?.let { runCatching { JSONObject(it).optJSONObject("flags")?.optBoolean(key, false) }.getOrNull() } == true

    suspend fun markUiFlag(key: String) { call("/auth/me/ui-flags", "PATCH", JSONObject().put(key, true)) }

    // ── 공개 프로필 ──

    /** GET /teachers/:id (preview=본인이 일반회원 시점으로 확인). */
    suspend fun publicProfile(userId: Int, preview: Boolean = false, src: String? = null): Result<PublicProfile> {
        val q = buildList {
            if (preview) add("preview=1")
            if (!src.isNullOrEmpty()) add("src=" + URLEncoder.encode(src, "UTF-8"))
        }
        val qs = if (q.isEmpty()) "" else "?" + q.joinToString("&")
        return call("/teachers/$userId$qs").map { PublicProfile.from(JSONObject(it)) }
    }

    suspend fun setScrap(teacherId: Int, on: Boolean): Result<Unit> =
        call("/teachers/$teacherId/scrap", if (on) "POST" else "DELETE").map { }

    suspend fun report(teacherId: Int, reason: String): Result<Unit> =
        call(
            "/reports", "POST",
            JSONObject().put("targetType", "TEACHER").put("targetId", teacherId).put("reason", reason),
        ).map { }

    // ── 원장: 지원자 이력서 열람 + 합불 ──

    suspend fun applicant(
        postingId: Int,
        applicationId: Int,
        kind: ApplicantPostingKind = ApplicantPostingKind.JOB,
    ): Result<Applicant> =
        call("/${kind.apiPath}/$postingId/applicants/$applicationId").map { Applicant.from(JSONObject(it)) }

    /** 합불 처리 → { remainingReviewingCount } (남은 검토중 지원자 수). */
    suspend fun decide(
        applicationId: Int,
        status: String,
        kind: ApplicantPostingKind = ApplicantPostingKind.JOB,
    ): Result<Int> =
        call("/${kind.apiPath}/applications/$applicationId/status", "PATCH", JSONObject().put("status", status))
            .map { JSONObject(it.ifBlank { "{}" }).optInt("remainingReviewingCount", 0) }

    /** 공고 마감 + 남은 지원자 일괄 미선정 → rejectedCount. */
    suspend fun finalizePosting(postingId: Int, kind: ApplicantPostingKind): Result<Int> =
        call("/${kind.apiPath}/$postingId/close", "PATCH", JSONObject().put("rejectPending", true))
            .map { JSONObject(it.ifBlank { "{}" }).optInt("rejectedCount", 0) }

    /** 지원자와 1:1 채팅방 생성 → roomId. */
    suspend fun directRoom(
        targetUserId: Int,
        applicationKind: String? = null,
        applicationId: Int? = null,
    ): Result<Int> {
        val body = JSONObject().put("targetUserId", targetUserId)
        if (applicationKind != null) body.put("applicationKind", applicationKind)
        if (applicationId != null) body.put("applicationId", applicationId)
        return call("/chat/rooms/direct", "POST", body)
            .map { JSONObject(it.ifBlank { "{}" }).optInt("roomId", 0) }
    }

    // ── 업로드(사진/포트폴리오) ──

    suspend fun uploadImage(bytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", "image.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
                .build()
            val req = Request.Builder().url("$apiBase/uploads/image").post(body)
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                .build()
            client.newCall(req).execute().use { res ->
                val text = res.body?.string().orEmpty()
                if (!res.isSuccessful) throw IllegalStateException("업로드 실패(${res.code})")
                JSONObject(text).optString("url")
            }
        }
    }

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload: RequestBody? = when {
                    body != null -> body.toString().toRequestBody(JSON)
                    method != "GET" && method != "DELETE" -> "".toRequestBody(JSON)
                    else -> null
                }
                val req = Request.Builder().url(apiBase + path).method(method, payload)
                    .addHeader("Content-Type", "application/json")
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                client.newCall(req).execute().use { res ->
                    val text = res.body?.string().orEmpty()
                    if (!res.isSuccessful) throw IllegalStateException(serverMessage(text) ?: "요청에 실패했어요.")
                    text
                }
            }
        }

    private fun serverMessage(text: String): String? = runCatching {
        val o = JSONObject(text)
        o.optJSONArray("message")
            ?.let { arr -> (0 until arr.length()).joinToString("\n") { arr.optString(it) } }?.ifEmpty { null }
            ?: o.optString("message").ifEmpty { null }
    }.getOrNull()

    private companion object { val JSON = "application/json".toMediaType() }
}
