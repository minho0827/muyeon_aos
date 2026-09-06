package com.muyeon.app.ui.lesson

import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.quote.map
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
 * 레슨 일정 REST — iOS `LessonService`(LessonModels.swift) 1:1.
 *  GET/PATCH/DELETE /api/lessons/schedules.
 */
class LessonApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    /** 기간 목록 — from/to 는 'yyyy-MM-dd'. */
    suspend fun list(from: String? = null, to: String? = null): Result<List<LessonSchedule>> {
        val q = buildList {
            if (!from.isNullOrEmpty()) add("from=$from")
            if (!to.isNullOrEmpty()) add("to=$to")
        }
        val path = "/lessons/schedules" + if (q.isEmpty()) "" else "?" + q.joinToString("&")
        return call(path).map { JSONArray(it.ifBlank { "[]" }).map(LessonSchedule::from) }
    }

    /** 날짜 미정(PENDING) 목록 — 강사 '일정 확정 필요' 배너. */
    suspend fun pending(): Result<List<LessonSchedule>> =
        call("/lessons/schedules/pending").map { JSONArray(it.ifBlank { "[]" }).map(LessonSchedule::from) }

    /** 단건 조회(상태 무관) — 취소/완료 알림 딥링크에서 목록에 없어도 상세 열람. */
    suspend fun get(id: Int): Result<LessonSchedule> =
        call("/lessons/schedules/$id").map { LessonSchedule.from(JSONObject(it)) }

    suspend fun history(id: Int): Result<List<LessonHistoryItem>> =
        call("/lessons/schedules/$id/history").map { JSONArray(it.ifBlank { "[]" }).map(LessonHistoryItem::from) }

    /**
     * 일정 확정/변경 페이로드 규약:
     *  - place/placeAddress/placeLat/placeLng 는 **항상 전송**(빈 값 = 해제).
     *  - calendarId 는 3-상태: null=미변경(키 생략) / Some(null)=기본으로 해제(JSONObject.NULL) / Some(v)=배정
     *
     * ⚠️ 좌표만 iOS 와 다르게 보낸다 — iOS 는 좌표를 있을 때만 실어서, 장소를 다른 곳으로
     *   바꾸면 서버가 **예전 좌표를 그대로 유지**한다(PATCH 는 미전송을 '변경 없음'으로 본다).
     *   그러면 이름·주소는 새 장소인데 지도 핀만 옛 장소를 가리킨다. 여기선 명시적 null 로 해제한다.
     */
    suspend fun setSchedule(
        id: Int,
        startAt: String? = null,
        place: LessonPlace? = null,
        memo: String? = null,
        calendarChange: CalendarChange? = null,
    ): Result<Unit> {
        val body = JSONObject()
        if (startAt != null) body.put("startAt", startAt)
        body.put("place", place?.name ?: "")
        body.put("placeAddress", place?.address ?: "")
        body.put("placeLat", place?.lat ?: JSONObject.NULL)
        body.put("placeLng", place?.lng ?: JSONObject.NULL)
        if (memo != null) body.put("memo", memo)
        calendarChange?.let { body.put("calendarId", it.value ?: JSONObject.NULL) }
        return call("/lessons/schedules/$id", "PATCH", body).map { }
    }

    /** 캘린더 배정 변경 의도 — 값이 null 이면 '기본으로 해제'. 아예 안 넘기면 미변경. */
    @JvmInline
    value class CalendarChange(val value: Int?)

    /** 레슨 완료(강사) → DONE + 회원 리뷰 작성 가능. */
    suspend fun complete(id: Int): Result<Unit> = call("/lessons/schedules/$id/complete", "PATCH").map { }

    /** 완료 취소(강사) — 후기 작성 전까지만(서버 검증). */
    suspend fun uncomplete(id: Int): Result<Unit> = call("/lessons/schedules/$id/uncomplete", "PATCH").map { }

    suspend fun cancel(id: Int): Result<Unit> = call("/lessons/schedules/$id", "DELETE").map { }

    /** 일정의 내 캘린더 이동 — 조용한 변경(알림·이력 없음). */
    suspend fun setCalendar(id: Int, calendarId: Int?): Result<Unit> =
        call(
            "/lessons/schedules/$id/calendar", "PATCH",
            JSONObject().put("calendarId", calendarId ?: JSONObject.NULL),
        ).map { }

    /** 레슨 인연(수강생) 목록 — 캘린더발 약속잡기(최근 레슨순). */
    suspend fun partners(): Result<List<LessonPartnerSummary>> =
        call("/lessons/partners").map { JSONArray(it.ifBlank { "[]" }).map(LessonPartnerSummary::from) }

    /** 1:1 채팅방 확보(멱등) — 방 없는 상대에게 제안 보낼 때. */
    suspend fun ensureDirectRoom(targetUserId: Int): Result<Int> =
        call("/chat/rooms/direct", "POST", JSONObject().put("targetUserId", targetUserId))
            .map { JSONObject(it.ifBlank { "{}" }).optInt("roomId", 0) }

    internal suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Result<String> =
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
