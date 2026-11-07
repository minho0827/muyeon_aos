package com.muyeon.domain.model.my

/** 멤버십 혜택 사용량 한 줄(monetization membership/me benefitUsage). limit<0 = 무제한. */
data class BenefitUsage(val key: String?, val label: String?, val kind: String?, val limit: Int?, val used: Int?)

data class MyMembership(val active: Boolean, val tier: String?, val benefitUsage: List<BenefitUsage>)

/** 인증 전이라 비공개인 내 공고(일반 회원) — `GET /me/pending-postings/summary`. */
data class PendingPostings(val total: Int, val roles: List<PendingRole>) {
    /** 모든 유형이 심사 중(PENDING) — '인증 확인 중' 안내. */
    val reviewing: Boolean get() = roles.isNotEmpty() && roles.all { it.status == "PENDING" }
}

data class PendingRole(val role: String, val status: String?)

/** MY 화면이 한 번에 읽는 값(실패한 칸은 기본값 — 부가 정보라 화면을 막지 않는다). */
data class MyPageInfo(
    val name: String?,
    val notiUnread: Int,
    val quoteUnread: Int,
    val membership: MyMembership?,
    val pending: PendingPostings?,
    val castingMaterialAllowed: Boolean,
    val supportChatEligible: Boolean,
)
