package com.muyeon.app.ui.chat.proposal

import org.json.JSONObject

/**
 * ★ 2026-11-27 견적에 적힌 수업 조건 — GET /chat/rooms/:id 의 quoteContext.lessonTerms.
 *  수업 길이는 강사가 견적서에 적는다. 장소는 수업하는 곳 주인([placeOwner])이 정한다:
 *   MEMBER = 회원이 있는 곳(회원이 입력) · TEACHER = 강사가 견적서에 적음(회원 화면엔 읽기 전용).
 *  예전 견적·구서버는 값이 없다(null) → 지금처럼 회원도 고른다.
 */
data class LessonTerms(
    val durationMin: Int? = null,
    val place: String? = null,
    val placeAddress: String? = null,
    val placeLat: Double? = null,
    val placeLng: Double? = null,
    val placeOwner: String? = null,
) {
    /** 회원 약속잡기에서 고정(읽기 전용)되는 수업 길이 — 없으면 회원이 고른다. */
    val memberFixedDuration: Int? get() = durationMin?.takeIf { it > 0 }

    /** 회원 약속잡기에서 고정되는 장소 — 강사가 정하는 장소이고 견적에 적혀 있을 때만. */
    val memberFixedPlace: String? get() = place?.trim()?.takeIf { placeOwner == TEACHER && it.isNotEmpty() }

    companion object {
        const val TEACHER = "TEACHER"
        const val MEMBER = "MEMBER"

        fun from(o: JSONObject?): LessonTerms? = o?.let {
            LessonTerms(
                durationMin = it.num("durationMin")?.toInt(),
                place = it.str("place"),
                placeAddress = it.str("placeAddress"),
                placeLat = it.num("placeLat"),
                placeLng = it.num("placeLng"),
                placeOwner = it.str("placeOwner"),
            )
        }

        private fun JSONObject.str(k: String): String? = if (has(k) && !isNull(k)) optString(k).takeIf { s -> s.isNotBlank() } else null
        private fun JSONObject.num(k: String): Double? = if (has(k) && !isNull(k)) optDouble(k).takeIf { d -> !d.isNaN() } else null
    }
}
