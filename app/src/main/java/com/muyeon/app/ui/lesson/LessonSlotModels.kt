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
data class MyLessonProduct(val id: Int, val title: String, val maxParticipants: Int?) {
    companion object {
        fun from(o: JSONObject) = MyLessonProduct(o.optInt("id"), o.optString("title"), o.intOrNull("maxParticipants"))
    }
}

/** 슬롯 예약자(출석 체크용). */
data class LessonSlotReservation(
    val id: Int,
    val userId: Int?,
    val userName: String?,
    val headcount: Int,
    val status: String?,
    val attendance: String?,   // PRESENT | ABSENT | null
) {
    companion object {
        fun from(o: JSONObject) = LessonSlotReservation(
            o.optInt("id"), o.intOrNull("userId"), o.stringOrNull("userName"),
            o.optInt("headcount", 1), o.stringOrNull("status"), o.stringOrNull("attendance"),
        )
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

    /** 출석 체크 — PATCH /lesson-slots/:sid/reservations/:rid/attendance { attendance }. */
    suspend fun setAttendance(slotId: Int, reservationId: Int, attendance: String): Result<Unit> =
        call(
            "/lesson-slots/$slotId/reservations/$reservationId/attendance", "PATCH",
            JSONObject().put("attendance", attendance),
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
