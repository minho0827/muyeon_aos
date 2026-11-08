package com.muyeon.domain.usecase.auth

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.auth.LoginSession
import com.muyeon.domain.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUseCasesTest {
    private class FakeRepo : AuthRepository {
        var loginEmail: String? = null
        override suspend fun login(email: String, password: String): ApiResult<LoginSession> {
            loginEmail = email
            return ApiResult.Success(LoginSession("t", null, "{}", true, false))
        }
        override suspend fun findId(name: String, phone: String) = ApiResult.Success("a@b.co")
        override suspend fun sendResetCode(email: String) = ApiResult.Success(Unit)
        override suspend fun resetPassword(email: String, code: String, newPassword: String) = ApiResult.Success(Unit)
        override suspend fun agreeTerms(agreements: Map<String, Boolean>) = ApiResult.Success(Unit)
        override suspend fun sendSignupCode(email: String) = ApiResult.Success(Unit)
        override suspend fun verifySignupCode(email: String, code: String) = ApiResult.Success(Unit)
        override suspend fun register(email: String, password: String, name: String, phone: String?, identityToken: String?, agreements: Map<String, Any>): ApiResult<LoginSession> =
            ApiResult.Success(LoginSession("t", null, "{}", true, false))
        override suspend fun completeSocial(name: String, identityToken: String?) = ApiResult.Success("{}")
        override suspend fun niceConfig() = ApiResult.Success(com.muyeon.domain.model.auth.NiceConfig(true, false))
        override suspend fun startNice(returnTo: String) = ApiResult.Success("")
        override suspend fun niceResult(token: String) = ApiResult.Success(com.muyeon.domain.model.auth.NiceIdentity(token, "", ""))
        override suspend fun addRole(role: String) = ApiResult.Success(Unit)
        override suspend fun submitRoleVerification(role: String, urls: List<String>, academyName: String?, businessNumber: String?, startDate: String?) = ApiResult.Success(false)
    }

    @Test fun `로그인 — 빈 칸이면 보내지 않고, 이메일 앞뒤 공백은 지운다`() = runTest {
        val repo = FakeRepo()
        val uc = EmailLoginUseCase(repo)
        assertFalse(uc.check(" ", "pw"))
        assertFalse(uc.check("a@b.co", ""))
        assertTrue(uc.check("a@b.co", "pw"))
        uc(" a@b.co ", "pw")
        assertEquals("a@b.co", repo.loginEmail)
    }

    @Test fun `비밀번호 재설정 검사 — 웹 AccountRecovery 순서`() {
        val uc = ResetPasswordUseCase(FakeRepo())
        assertEquals(ResetPasswordUseCase.Invalid.EMAIL, uc.checkEmail("abc@naver"))
        assertNull(uc.checkEmail("abc@naver.com"))
        assertEquals(ResetPasswordUseCase.Invalid.CODE, uc.check("12345", "password1", "password1"))
        assertEquals(ResetPasswordUseCase.Invalid.LENGTH, uc.check("123456", "short", "short"))
        assertEquals(ResetPasswordUseCase.Invalid.MISMATCH, uc.check("123456", "password1", "password2"))
        assertNull(uc.check("123456", "password1", "password1"))
    }
}
