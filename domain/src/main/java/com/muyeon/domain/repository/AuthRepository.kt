package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.auth.LoginSession

/** 로그인·아이디 찾기·비밀번호 재설정·약관 동의·가입·본인확인·가입 직후 회원유형(auth · identity · roles controller). */
interface AuthRepository {
    suspend fun login(email: String, password: String): ApiResult<LoginSession>
    /** 가입한 이메일. */
    suspend fun findId(name: String, phone: String): ApiResult<String>
    /** 비밀번호 재설정 인증번호 메일(purpose RESET). */
    suspend fun sendResetCode(email: String): ApiResult<Unit>
    suspend fun resetPassword(email: String, code: String, newPassword: String): ApiResult<Unit>
    suspend fun agreeTerms(agreements: Map<String, Boolean>): ApiResult<Unit>

    // ★ 2026-11-08 [b1] 이메일 가입·소셜 추가정보(웹 Register.js · SocialSignup.js)
    /** 가입 인증번호 메일(purpose SIGNUP). */
    suspend fun sendSignupCode(email: String): ApiResult<Unit>
    suspend fun verifySignupCode(email: String, code: String): ApiResult<Unit>
    /** 가입 = 바로 로그인(서버 issueToken). */
    suspend fun register(email: String, password: String, name: String, phone: String?, identityToken: String?, agreements: Map<String, Any>): ApiResult<LoginSession>
    /** 소셜 추가정보 저장 — 갱신된 user(JSON 문자열, 웹 저장소에 그대로 넣는다). */
    suspend fun completeSocial(name: String, identityToken: String?): ApiResult<String>
    suspend fun niceConfig(): ApiResult<com.muyeon.domain.model.auth.NiceConfig>
    /** 가입 전 본인확인 표준창 주소 — 끝나면 [returnTo]?niceToken=… / ?niceError=… 로 돌아온다. */
    suspend fun startNice(returnTo: String): ApiResult<String>
    suspend fun niceResult(token: String): ApiResult<com.muyeon.domain.model.auth.NiceIdentity>
    suspend fun addRole(role: String): ApiResult<Unit>
    /** 서류 제출 — true = 자동 승인(학원 사업자번호 진위확인). */
    suspend fun submitRoleVerification(role: String, urls: List<String>, academyName: String?, businessNumber: String?, startDate: String?): ApiResult<Boolean>
}
