package com.muyeon.network.dto.auth

import com.squareup.moshi.JsonClass

// ★ 2026-11-08 [b1] 이메일 가입·소셜 추가정보·본인확인·가입 직후 회원유형 — 웹 api/MuyeonApi.js 와 같은 몸통.

/** POST /auth/email/verify-code — purpose SIGNUP. */
@JsonClass(generateAdapter = true)
data class VerifyEmailCodeBody(val email: String, val code: String, val purpose: String)

/**
 * POST /auth/register — 웹 Register.submit 의 register({...}).
 *   본인확인을 했으면 [phone] 은 보내지 않는다(서버가 토큰에서 꺼낸다). [agreements] 는 웹 toAgreements 모양(version 포함).
 */
@JsonClass(generateAdapter = true)
data class RegisterBody(
    val email: String,
    val password: String,
    val name: String,
    val phone: String? = null,
    val identityToken: String? = null,
    val agreements: Map<String, Any>,
)

/** PATCH /auth/me/social-complete */
@JsonClass(generateAdapter = true)
data class SocialCompleteBody(val name: String, val identityToken: String? = null)

/** GET /identity/nice/config */
@JsonClass(generateAdapter = true)
data class NiceConfigDto(val available: Boolean? = null, val required: Boolean? = null)

/** GET /identity/nice/result — 이름은 전부, 휴대폰은 가운데를 가린 표시용. */
@JsonClass(generateAdapter = true)
data class NiceResultDto(val name: String? = null, val phone: String? = null)

/** POST /me/roles */
@JsonClass(generateAdapter = true)
data class AddRoleBody(val role: String)

@JsonClass(generateAdapter = true)
data class RoleDocumentBody(val type: String, val url: String)

/** POST /me/roles/{role}/verification — 웹 callSubmitRoleVerification(빈 값은 보내지 않는다). */
@JsonClass(generateAdapter = true)
data class RoleVerificationBody(
    val documents: List<RoleDocumentBody>,
    val academyName: String? = null,
    val businessRegistrationNumber: String? = null,
    val businessStartDate: String? = null,
)

@JsonClass(generateAdapter = true)
data class RoleVerificationResultDto(val autoApproved: Boolean? = null)
