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

/**
 * 레슨 예약 — iOS `LessonBookingModels.swift` 의 예약 파트 이식.
 *  회차(슬롯) 선택 → 예약/변경/취소. 변경은 서버가 원자적 리스케줄(기존 취소 + 새 회차 재지정).
 */

/**
 * 레슨 상품의 결제 설정 — iOS `LessonBookingProductDTO`.
 *  ★ 금액은 전부 서버 값이다. 화면에서 만들지 말 것 — 표시액과 청구액이 갈라진다.
 *  ★ 예약금은 총 레슨비의 일부이며 별도 이용료가 아니다(잔액은 현장 결제).
 */
data class LessonBookingProduct(
    val price: Int?,
    val priceUnit: String?,      // PER_PERSON | PER_BOOKING
    val paymentMode: String?,    // NONE | DEPOSIT
    val depositRequired: Boolean?,
    val depositAmount: Int?,
    val cancelPolicy: String?,   // 판매자 추가 안내(공통 기준보다 유리할 때만 적용)
) {
    /** 인원 배수. 예약 건당 과금이면 인원과 무관하게 1. */
    fun multiplier(headcount: Int): Int =
        if (priceUnit == "PER_BOOKING") 1 else maxOf(1, headcount)

    fun totalPrice(headcount: Int): Int = (price ?: 0) * multiplier(headcount)

    fun depositFor(headcount: Int): Int {
        // paymentMode 가 비어 있는 구버전 상품은 depositRequired 로 판정(iOS 와 동일).
        val mode = paymentMode ?: if (depositRequired == true) "DEPOSIT" else "NONE"
        return if (mode == "DEPOSIT") (depositAmount ?: 20_000) * multiplier(headcount) else 0
    }

    fun remainingFor(headcount: Int): Int =
        maxOf(0, totalPrice(headcount) - depositFor(headcount))

    companion object {
        fun from(o: JSONObject) = LessonBookingProduct(
            price = o.intOrNull("price"),
            priceUnit = o.stringOrNull("priceUnit"),
            paymentMode = o.stringOrNull("paymentMode"),
            depositRequired = if (o.has("depositRequired")) o.optBoolean("depositRequired") else null,
            depositAmount = o.intOrNull("depositAmount"),
            cancelPolicy = o.stringOrNull("cancelPolicy"),
        )
    }
}

/**
 * 예약 생성 결과 — iOS `LessonReserveResultDTO`.
 *  status 가 PENDING_PAYMENT 면 아직 확정이 아니다. 정원만 잡아둔 상태이고 결제해야 예약이 된다
 *  (결제 없이 두면 서버가 15분 뒤 자리를 회수한다).
 */
data class LessonReserveResult(val id: Int, val status: String) {
    val needsPayment: Boolean get() = status == "PENDING_PAYMENT"

    companion object {
        fun from(o: JSONObject) =
            LessonReserveResult(o.optInt("id"), o.optString("status"))
    }
}

/** 내 예약 1건. */
data class MyLessonReservation(
    val id: Int,
    val status: String,
    val headcount: Int,
    val date: String?,
    val startTime: String?,
    val endTime: String?,
    val lessonProductId: Int,
    val title: String,
) {
    val timeLabel: String
        get() = listOfNotNull(date, startTime?.let { LessonTimeFmt.ampm(it) }).joinToString(" ")

    companion object {
        fun from(o: JSONObject) = MyLessonReservation(
            o.optInt("id"), o.optString("status"), o.optInt("headcount", 1),
            o.stringOrNull("date"), o.stringOrNull("startTime"), o.stringOrNull("endTime"),
            o.optInt("lessonProductId"), o.optString("title"),
        )
    }
}

/** 예약 상세 — 취소 규정·장소 등 안내 포함. */
data class LessonReservationDetail(
    val id: Int,
    val status: String?,
    val headcount: Int,
    val date: String?,
    val startTime: String?,
    val endTime: String?,
    val title: String?,
    val place: String?,
    val address: String?,
    val price: Int?,
    val cancelPolicy: String?,
    val notice: String?,
    val teacherName: String?,
) {
    companion object {
        fun from(o: JSONObject) = LessonReservationDetail(
            o.optInt("id"), o.stringOrNull("status"), o.optInt("headcount", 1),
            o.stringOrNull("date"), o.stringOrNull("startTime"), o.stringOrNull("endTime"),
            o.stringOrNull("title"), o.stringOrNull("place"), o.stringOrNull("address"),
            o.intOrNull("price"), o.stringOrNull("cancelPolicy"), o.stringOrNull("notice"),
            o.stringOrNull("teacherName"),
        )
    }
}

/**
 * 예약 취소 사유 — 코드 문자열은 서버(CANCEL_REASON_CODES)·admin 과 **3레포 계약**.
 *  ⚠️ 값을 바꾸면 관리자 통계가 조용히 깨진다.
 */
enum class LessonCancelReason(val code: String, val label: String) {
    OTHER_LESSON("OTHER_LESSON", "다른 강사(레슨)를 예약했어요."),
    RESCHEDULE("RESCHEDULE", "예약 날짜나 시간을 변경하고 싶어요."),
    CHANGE_OF_MIND("CHANGE_OF_MIND", "단순히 마음이 바뀌었어요."),
    TEACHER_CANCELED("TEACHER_CANCELED", "강사님 사정으로 취소하게 됐어요."),
}

class LessonBookingApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    /** 예약 가능한 회차 — GET /lesson-products/:id/available-slots?from=&to= */
    suspend fun availableSlots(productId: Int, from: String, to: String): Result<List<LessonSlot>> =
        call("/lesson-products/$productId/available-slots?from=$from&to=$to")
            .map { JSONArray(it.ifBlank { "[]" }).map(LessonSlot::from) }

    /**
     * 상품의 결제 설정(총 레슨비·예약금·과금 단위). 예약 확정 전 금액 안내에 쓴다.
     *  ★ available-slots 는 회차만 준다 — 금액은 상품에서 따로 읽어야 한다(iOS·웹과 동일).
     */
    suspend fun product(productId: Int): Result<LessonBookingProduct> =
        call("/lesson-products/$productId").map { LessonBookingProduct.from(JSONObject(it)) }

    /** 예약 생성. 예약금 상품이면 status=PENDING_PAYMENT 로 오고 결제해야 확정된다. */
    suspend fun reserve(slotInstanceId: Int, headcount: Int): Result<LessonReserveResult> =
        call(
            "/lesson-reservations", "POST",
            JSONObject().put("slotInstanceId", slotInstanceId).put("headcount", headcount),
        ).map { LessonReserveResult.from(JSONObject(it)) }

    /** 예약 변경 — 서버가 기존 예약 취소 + 새 회차 재지정을 원자적으로 처리한다. */
    suspend fun reschedule(reservationId: Int, slotInstanceId: Int, headcount: Int): Result<Unit> =
        call(
            "/lesson-reservations/$reservationId", "PATCH",
            JSONObject().put("slotInstanceId", slotInstanceId).put("headcount", headcount),
        ).map { }

    suspend fun myReservations(): Result<List<MyLessonReservation>> =
        call("/me/lesson-reservations").map { JSONArray(it.ifBlank { "[]" }).map(MyLessonReservation::from) }

    suspend fun cancel(reservationId: Int, reasonCode: String): Result<Unit> =
        call("/lesson-reservations/$reservationId/cancel", "POST", JSONObject().put("reasonCode", reasonCode)).map { }

    suspend fun reservationDetail(id: Int): Result<LessonReservationDetail> =
        call("/lesson-reservations/$id/detail").map { LessonReservationDetail.from(JSONObject(it)) }

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
