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

/** 회원탈퇴 안내(GET /auth/me/withdrawal-info) — 화면이 보여줄 값은 전부 서버가 판정한다(웹 Withdraw.js). */
data class WithdrawalInfo(
    val nickname: String,
    /** true 면 비밀번호, false 면 [confirmText] 입력으로 확인(소셜 가입). */
    val requiresPassword: Boolean,
    val confirmText: String,
    val provider: String?,
    val retentionMonths: Int,
    val rejoinableFrom: String?,
    /** 먼저 정리해야 탈퇴할 수 있는 내역 — 있으면 진행 버튼이 잠긴다. [WithdrawalNotice.action] 은 정리하러 갈 주소. */
    val blockers: List<WithdrawalNotice>,
    val warnings: List<WithdrawalNotice>,
    /** 서버가 대신 취소하는 내역 — 있으면 동의 하나가 더 붙는다. */
    val autoCancel: List<WithdrawalNotice>,
    /** 삭제 대상 건수(posts·comments·chatRooms·reviews·resumes·postings·spaces). */
    val summary: Map<String, Int>,
    val reasonGroups: List<WithdrawalReasonGroup>,
)

data class WithdrawalNotice(val code: String, val label: String, val detail: String?, val action: String?)
data class WithdrawalReasonGroup(val key: String, val title: String, val items: List<Pair<String, String>>)

/** 탈퇴 결과 — 성공이면 재가입 가능일, 화면을 띄운 뒤 정리할 내역이 생겼으면 [Blocked]. */
sealed interface WithdrawResult {
    data class Done(val rejoinableFrom: String?) : WithdrawResult
    data object Blocked : WithdrawResult
}
