package com.muyeon.domain.model.academy

/**
 * ★ 2026-11-08 묶음 2 — 학원 정보 관리(웹 pages/muyeon/my/AcademyProfile.js 의 form) — GET·PATCH /auth/me/academy 칸 그대로.
 *   [academyImages] 첫 장이 대표 · [academyKeywords] 는 화면에서 쉼표 글로 고친다(저장 때 최대 8개로 자른다).
 */
data class AcademyProfileForm(
    val academyName: String = "",
    val academyImages: List<String> = emptyList(),
    val academyGenres: List<String> = emptyList(),
    val academyKeywords: List<String> = emptyList(),
    val academyRegion: String = "",
    val academyAddress: String = "",
    val academyBusinessHours: String = "",
    val academyFacilities: List<String> = emptyList(),
    val academyParkingInfo: String = "",
    val academyHomepage: String = "",
    val academyIntro: String = "",
    val wantGenres: List<String> = emptyList(),
    val wantFields: List<String> = emptyList(),
)

/** 대표원장(본인확인 실명 — 고칠 수 없다) · 내 회원 번호(= 학원 공개 프로필 번호). */
data class AcademyOwner(val id: Long?, val name: String)

/** 미리보기 '수업 안내' 한 칸 — GET /academies/:id 의 lessons. */
data class AcademyLinkedLesson(val id: Long, val title: String?, val image: String?, val genre: String?, val scheduleText: String?)
