package com.muyeon.domain.model.member

/**
 * ★ 2026-11-16 멤버십 성과 화면 문구 표 — 학원 / 개인(강사·레슨 제공 무용수).
 *   예전엔 `memberType == "ACADEMY"` 하나로 갈라 무용수 제공자도 "강사 성과"·"강사님"으로 보였다.
 */
data class PerformanceCopy(
    val isAcademy: Boolean,
    /** 화면 제목 — 학원 성과 / 강사 성과 / 무용수 성과. */
    val screenTitle: String,
    /** 문의 지표 이름(짧게) — 상담·예약 / 견적요청·채팅. */
    val leadMetric: String,
    /** 문의 지표 이름(카드·증감) — 상담문의·예약 / 견적요청·채팅. */
    val leadPill: String,
    /** 가장 많이 는 지표 후보 이름 — 상담문의 / 견적요청·채팅. */
    val leadGrowth: String,
    /** 단계 1 — 학원 목록 노출 / 강사 목록 노출. */
    val impressionStep: String,
    /** 단계 4 — 상담문의·예약 / 레슨 견적요청·채팅. */
    val leadStep: String,
    val headline: String,
    /** "… 실제 ○○으로 이어진 흐름" 의 ○○. */
    val flowTail: String,
    val funnelTitle: String,
    val funnelDesc: String,
    val tableTitle: String,
    /** 늘어난 게 없을 때 — "결제 후 ○○을 이만큼 찾아봤어요" 의 ○○. */
    val foundWho: String,
) {
    companion object {
        private val ACADEMY = PerformanceCopy(
            isAcademy = true, screenTitle = "학원 성과", leadMetric = "상담·예약", leadPill = "상담문의·예약", leadGrowth = "상담문의",
            impressionStep = "학원 목록 노출", leadStep = "상담문의·예약",
            headline = "학원을 찾은 회원의 반응이에요", flowTail = "문의와 예약",
            funnelTitle = "상담문의까지 오는 과정",
            funnelDesc = "학원 목록에 뜨고 → 카드를 누르고 → 학원 프로필을 보고 → 상담문의·예약까지 간 횟수예요.",
            tableTitle = "문의·예약·채용", foundWho = "학원",
        )

        private fun personal(noun: String, profile: String) = PerformanceCopy(
            isAcademy = false, screenTitle = "$noun 성과", leadMetric = "견적요청·채팅", leadPill = "견적요청·채팅", leadGrowth = "견적요청·채팅",
            impressionStep = "강사 목록 노출", leadStep = "레슨 견적요청·채팅",
            headline = "회원들이 ${noun}님을 찾고 있어요", flowTail = "견적과 레슨",
            funnelTitle = "레슨 견적요청까지 오는 과정",
            funnelDesc = "강사 목록에 뜨고 → 카드를 누르고 → ${profile}을 보고 → 레슨 견적요청·채팅까지 간 횟수예요.",
            tableTitle = "견적부터 재레슨까지", foundWho = "${noun}님",
        )

        private val TEACHER = personal("강사", "강사프로필")
        private val DANCER = personal("무용수", "무용수 프로필")

        /** 성과를 볼 유형으로 고른다 — 학원 · 무용수 · 그 밖(강사). */
        fun of(memberType: String?): PerformanceCopy = when (ActiveType.visible(memberType)) {
            ActiveType.ACADEMY -> ACADEMY
            ActiveType.DANCER -> DANCER
            else -> TEACHER
        }
    }
}
