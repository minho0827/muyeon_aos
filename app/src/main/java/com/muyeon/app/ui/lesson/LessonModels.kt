package com.muyeon.app.ui.lesson

import com.muyeon.app.ui.quote.boolOrNull
import com.muyeon.app.ui.quote.doubleOrNull
import com.muyeon.app.ui.quote.intOrNull
import com.muyeon.app.ui.quote.stringOrNull
import com.muyeon.app.ui.quote.QuoteUi
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 레슨 일정 모델 — iOS `LessonModels.swift` 1:1.
 *  채택 시 백엔드가 PENDING(날짜 미정) 1건을 자동 생성 → 강사/고객이 날짜·장소를 확정(SCHEDULED).
 */

data class LessonPartner(
    val id: Int,
    val name: String?,
    val nickname: String?,
    val image: String?,
    val service: String?,
) {
    val displayName: String get() = nickname ?: name ?: "상대"

    companion object {
        fun from(o: JSONObject?) = LessonPartner(
            o?.optInt("id") ?: 0, o?.stringOrNull("name"), o?.stringOrNull("nickname"),
            o?.stringOrNull("image"), o?.stringOrNull("service"),
        )
    }
}

/** 레슨 일정 변경 이력 — 캘린더 상세 활동 타임라인. */
data class LessonHistoryItem(
    val id: Int,
    val action: String,       // MATCHED | CREATED | UPDATED | CANCELED | DONE
    val actorName: String?,
    val isMe: Boolean,
    val startAt: String?,
    val place: String?,
    val createdAt: String?,
) {
    val actionLabel: String
        get() = when (action) {
            "MATCHED" -> "강사를 채택했어요"
            "CREATED" -> "레슨 일정을 등록했어요"
            "UPDATED" -> "레슨 일정을 변경했어요"
            "CANCELED" -> "레슨 일정을 취소했어요"
            "DONE" -> "레슨을 완료했어요"
            else -> "일정을 변경했어요"
        }

    companion object {
        fun from(o: JSONObject) = LessonHistoryItem(
            o.optInt("id"), o.optString("action"), o.stringOrNull("actorName"),
            o.optBoolean("isMe", false), o.stringOrNull("startAt"),
            o.stringOrNull("place"), o.stringOrNull("createdAt"),
        )
    }
}

/** 선택한 장소(검색/직접입력 결과). */
data class LessonPlace(
    var name: String = "",
    var address: String? = null,
    var lat: Double? = null,
    var lng: Double? = null,
) {
    val hasCoord: Boolean get() = lat != null && lng != null
}

data class LessonSchedule(
    val id: Int,
    val quoteId: Int?,        // 견적 채택건만 존재. 시간슬롯 예약(BOOKING)은 null
    val responseId: Int?,
    val source: String?,      // "QUOTE" | "BOOKING"
    val reservationId: Int?,  // BOOKING 일 때 원본 예약 id
    val roomId: Int?,
    val categoryId: String?,
    val service: String?,
    val startAt: String?,     // null = 날짜미정(PENDING)
    val endAt: String?,
    val place: String?,
    val placeAddress: String?,
    val placeLat: Double?,
    val placeLng: Double?,
    val memo: String?,
    val calendarId: Int?,     // 사용자 캘린더 배정(null=기본) — 색 매칭용
    val status: String,       // PENDING | SCHEDULED | DONE | CANCELED
    val createdAt: String?,
    val updatedAt: String?,
    val iAmTeacher: Boolean,
    val partner: LessonPartner,
) {
    private val genre: String get() = (service ?: categoryId).orEmpty()

    val serviceLabel: String get() = if (genre.isEmpty()) "레슨" else "$genre 레슨"
    val genreLabel: String get() = if (genre.isEmpty()) "레슨" else genre
    val isPending: Boolean get() = status == "PENDING" || startAt == null

    /** 시간슬롯 예약에서 생성된 일정(견적 아님) → 예약상세 진입 가능. */
    val isBooking: Boolean get() = source == "BOOKING" && reservationId != null

    val startMillis: Long? get() = QuoteUi.parseDate(startAt)
    val endMillis: Long? get() = QuoteUi.parseDate(endAt)

    /** 'yyyy-MM-dd' (KST) — 캘린더 셀 매칭용. */
    val ymdKST: String? get() = startMillis?.let { kstYmd.format(Date(it)) }

    companion object {
        fun from(o: JSONObject) = LessonSchedule(
            id = o.optInt("id"),
            quoteId = o.intOrNull("quoteId"), responseId = o.intOrNull("responseId"),
            source = o.stringOrNull("source"), reservationId = o.intOrNull("reservationId"),
            roomId = o.intOrNull("roomId"), categoryId = o.stringOrNull("categoryId"),
            service = o.stringOrNull("service"),
            startAt = o.stringOrNull("startAt"), endAt = o.stringOrNull("endAt"),
            place = o.stringOrNull("place"), placeAddress = o.stringOrNull("placeAddress"),
            placeLat = o.doubleOrNull("placeLat"), placeLng = o.doubleOrNull("placeLng"),
            memo = o.stringOrNull("memo"), calendarId = o.intOrNull("calendarId"),
            status = o.optString("status").ifEmpty { "PENDING" },
            createdAt = o.stringOrNull("createdAt"), updatedAt = o.stringOrNull("updatedAt"),
            iAmTeacher = o.optBoolean("iAmTeacher", false),
            partner = LessonPartner.from(o.optJSONObject("partner")),
        )
    }
}

/** 레슨 인연(수강생) — 캘린더발 약속잡기 목록 항목. */
data class LessonPartnerSummary(
    val userId: Int,
    val name: String?,
    val nickname: String?,
    val image: String?,
    val lastLessonAt: String?,
    val lastService: String?,
    val roomId: Int?,
) {
    val displayName: String get() = nickname ?: name ?: "회원"

    companion object {
        fun from(o: JSONObject) = LessonPartnerSummary(
            o.optInt("userId"), o.stringOrNull("name"), o.stringOrNull("nickname"),
            o.stringOrNull("image"), o.stringOrNull("lastLessonAt"),
            o.stringOrNull("lastService"), o.intOrNull("roomId"),
        )
    }
}

/**
 * KST 'yyyy-MM-dd' 포매터 — iOS 는 정적 공유로 캘린더 렌더의 수천 회 할당(실측 180ms 렉)을 없앴다.
 *  Android 도 같은 이유로 최상위 단일 인스턴스.
 *  ⚠️ SimpleDateFormat 은 스레드 세이프가 아니므로 UI 스레드에서만 쓴다.
 */
internal val kstYmd: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("Asia/Seoul")
}
