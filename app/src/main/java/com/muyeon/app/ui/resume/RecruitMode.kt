package com.muyeon.app.ui.resume

/**
 * 강사 상세의 채용 모드(하단 [채팅하기]) 판정 — 웹 nativeBridge.openPublicProfile 과 같은 규칙.
 *  ★ 2026-11-19 원장·공연팀 유형이면 어느 화면에서 열든 채용 모드. 강사 목록(LessonBrowseScreen)·견적·채팅 등
 *   네이티브 화면은 recruit 를 넘기지 않아 원장도 [레슨 문의]만 보였다(무연 계정 제보).
 */
object RecruitMode {
    val RECRUITER_TYPES = setOf("ACADEMY", "TEAM")

    fun of(requested: Boolean, activeType: String?): Boolean =
        requested || activeType?.uppercase() in RECRUITER_TYPES
}

/**
 * ★ 2026-11-26 원장은 강사·무용수에게 채팅을 바로 걸지 않고 [레슨요청]을 먼저 보낸다 — 웹 common/academyLessonRequest · iOS 와 같은 규칙.
 *  상대가 요청을 수락(견적 응답)하면 서버가 채팅방을 연다. 이미 방이 있으면 [채팅하기]로 그 방을 연다.
 *  원장 = ACADEMY 보유 + 지금 활동 유형이 공연팀(TEAM)이 아님. 공연팀은 지금처럼 [채팅하기]·캐스팅 제안.
 *  보유 유형을 아직 못 읽었으면(null) 활동 유형이 ACADEMY 인지만 본다.
 *  서버 스위치 ACADEMY_CHAT_REQUIRES_LESSON_REQUEST 가 켜지면 바로 채팅 열기는 403 LESSON_REQUEST_REQUIRED.
 */
object AcademyLessonRequest {
    const val REQUIRED_CODE = "LESSON_REQUEST_REQUIRED"

    fun isAcademy(
        activeType: String?,
        held: List<String>? = com.muyeon.app.webview.ActiveRole.heldTypes(),
    ): Boolean {
        val active = activeType?.uppercase().orEmpty()
        if (active == "TEAM") return false
        if (held != null) return held.any { it.uppercase() == "ACADEMY" }
        return active == "ACADEMY"
    }
}

/** 바로 채팅 열기가 '레슨요청 먼저'로 막혔다(서버 403 LESSON_REQUEST_REQUIRED). */
class LessonRequestRequiredException(message: String?) : IllegalStateException(message ?: "레슨요청을 먼저 보내 주세요.")
