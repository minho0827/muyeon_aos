package com.muyeon.app.ui.quote.respond

import org.json.JSONObject

/**
 * ★ 2026-11-27 견적서에 적는 수업 조건 — 회당 수업 길이(필수) · 장소(장소 주인이 강사일 때만, 선택).
 *  POST /quotes/:id/responses 본문에 durationMin · place 로 더한다. 회원이 약속잡기 할 때 이 값으로 고정된다.
 */
data class QuoteLessonTermsInput(val durationMin: Int?, val place: String? = null) {
    fun putInto(body: JSONObject): JSONObject {
        durationMin?.let { body.put("durationMin", it) }
        place?.trim()?.takeIf { it.isNotEmpty() }?.let { body.put("place", it.take(PLACE_MAX)) }
        return body
    }

    companion object {
        const val PLACE_MAX = 100

        /** 장소 칸을 보일지 — 견적 요청 상세의 placeOwner 가 TEACHER 일 때만(구서버 null 은 안 보인다). */
        /** 회당 수업 길이 필수 여부 — 학원 요청(채택 = 채용, 레슨 약속 없음)만 선택. 보내기·고치기 같은 규칙. */
        fun durationRequired(academyRequest: Boolean): Boolean = !academyRequest

        fun asksPlace(placeOwner: String?): Boolean = placeOwner == com.muyeon.app.ui.chat.proposal.LessonTerms.TEACHER
    }
}
