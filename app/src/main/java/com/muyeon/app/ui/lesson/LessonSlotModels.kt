package com.muyeon.app.ui.lesson

import com.muyeon.app.BuildConfig
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
import java.util.Locale

/**
 * 레슨 예약 슬롯/템플릿 — iOS `LessonBookingModels.swift` 의 슬롯 파트 이식.
 *  템플릿(요일 규칙) → 서버가 실제 슬롯(날짜별)을 생성한다.
 */

/** 실제 예약 슬롯(날짜별). */
data class LessonSlot(
    val id: Int,
    val date: String,        // "2026-07-16"
    val startTime: String,   // "19:00"
    val endTime: String,
    val capacity: Int,
    val reservedCount: Int,
    val status: String,      // OPEN | FULL | CLOSED
) {
    val remaining: Int get() = (capacity - reservedCount).coerceAtLeast(0)
    val isOpen: Boolean get() = status == "OPEN" && remaining > 0
    val timeLabel: String get() = LessonTimeFmt.ampm(startTime)

    companion object {
        fun from(o: JSONObject) = LessonSlot(
            o.optInt("id"), o.optString("date"), o.optString("startTime"), o.optString("endTime"),
            o.optInt("capacity"), o.optInt("reservedCount"), o.optString("status"),
        )
    }
}

/** 요일 규칙(템플릿). */
data class LessonSlotTemplate(
    val id: Int,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val capacity: Int,
) {
    val dayLabel: String get() = LessonOptions.dayLabel(dayOfWeek)
    val timeLabel: String get() = "${LessonTimeFmt.ampm(startTime)}~${LessonTimeFmt.ampm(endTime)}"

    companion object {
        fun from(o: JSONObject) = LessonSlotTemplate(
            o.optInt("id"), o.optInt("dayOfWeek"), o.optString("startTime"),
            o.optString("endTime"), o.optInt("capacity"),
        )
    }
}

/** 내 레슨 상품(슬롯 관리 선택용). */
data class MyLessonProduct(
    val id: Int,
    val title: String,
    val maxParticipants: Int?,
    // 학원이 관리하는 레슨이면 학원 id·이름. 시간표 편성은 학원 몫이라 강사 화면은 읽기전용이 된다.
    val ownerAcademyId: Int? = null,
    val ownerAcademyName: String? = null,
) {
    /**
     * 지금 보는 사람이 시간표를 고칠 수 없는 레슨인가.
     * /lesson-products/mine 은 '내가 만든 것' 또는 '우리 학원이 관리하는 것'만 주므로,
     * 학원 유형이 아닌데 ownerAcademyId 가 있으면 나는 소속 강사 쪽이다.
     */
    fun slotsReadOnly(isAcademy: Boolean): Boolean = ownerAcademyId != null && !isAcademy

    val academyName: String get() = ownerAcademyName ?: "학원"

    companion object {
        fun from(o: JSONObject) = MyLessonProduct(
            o.optInt("id"), o.optString("title"), o.intOrNull("maxParticipants"),
            ownerAcademyId = o.intOrNull("ownerAcademyId"),
            ownerAcademyName = o.optJSONObject("ownerAcademy")?.stringOrNull("name"),
        )
    }
}

/** 명단의 회원 수강권 — 출석 처리 시 '어느 권에서 차감할지' 선택기용. iOS `RosterPassDTO`. */
data class RosterPass(
    val id: Int,
    val productName: String,
    val passType: String,          // PERIOD | COUNT
    val remainingCount: Int?,
) {
    val label: String
        get() = if (passType == "COUNT") "$productName · 잔여 ${remainingCount ?: 0}회" else "$productName · 기간권"

    companion object {
        fun from(o: JSONObject) = RosterPass(
            o.optInt("id"), o.optString("productName"), o.optString("passType"),
            o.intOrNull("remainingCount"),
        )
    }
}

/**
 * 슬롯 예약자(출석 체크용) — iOS `LessonSlotReservationDTO` 1:1.
 *
 * ⚠️ 출석 상태는 **status** 다(CONFIRMED|ATTENDED|NOSHOW). 종전 AOS 는 없는
 *   `attendance`("PRESENT"/"ABSENT") 를 읽고 있어 늘 null 이었고, 이름도
 *   서버에 없는 `userName` 을 봤다(서버 키는 name).
 */
data class LessonSlotReservation(
    val id: Int,
    val userId: Int?,
    val name: String,
    val phone: String?,
    val headcount: Int,
    val status: String?,           // CONFIRMED | ATTENDED | NOSHOW
    val passes: List<RosterPass>,
    val rescheduleStatus: String?, // REQUESTED 면 승인/거절 줄이 뜬다
) {
    val attendance: SlotAttendance get() = SlotAttendance.from(status)

    companion object {
        fun from(o: JSONObject) = LessonSlotReservation(
            o.optInt("id"), o.intOrNull("userId"),
            o.stringOrNull("name") ?: "회원", o.stringOrNull("phone"),
            o.optInt("headcount", 1), o.stringOrNull("status"),
            o.optJSONArray("passes")?.map(RosterPass::from) ?: emptyList(),
            o.stringOrNull("rescheduleStatus"),
        )
    }
}

/** 출석 상태 — 예약/출석/결석. iOS `AttendanceState` 와 같은 라벨·색. */
enum class SlotAttendance(val label: String, val tint: androidx.compose.ui.graphics.Color) {
    CONFIRMED("예약", com.muyeon.app.ui.common.MuyeonColors.primary),
    ATTENDED("출석", androidx.compose.ui.graphics.Color(0xFF29A659)),
    NOSHOW("결석", androidx.compose.ui.graphics.Color(0xFFE6382E));

    companion object {
        /** 모르는 값은 '예약'으로 — iOS ?? .confirmed. */
        fun from(status: String?): SlotAttendance = entries.firstOrNull { it.name == status } ?: CONFIRMED
    }
}

/** "2026-07-17" → "2026.07.17 (금)" — iOS LessonDateFmt.krDate. */
object LessonDateFmt {
    private val WEEK = listOf("일", "월", "화", "수", "목", "금", "토")

    fun krDate(ymd: String): String {
        val p = ymd.split("-").mapNotNull { it.toIntOrNull() }
        if (p.size < 3) return ymd
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Seoul"))
        cal.clear()
        cal.set(p[0], p[1] - 1, p[2])
        val w = WEEK[(cal.get(java.util.Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
        return String.format(Locale.KOREA, "%04d.%02d.%02d (%s)", p[0], p[1], p[2], w)
    }
}

/** "19:00" → "오후 7:00" — iOS LessonTimeFmt.ampm. */
object LessonTimeFmt {
    fun ampm(hhmm: String): String {
        val p = hhmm.split(":").mapNotNull { it.toIntOrNull() }
        if (p.size < 2) return hhmm
        val h = p[0]
        val label = if (h < 12) "오전" else "오후"
        val h12 = when {
            h % 12 == 0 -> 12
            else -> h % 12
        }
        return String.format(Locale.KOREA, "%s %d:%02d", label, h12, p[1])
    }
}

class LessonSlotApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    /** 내 레슨 상품 목록(슬롯 관리 대상). */
    suspend fun myProducts(): Result<List<MyLessonProduct>> =
        call("/lesson-products/mine").map { JSONArray(it.ifBlank { "[]" }).map(MyLessonProduct::from) }

    /** 기간 슬롯 — GET /lesson-products/:id/slots?from=&to= */
    suspend fun slots(productId: Int, from: String, to: String): Result<List<LessonSlot>> =
        call("/lesson-products/$productId/slots?from=$from&to=$to")
            .map { JSONArray(it.ifBlank { "[]" }).map(LessonSlot::from) }

    suspend fun templates(productId: Int): Result<List<LessonSlotTemplate>> =
        call("/lesson-slot-templates?lessonProductId=$productId")
            .map { JSONArray(it.ifBlank { "[]" }).map(LessonSlotTemplate::from) }

    suspend fun createTemplate(
        productId: Int,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        capacity: Int,
    ): Result<Unit> = call(
        "/lesson-slot-templates", "POST",
        JSONObject()
            .put("lessonProductId", productId).put("dayOfWeek", dayOfWeek)
            .put("startTime", startTime).put("endTime", endTime).put("capacity", capacity),
    ).map { }

    suspend fun deleteTemplate(id: Int): Result<Unit> = call("/lesson-slot-templates/$id", "DELETE").map { }

    suspend fun slotReservations(slotId: Int): Result<List<LessonSlotReservation>> =
        call("/lesson-slots/$slotId/reservations").map { JSONArray(it.ifBlank { "[]" }).map(LessonSlotReservation::from) }

    /**
     * 출석 체크 — PATCH /lesson-slots/:sid/reservations/:rid/attendance.
     *  ⚠️ 바디 키는 **status** 다. 종전엔 `attendance` 로 보내 서버 DTO 검증에서 전부 400 이었다.
     *  passId 를 주면 그 수강권에서 차감하고, 생략하면 서버가 만료 임박순으로 고른다.
     */
    suspend fun setAttendance(
        slotId: Int,
        reservationId: Int,
        status: String,
        passId: Int? = null,
    ): Result<Unit> = call(
        "/lesson-slots/$slotId/reservations/$reservationId/attendance", "PATCH",
        JSONObject().put("status", status).apply { passId?.let { put("passId", it) } },
    ).map { }

    /** 일정 변경 요청 승인·거절 — PATCH /lesson-reservations/:id/reschedule-decision. */
    suspend fun decideReschedule(reservationId: Int, approved: Boolean): Result<Unit> =
        call(
            "/lesson-reservations/$reservationId/reschedule-decision", "PATCH",
            JSONObject().put("approved", approved),
        ).map { }

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
