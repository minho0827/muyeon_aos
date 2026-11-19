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
