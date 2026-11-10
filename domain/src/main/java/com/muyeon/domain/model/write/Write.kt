package com.muyeon.domain.model.write

/**
 * ★ 2026-11-08 묶음 3 — 대타공고 수정 · 커뮤니티 글쓰기/수정(웹 SubCreate 수정 모드 · CommunityWrite).
 */

/** 대타공고 수정 화면을 채우는 값(GET /subs/:id). 빈 값은 null. 웹 SubCreate 수정 모드가 읽는 칸과 같다. */
data class SubEditForm(
    val title: String,
    val academy: String?,
    val region: String?,
    val regionCode: String?,
    val genre: String?,
    val target: String?,
    val fields: List<String>,
    val scheduleType: String?,
    val classDate: String?,
    /** 'HH:mm~HH:mm'(사이 공백 있을 수 있음). */
    val classTime: String?,
    val classCount: Int?,
    val salary: String?,
    val career: String?,
    val description: String?,
    val image: String?,
    /** 이미 긴급 발송 중인 공고(끌 수 없음) — 급여 칸 대신 타임당 금액(서버가 급여 글자를 채운다). */
    val dispatchEnabled: Boolean,
    /** 확정된 공고 — 급여를 바꿀 수 없다(서버 SUB_PAY_LOCKED). */
    val payLocked: Boolean,
)

/**
 * 대타공고 수정 본문(PATCH /subs/:id) — 웹 SubCreate buildPayload(isEdit). null 칸은 보내지 않는다(서버가 기존 값 유지).
 *   급여만 예외: [sendSalary] 면 비운 값도 null 로 보내 지운다(웹 `salary: form.salary || null`). 긴급 공고는 급여를 보내지 않는다.
 */
data class SubUpdate(
    val title: String,
    val academy: String?,
    val genre: String,
    val target: String,
    val fields: List<String>,
    val scheduleType: String?,
    /** 날짜 없는 글이면 null(빈 글자를 보내지 않는다). */
    val classDate: String?,
    val classTime: String?,
    val classCount: Int?,
    val region: String?,
    val regionCode: String?,
    val salary: String?,
    val sendSalary: Boolean,
    val career: String?,
    val description: String?,
    val image: String?,
)

/** 커뮤니티 글(POST·PATCH /community/posts) — 웹 CommunityWrite payload. 사진은 올린 주소(최대 10장). */
data class CommunityDraft(
    val category: String,
    val title: String,
    val body: String,
    val images: List<String>,
)
