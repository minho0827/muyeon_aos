package com.muyeon.domain.model.member

/**
 * 활성 회원유형(서버 `X-Active-Type` 값). 웹 constants/permissions.js · ActiveTypeSheet.js 와 같은 규칙.
 *
 *   PRO(강사·무용수·학원·공연팀)는 탭이 [홈·구인구직·캐스팅·대타공고·MY], 그 밖(일반·취미·공간·비회원)은
 *   [홈·강사·커뮤니티·공연·MY] 다. SPACE 유형은 폐기(SPACE_FEATURE_ENABLED=false) — 일반으로 본다.
 */
object ActiveType {
    const val GENERAL = "GENERAL"
    const val TEACHER = "TEACHER"
    const val DANCER = "DANCER"
    const val ACADEMY = "ACADEMY"
    const val TEAM = "TEAM"
    const val HOBBY = "HOBBY"
    const val SPACE = "SPACE"

    private val PRO = setOf(TEACHER, DANCER, ACADEMY, TEAM)

    fun isPro(type: String?): Boolean = type?.uppercase() in PRO

    /** 화면에서 쓰는 유형 — HOBBY·SPACE·null 은 GENERAL(웹 visibleType). */
    fun visible(type: String?): String = when (val t = type?.uppercase()) {
        null, HOBBY, SPACE -> GENERAL
        else -> t
    }
}
