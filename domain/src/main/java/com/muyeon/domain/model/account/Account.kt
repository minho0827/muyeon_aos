package com.muyeon.domain.model.account

/**
 * 개인정보 수정 화면의 내 정보(GET /auth/me).
 *   [nameLocked] 본인확인을 마쳐 이름이 인증 실명으로 고정(서버가 변경을 막는다). 휴대폰은 본인확인으로만 바뀐다.
 */
data class MyAccount(
    val email: String,
    val name: String,
    val phone: String,
    val image: String?,
    val memberTypes: List<String>,
    val nameLocked: Boolean,
    val phoneVerified: Boolean,
)

/** 대타 신뢰도(최근 90일) — [restrictedUntil] 이 지금보다 뒤면 긴급 대타 이용 정지. */
data class SubTrust(val points90d: Int, val cancels90d: Int, val restrictedUntil: String?, val history: List<SubTrustEntry>)

/** 신뢰도 내역 — [side] TEACHER(강사로)·POSTER(공고 올린 사람으로), [noShow] 노쇼 신고(아니면 확정 취소). */
data class SubTrustEntry(
    val id: Long, val createdAt: String?, val postingTitle: String?, val side: String?, val noShow: Boolean,
    val points: Int, val revoked: Boolean, val expiresAt: String?,
)
