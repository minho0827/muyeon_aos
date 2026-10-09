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
            out += if (it < 0) "레슨 마음껏 올리기" else "레슨 ${it}개까지 올리기"
        }
        // 이력서 열람은 무료라 혜택 목록에 넣지 않는다(2026-10-07).
        if (!isDancer) l.autoQuotes?.takeIf { it != 0 }?.let {
            out += if (it < 0) "견적 자동으로 보내기 (제한 없이)" else "견적 자동으로 보내기 (한 달 ${it}건)"
        }
        if ((l.boostWeight ?: 0) > 0) out += "목록에서 위쪽에 보이기"
        l.performanceDays?.takeIf { it != 0 }?.let {
            out += if (it < 0) "전체 기간 내 활동 통계 보기" else "최근 ${it}일 내 활동 통계 보기"
        }
        return out
    }

    /** 등급이 여는 기능 중 한도(숫자)가 아닌 것 — 혜택 목록 끝에 붙인다. 웹 TIER_EXTRA_BENEFITS 와 같다.
     *  서버 문구(MembershipCopy.extras)가 없을 때 쓰는 기본값이다. */
    fun extras(tier: String?): List<String> = when (tier) {
        "PRO" -> listOf("레슨 소개를 이미지로 꾸미기", "학원 관리 기능 (수강생·수강권·매출·시간표)")
        "BASIC", "STANDARD" -> listOf("레슨 소개를 이미지로 꾸미기")
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
            return if (first < 0) "채용·대타·캐스팅 공고 마음껏 올리기" else "채용·대타·캐스팅 공고 각각 ${first}개까지"
        }
        return postingKinds.zip(known).joinToString(" · ") { (kind, v) ->
            if (v < 0) "${kind.second} 무제한" else "${kind.second} $v"
        }
    }
}
