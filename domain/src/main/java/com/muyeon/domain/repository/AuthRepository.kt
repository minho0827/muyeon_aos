package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.auth.LoginSession

/** 로그인·아이디 찾기·비밀번호 재설정·약관 동의(auth.controller.ts). */
interface AuthRepository {
    suspend fun login(email: String, password: String): ApiResult<LoginSession>
    /** 가입한 이메일. */
    suspend fun findId(name: String, phone: String): ApiResult<String>
    /** 비밀번호 재설정 인증번호 메일(purpose RESET). */
    suspend fun sendResetCode(email: String): ApiResult<Unit>
    suspend fun resetPassword(email: String, code: String, newPassword: String): ApiResult<Unit>
    suspend fun agreeTerms(agreements: Map<String, Boolean>): ApiResult<Unit>
}
