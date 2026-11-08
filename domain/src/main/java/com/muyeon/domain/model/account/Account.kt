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
