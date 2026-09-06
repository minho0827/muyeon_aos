package com.muyeon.app.ui.lesson

import androidx.compose.ui.graphics.Color
import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.quote.doubleOrNull
import com.muyeon.app.ui.quote.intOrNull
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
 * 사용자 캘린더(timetree식) — iOS `UserCalendar.swift` 1:1.
 *  강사/원장이 만든 캘린더(이름·색)에 일정을 배정해 색으로 구분·필터.
 *  서버: /studio/calendars CRUD.
 */
data class UserCalendar(
    val id: Int,
    val name: String,
    val color: String,       // hex "#RRGGBB"
    val preset: String? = null,
    val sortOrder: Int? = null,
) {
    val uiColor: Color get() = hexToColor(color)

    companion object {
        /** "기본"(미배정) 가상 캘린더 — 서버에 없고 클라 표시 전용. id=0. */
        val DEFAULT = UserCalendar(0, "기본", "#F58232", null, -1)

        fun from(o: JSONObject) = UserCalendar(
            o.optInt("id"), o.optString("name"), o.optString("color").ifEmpty { "#F58232" },
            o.stringOrNull("preset"), o.intOrNull("sortOrder"),
        )

        fun hexToColor(hex: String): Color = runCatching {
            val clean = hex.removePrefix("#")
            Color(("ff$clean").toLong(16))
        }.getOrDefault(Color(0xFFF58232))
    }
}

object UserCalendarCatalog {

    /** 12색 팔레트(브랜드 주황 선두) — 만들기 시트 스와치. */
    val palette = listOf(
        "#F58232", "#E32502", "#E6A015", "#9AA208", "#2E9E5B", "#17A2A6",
        "#3478F6", "#6A5ACD", "#B04FBF", "#E85D8A", "#8D6E63", "#6D6E71",
    )

    data class Preset(val id: String, val name: String, val color: String)

    /** 만들기 시트 프리셋(무용 도메인) — 탭하면 이름·색 프리필. */
    val presets = listOf(
        Preset("ADULT", "성인 취미반", "#F58232"),
        Preset("EXAM", "입시반", "#E32502"),
        Preset("KIDS", "유아·초등반", "#E6A015"),
        Preset("MAJOR", "전공·콩쿠르", "#6A5ACD"),
        Preset("PERSONAL", "개인 일정", "#2E9E5B"),
        Preset("SPACE", "공간·대관", "#3478F6"),
    )

    /** 스마트 기본값 — 일정의 과목명(발레/바레 등)이 이름에 포함된 캘린더를 우선 제안. */
    fun match(genre: String?, calendars: List<UserCalendar>): UserCalendar? {
        if (genre.isNullOrEmpty()) return null
        return calendars.firstOrNull { it.name.replace(" ", "").contains(genre) }
    }
}

/** 개인 일정 블록(스튜디오 일정) — 캘린더 그리드에 레슨과 함께 표시. */
data class StudioBlock(
    val id: Int,
    val date: String,          // yyyy-MM-dd
    val title: String,
    val allDay: Boolean,
    val startTime: String?,    // HH:mm
    val endTime: String?,
    val calendarId: Int?,
    val memo: String?,
    val place: String?,
    val placeAddress: String?,
    val placeLat: Double?,
    val placeLng: Double?,
) {
    companion object {
        fun from(o: JSONObject) = StudioBlock(
            o.optInt("id"), o.optString("date"), o.optString("title"),
            o.optBoolean("allDay", false), o.stringOrNull("startTime"), o.stringOrNull("endTime"),
            o.intOrNull("calendarId"), o.stringOrNull("memo"),
            o.stringOrNull("place"), o.stringOrNull("placeAddress"),
            o.doubleOrNull("placeLat"), o.doubleOrNull("placeLng"),
        )
    }
}

class UserCalendarApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    suspend fun list(): Result<List<UserCalendar>> =
        call("/studio/calendars").map { JSONArray(it.ifBlank { "[]" }).map(UserCalendar::from) }

    suspend fun create(name: String, color: String, preset: String?): Result<Int> =
        call(
            "/studio/calendars", "POST",
            JSONObject().put("name", name).put("color", color).apply { preset?.let { put("preset", it) } },
        ).map { JSONObject(it.ifBlank { "{}" }).optInt("id", 0) }

    suspend fun update(id: Int, name: String, color: String): Result<Unit> =
        call("/studio/calendars/$id", "PATCH", JSONObject().put("name", name).put("color", color)).map { }

    suspend fun remove(id: Int): Result<Unit> = call("/studio/calendars/$id", "DELETE").map { }

    /** 개인 일정 조회 — from/to 'yyyy-MM-dd'. { blocks: [] }. */
    /**
     * 개인 일정 등록 — POST /studio/schedule-blocks.
     *  하루 종일이면 시간 필드를 아예 보내지 않는다(서버가 HH:mm 형식을 강제한다).
     */
    suspend fun createBlock(
        date: String, title: String, calendarId: Int?, allDay: Boolean,
        startTime: String?, endTime: String?, memo: String?, remindMinutesBefore: Int?,
        place: LessonPlace? = null,
    ): Result<Unit> = call("/studio/schedule-blocks", "POST",
        blockBody(date, title, calendarId, allDay, startTime, endTime, memo, remindMinutesBefore, place)).map { }

    /** 개인 일정 수정 — PATCH /studio/schedule-blocks/:id. */
    suspend fun updateBlock(
        id: Int, date: String, title: String, calendarId: Int?, allDay: Boolean,
        startTime: String?, endTime: String?, memo: String?, remindMinutesBefore: Int?,
        place: LessonPlace? = null,
    ): Result<Unit> = call("/studio/schedule-blocks/$id", "PATCH",
        blockBody(date, title, calendarId, allDay, startTime, endTime, memo, remindMinutesBefore, place)).map { }

    suspend fun deleteBlock(id: Int): Result<Unit> =
        call("/studio/schedule-blocks/$id", "DELETE").map { }

    private fun blockBody(
        date: String, title: String, calendarId: Int?, allDay: Boolean,
        startTime: String?, endTime: String?, memo: String?, remindMinutesBefore: Int?,
        place: LessonPlace?,
    ): JSONObject = JSONObject()
        .put("date", date)
        .put("title", title)
        .put("allDay", allDay)
        .apply {
            calendarId?.takeIf { it > 0 }?.let { put("calendarId", it) }
            if (!allDay) {
                startTime?.let { put("startTime", it) }
                endTime?.let { put("endTime", it) }
            }
            memo?.takeIf { it.isNotEmpty() }?.let { put("memo", it) }
            remindMinutesBefore?.let { put("remindMinutesBefore", it) }
            // 좌표는 검색으로 고른 장소에만 있다. 직접입력이면 place 만 값이 있고 나머지는 null.
            // ⚠️ 비었을 때 키를 빼면 안 된다 — 서버는 undefined 를 '변경 없음'으로 보므로
            //   수정에서 장소를 지워도 예전 값이 그대로 남는다(DTO 주석: null 전달 = 해제).
            put("place", place?.name?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            put("placeAddress", place?.address ?: JSONObject.NULL)
            put("placeLat", place?.lat ?: JSONObject.NULL)
            put("placeLng", place?.lng ?: JSONObject.NULL)
        }

    suspend fun schedule(from: String, to: String): Result<List<StudioBlock>> =
        call("/studio/schedule?from=$from&to=$to").map {
            JSONObject(it.ifBlank { "{}" }).optJSONArray("blocks")?.map(StudioBlock::from) ?: emptyList()
        }

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = when {
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
                    if (!res.isSuccessful) {
                        val msg = runCatching { JSONObject(text).optString("message") }.getOrNull()
                        throw IllegalStateException(msg?.ifEmpty { null } ?: "요청에 실패했어요.")
                    }
                    text
                }
            }
        }

    private companion object { val JSON = "application/json".toMediaType() }
}
