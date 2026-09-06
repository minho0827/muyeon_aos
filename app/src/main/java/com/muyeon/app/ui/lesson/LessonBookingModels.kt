package com.muyeon.app.ui.lesson

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
/** 예약 상세의 레슨/장소 — 서버가 `lesson` 으로 중첩해 내려준다. */
data class LessonResPlace(
    val id: Int?,
    val title: String,
    val genre: String?,
    val description: String?,
    val place: String?,
    val address: String?,
    val lat: Double?,
    val lng: Double?,
    val phone: String?,
    val parkingInfo: String?,
    val valetInfo: String?,
    val notice: String?,
    val homepage: String?,
    val businessHours: List<LessonBizHour>?,
    val cancelPolicy: String?,
) {
    companion object {
        fun from(o: JSONObject?): LessonResPlace {
            val h = o?.optJSONArray("businessHours")
            return LessonResPlace(
                id = o?.intOrNull("id"),
                title = o?.stringOrNull("title") ?: "레슨",
                genre = o?.stringOrNull("genre"),
                description = o?.stringOrNull("description"),
                place = o?.stringOrNull("place"),
                address = o?.stringOrNull("address"),
                lat = o?.doubleOrNull("lat"),
                lng = o?.doubleOrNull("lng"),
                phone = o?.stringOrNull("phone"),
                parkingInfo = o?.stringOrNull("parkingInfo"),
                valetInfo = o?.stringOrNull("valetInfo"),
                notice = o?.stringOrNull("notice"),
                homepage = o?.stringOrNull("homepage"),
                businessHours = h?.let { arr ->
                    (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(LessonBizHour::from) }
                },
                cancelPolicy = o?.stringOrNull("cancelPolicy"),
            )
        }
    }
}

data class LessonBizHour(val day: String, val time: String, val lastOrder: String?) {
    companion object {
        fun from(o: JSONObject) =
            LessonBizHour(o.optString("day"), o.optString("time"), o.stringOrNull("lastOrder"))
    }
}

data class LessonResPerson(val id: Int?, val name: String, val phone: String?) {
    companion object {
        fun from(o: JSONObject?) =
            LessonResPerson(o?.intOrNull("id"), o?.stringOrNull("name") ?: "", o?.stringOrNull("phone"))
    }
}

/**
 * 예약 상세 — iOS `LessonResDetailDTO` 1:1.
 *
 * ⚠️ 종전 AOS 모델은 title/place/address 를 **최상위에서** 읽었는데 서버는 `lesson` 안에 넣어 보낸다
 *   (lesson-booking.service 의 detail 응답). 화면이 없어 드러나지 않았을 뿐 값이 전부 비었다.
 */
data class LessonReservationDetail(
    val id: Int,
    val status: String?,
    val headcount: Int,
    val deposit: Int,
    val paymentAmount: Int?,
    val totalPrice: Int?,
    val remainingAmount: Int?,
    val rescheduleCount: Int?,
    val disputeStatus: String?,
    val cancelFee: Int?,
    val refundAmount: Int?,
    // 아직 취소하지 않은 예약의 **예상** 환불액(서버 계산).
    //  ★ 앱에서 위약금을 다시 계산하지 않는다 — 규정이 바뀌면 서버만 고치면 되게.
    val expectedCancelFee: Int?,
    val expectedRefundAmount: Int?,
    val date: String?,
    val startTime: String?,
    val endTime: String?,
    val freeCancelDays: Int,
    val lesson: LessonResPlace,
    val owner: LessonResPerson,
    val member: LessonResPerson,
) {
    val dateLine: String
        get() = listOf(
            LessonDateFmt.krDate(date.orEmpty()),
            startTime?.let { LessonTimeFmt.ampm(it) }.orEmpty(),
            "${headcount}명",
        ).filter { it.isNotEmpty() }.joinToString(" · ")

    companion object {
        fun from(o: JSONObject) = LessonReservationDetail(
            id = o.optInt("id"),
            status = o.stringOrNull("status"),
            headcount = o.optInt("headcount", 1),
            deposit = o.optInt("deposit"),
            paymentAmount = o.intOrNull("paymentAmount"),
            totalPrice = o.intOrNull("totalPrice"),
            remainingAmount = o.intOrNull("remainingAmount"),
            rescheduleCount = o.intOrNull("rescheduleCount"),
            disputeStatus = o.stringOrNull("disputeStatus"),
            cancelFee = o.intOrNull("cancelFee"),
            refundAmount = o.intOrNull("refundAmount"),
            expectedCancelFee = o.intOrNull("expectedCancelFee"),
            expectedRefundAmount = o.intOrNull("expectedRefundAmount"),
            date = o.stringOrNull("date"),
            startTime = o.stringOrNull("startTime"),
            endTime = o.stringOrNull("endTime"),
            freeCancelDays = o.optInt("freeCancelDays"),
            lesson = LessonResPlace.from(o.optJSONObject("lesson")),
            owner = LessonResPerson.from(o.optJSONObject("owner")),
            member = LessonResPerson.from(o.optJSONObject("member")),
        )
    }
}

/**
 * 예약 취소 사유 — 코드 문자열은 서버(CANCEL_REASON_CODES)·admin 과 **3레포 계약**.
 *  ⚠️ 값을 바꾸면 관리자 통계가 조용히 깨진다.
 *
 * ★ 종전 AOS 는 RESCHEDULE/CHANGE_OF_MIND/TEACHER_CANCELED 3개만 갖고 있었는데,
 *   그건 서버 DTO 에서 "이미 배포된 구버전 앱 호환 코드"로 남겨둔 것들이다.
 *   정본은 iOS 와 같은 아래 8개다(lesson-booking.dto.ts).
 */
enum class LessonCancelReason(val code: String, val label: String) {
    SCHEDULE_MISMATCH("SCHEDULE_MISMATCH", "일정이 맞지 않아요"),
    PERSONAL_CIRCUMSTANCES("PERSONAL_CIRCUMSTANCES", "개인 사정이 생겼어요"),
    AGREED_WITH_TEACHER("AGREED_WITH_TEACHER", "강사와 협의 후 취소해요"),
    LESSON_MISMATCH("LESSON_MISMATCH", "원하는 레슨과 달라요"),
    OTHER_LESSON("OTHER_LESSON", "다른 레슨을 선택했어요"),
    CONTACT_DIFFICULTY("CONTACT_DIFFICULTY", "강사와 연락이 어려워요"),
    COST_BURDEN("COST_BURDEN", "비용이 부담돼요"),
    OTHER("OTHER", "기타"),
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

    suspend fun cancel(reservationId: Int, reasonCode: String, reasonDetail: String? = null): Result<Unit> {
        val body = JSONObject().put("reasonCode", reasonCode)
        if (!reasonDetail.isNullOrEmpty()) body.put("reasonDetail", reasonDetail)
        return call("/lesson-reservations/$reservationId/cancel", "POST", body).map { }
    }

    /** 수업 처리 이의 신청 — 접수되면 확인 전까지 정산이 보류된다. */
    suspend fun openDispute(reservationId: Int, reason: String): Result<Unit> =
        call(
            "/lesson-reservations/$reservationId/disputes", "POST",
            JSONObject().put("reason", reason),
        ).map { }

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
