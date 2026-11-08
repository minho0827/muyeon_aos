package com.muyeon.network.dto.auth

import com.squareup.moshi.JsonClass

/** POST /auth/login — 서버 LoginDto(이메일 앞뒤 공백은 서버도 지운다). */
@JsonClass(generateAdapter = true)
data class LoginBody(val email: String, val password: String)

/**
 * POST /auth/login 응답(issueToken) — `user` 는 웹이 localStorage muyeonAuth 에 그대로 두는 공개 회원 정보라
 *   칸을 고르지 않고 통째로 받는다(앱 웹 창에도 같은 값을 넣는다 — WebLogin).
 */
@JsonClass(generateAdapter = true)
data class LoginResponseDto(
    val accessToken: String? = null,
    val sessionId: String? = null,
    val user: Map<String, Any?>? = null,
)

/** POST /auth/find-id */
@JsonClass(generateAdapter = true)
data class FindIdBody(val name: String, val phone: String)

@JsonClass(generateAdapter = true)
data class FindIdDto(val email: String? = null)

/** POST /auth/email/send-code — purpose RESET(비밀번호 재설정). */
@JsonClass(generateAdapter = true)
data class SendEmailCodeBody(val email: String, val purpose: String)

/** POST /auth/reset-password-email */
@JsonClass(generateAdapter = true)
data class ResetPasswordEmailBody(val email: String, val code: String, val newPassword: String)

/** POST /auth/terms/agree — 웹 callAgreeTerms 와 같은 { agreements }. */
@JsonClass(generateAdapter = true)
data class TermsAgreeBody(val agreements: Map<String, Boolean>)
