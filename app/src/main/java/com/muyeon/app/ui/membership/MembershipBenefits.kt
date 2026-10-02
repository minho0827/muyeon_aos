package com.muyeon.app.ui.membership

/**
 * 멤버십 한도(limits) → 혜택 문구. 웹(common/membershipBenefits.js)·iOS(MembershipBenefits.swift)와 같은 규칙.
 *
 * 한도의 원천은 서버(admin 멤버십 한도)다. 앱에 숫자를 박아두면 관리자가 바꿔도 옛말이 남는다.
 * -1 = 무제한, 0 = 이용 불가(줄을 뺀다).
 * 구 서버는 postingsByKind 가 없다 → postings 하나로 세 종류를 채운다.
 */
object MembershipBenefits {
    private val postingKinds = listOf("JOB" to "채용", "SUB" to "대타", "CASTING" to "캐스팅")

    /** isDancer: 무용수 활동 중에는 레슨 활동이 없다(2026-09-26) — 레슨·자동견적 줄을 뺀다. */
    fun lines(l: MembershipLimits?, isDancer: Boolean = false): List<String> {
        if (l == null) return emptyList()
        val out = mutableListOf<String>()
        postingLine(l)?.let { out += it }
        if (!isDancer) l.lessons?.takeIf { it != 0 }?.let {
            out += if (it < 0) "레슨 무제한" else "레슨 ${it}개"
        }
        l.resumeViews?.takeIf { it != 0 }?.let {
            out += if (it < 0) "이력서 열람 무제한" else "이력서 열람 월 ${it}건"
        }
        if (!isDancer) l.autoQuotes?.takeIf { it != 0 }?.let {
            out += if (it < 0) "자동견적 무제한" else "자동견적 월 ${it}건"
        }
        if ((l.boostWeight ?: 0) > 0) out += "목록 상단 노출"
        l.performanceDays?.takeIf { it != 0 }?.let {
            out += if (it < 0) "성과 분석 전체 기간" else "성과 분석 최근 ${it}일"
        }
        return out
    }

    /** 등급이 여는 기능 중 한도(숫자)가 아닌 것 — 혜택 목록 끝에 붙인다. 웹 TIER_EXTRA_BENEFITS 와 같다. */
    fun extras(tier: String?): List<String> = when (tier) {
        "PRO" -> listOf("상세페이지 이미지", "학원 운영 도구 (수강생·수강권·매출·시간표)")
        "BASIC", "STANDARD" -> listOf("상세페이지 이미지")
        else -> emptyList()
    }

    /** 성과 화면 기간 문구 — 응답 days(-1 전체 기간), 없으면(구 서버) 30일. */
    fun performancePeriod(days: Int?): String = when {
        days != null && days < 0 -> "전체 기간"
        days != null && days > 0 -> "최근 ${days}일"
        else -> "최근 30일"
    }

    private fun postingLine(l: MembershipLimits): String? {
        val values = postingKinds.map { (k, _) -> l.postingsByKind?.get(k) ?: l.postings }
        if (values.any { it == null }) return null
        val known = values.filterNotNull()
        val first = known.first()
        if (known.all { it == first }) {
            if (first == 0) return null
            return if (first < 0) "공고 무제한" else "공고 종류별 ${first}개씩"
        }
        return postingKinds.zip(known).joinToString(" · ") { (kind, v) ->
            if (v < 0) "${kind.second} 무제한" else "${kind.second} $v"
        }
    }
}
