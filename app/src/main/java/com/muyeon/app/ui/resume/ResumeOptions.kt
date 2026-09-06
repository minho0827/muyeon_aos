package com.muyeon.app.ui.resume

/**
 * 이력서 선택지 상수 — iOS `ResumeOptions.swift` 1:1.
 *  웹(constants/jobOptions.js·timeSlots.js·subOptions.js)과 **값 계약**.
 *  ⚠️ 한쪽만 바꾸면 필터·매칭이 조용히 깨진다. 변경 시 웹·iOS·AOS 세 곳을 함께 수정.
 */
object ResumeOptions {

    val weekDays = listOf("월", "화", "수", "목", "금", "토", "일")

    /** 수업 가능 시간대 — 견적 문진(time)과 동일 키. */
    val timeSlots = listOf(
        "dawn" to "이른오전", "morning" to "오전", "noon" to "오후",
        "afternoon" to "늦은오후", "evening" to "저녁", "night" to "늦은저녁",
    )

    /** 전공/장르 — 웹 GENRES 와 동일. */
    val genres = listOf("발레", "한국무용", "현대무용", "실용무용", "바레", "발레핏", "뮤지컬")

    /** 경력 버킷 — CareerLevel enum. */
    val careerLevels = listOf(
        "NEW" to "신입", "Y1_3" to "1~3년", "Y3_5" to "3~5년",
        "Y5_10" to "5~10년", "Y10" to "10년 이상",
    )

    /** 급여 버킷 — SalaryRange enum(시급). */
    val salaryRanges = listOf(
        "W1_2" to "1만원~2만원 미만", "W2_3" to "2만원~3만원 미만",
        "W3_4" to "3만원~4만원 미만", "W4_5" to "4만원~5만원 미만",
        "W5_6" to "5만원~6만원 미만", "NEGOTIABLE" to "추후 협의",
    )

    /** 수업 분야 — 웹 TEACHING_FIELDS 와 1:1. */
    val teachingFields = listOf(
        "BALLET_KIDS" to "유아발레", "BALLET_ELEM" to "초등발레", "BALLET_ADULT" to "성인취미발레",
        "BALLET_EXAM" to "입시발레", "BALLET_MAJOR" to "전공반", "CONCOURS" to "콩쿠르 작품지도",
        "MODERN" to "현대무용", "KOREAN" to "한국무용", "STRETCH" to "스트레칭",
        "PILATES" to "필라테스", "BALLET_FIT" to "발레핏", "BARRE" to "바레",
    )

    /** 수업 대상 — 웹 CLASS_TARGETS 와 1:1. */
    val classTargets = listOf(
        "KIDS" to "유아", "ELEM" to "초등", "TEEN" to "중고등", "ADULT" to "성인", "EXAM" to "입시생",
    )

    /** 공개 프로필 시안 그리드: 6버킷 → 오전/오후/저녁 3행 매핑. */
    val gridRows = listOf(
        "오전" to listOf("dawn", "morning"),
        "오후" to listOf("noon", "afternoon"),
        "저녁" to listOf("evening", "night"),
    )

    fun fieldLabel(v: String) = teachingFields.firstOrNull { it.first == v }?.second ?: v
    fun targetLabel(v: String) = classTargets.firstOrNull { it.first == v }?.second ?: v
    fun careerLabel(v: String?) = careerLevels.firstOrNull { it.first == v }?.second ?: (v ?: "")
    fun salaryLabel(v: String?) = salaryRanges.firstOrNull { it.first == v }?.second ?: (v ?: "")
    fun timeSlotLabel(v: String) = timeSlots.firstOrNull { it.first == v }?.second ?: v
}

/** 이력서 편집/목록 모드 — 강사(기본)와 무용수(간소화+포트폴리오)를 같은 스택으로 처리. */
enum class ResumeMode(val raw: String) {
    TEACHER("teacher"),   // 희망조건·경력·레슨시간 등 전체
    DANCER("dancer");     // 희망조건·경력·레슨시간 제거 + 성별/키/무용단경력/포트폴리오(최대20)

    val navTitle: String get() = if (this == DANCER) "무용수 이력서" else "이력서 작성"
    val listTitle: String get() = if (this == DANCER) "무용수 이력서 관리" else "이력서 관리"
    val defaultResumeTitle: String get() = if (this == DANCER) "무용수 프로필" else "이력서"
    val isDancer: Boolean get() = this == DANCER
    /** 서버 태그(resumes.service.normalizeRole) — 목록 필터/저장 공통. */
    val roleIntent: String get() = if (this == DANCER) "DANCER" else "TEACHER"

    companion object {
        fun from(s: String?) = if (s == "dancer") DANCER else TEACHER
    }
}
