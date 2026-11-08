package com.muyeon.domain.usecase.auth

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.auth.LoginSession
import com.muyeon.domain.repository.AuthRepository

/**
 * 이메일 로그인 — 웹 pages/login/Login.js submit.
 *   빈 칸이면 보내지 않는다([check]). 이메일 앞뒤 공백은 지운다(아이폰 자동완성이 붙인 공백으로 실패했다 — 2026-10-04).
 *   실패 문구는 서버 message 그대로(ApiResult.Error.message).
 */
class EmailLoginUseCase(private val repo: AuthRepository) {
    fun check(email: String, password: String): Boolean = email.isNotBlank() && password.isNotEmpty()

    suspend operator fun invoke(email: String, password: String): ApiResult<LoginSession> = repo.login(email.trim(), password)
}

/** 아이디(가입 이메일) 찾기 — 웹 AccountRecovery ?type=id(이름 + 휴대폰). */
class FindIdUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(name: String, phone: String): ApiResult<String> = repo.findId(name.trim(), phone.trim())
}

/**
 * 비밀번호 재설정 — 웹 AccountRecovery ?type=pw(이메일 인증번호 → 새 비밀번호).
 *   검사 순서·기준은 웹 그대로: 이메일 형식 → 인증번호 6자리 → 8자 이상 → 확인 일치.
 */
class ResetPasswordUseCase(private val repo: AuthRepository) {
    enum class Invalid { EMAIL, CODE, LENGTH, MISMATCH }

    fun checkEmail(email: String): Invalid? = if (EMAIL_RE.matches(email)) null else Invalid.EMAIL

    fun check(code: String, password: String, confirm: String): Invalid? = when {
        code.length < CODE_LENGTH -> Invalid.CODE
        password.length < MIN_PASSWORD -> Invalid.LENGTH
        password != confirm -> Invalid.MISMATCH
        else -> null
    }

    suspend fun sendCode(email: String): ApiResult<Unit> = repo.sendResetCode(email)

    suspend operator fun invoke(email: String, code: String, password: String): ApiResult<Unit> = repo.resetPassword(email, code, password)

    companion object {
        const val CODE_LENGTH = 6
        const val MIN_PASSWORD = 8
        /** 웹 AccountRecovery EMAIL_RE 와 같다. */
        private val EMAIL_RE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}

/** 약관 동의 저장(게이트) — 웹 AppRouter __onSignupTermsAgreed(mode gate) → POST /auth/terms/agree. */
class AgreeTermsUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(agreements: Map<String, Boolean>): ApiResult<Unit> = repo.agreeTerms(agreements)
}
