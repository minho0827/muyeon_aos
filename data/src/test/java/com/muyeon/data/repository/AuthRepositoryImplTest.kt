package com.muyeon.data.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.network.dto.auth.LoginResponseDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {
    private val moshi = Moshi.Builder().build()
    private val repo = AuthRepositoryImpl(api = FakeAuthApi, moshi = moshi)
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))

    @Test fun `로그인 응답 — 토큰·세션·약관 동의 · 회원 번호는 정수 그대로`() {
        val r = repo.toSession(
            LoginResponseDto("tok", "sid", mapOf("id" to 12.0, "name" to "김무용", "termsAgreed" to false, "memberTypes" to listOf("TEACHER"))),
        ) as ApiResult.Success
        assertEquals("tok", r.data.accessToken)
        assertEquals("sid", r.data.sessionId)
        assertEquals(false, r.data.termsAgreed)
        assertFalse(r.data.signupIncomplete)
        assertTrue(r.data.userJson.contains("\"id\":12,"))
        assertEquals("김무용", mapAdapter.fromJson(r.data.userJson)!!["name"])
    }

    @Test fun `옛 응답(약관 칸 없음)은 게이트 없음 · 소셜 미완료는 그대로`() {
        val r = repo.toSession(LoginResponseDto("tok", null, mapOf("signupIncomplete" to true))) as ApiResult.Success
        assertNull(r.data.termsAgreed)
        assertNull(r.data.sessionId)
        assertTrue(r.data.signupIncomplete)
    }

    @Test fun `토큰이 없으면 실패`() {
        assertTrue(repo.toSession(LoginResponseDto(null, "sid", emptyMap())) is ApiResult.Error)
    }

    private object FakeAuthApi : com.muyeon.network.api.AuthApi {
        override suspend fun login(body: com.muyeon.network.dto.auth.LoginBody) = error("unused")
        override suspend fun findId(body: com.muyeon.network.dto.auth.FindIdBody) = error("unused")
        override suspend fun sendEmailCode(body: com.muyeon.network.dto.auth.SendEmailCodeBody) = error("unused")
        override suspend fun resetPasswordEmail(body: com.muyeon.network.dto.auth.ResetPasswordEmailBody) = error("unused")
        override suspend fun agreeTerms(body: com.muyeon.network.dto.auth.TermsAgreeBody) = error("unused")
        override suspend fun verifyEmailCode(body: com.muyeon.network.dto.auth.VerifyEmailCodeBody) = error("unused")
        override suspend fun register(body: com.muyeon.network.dto.auth.RegisterBody) = error("unused")
        override suspend fun completeSocial(body: com.muyeon.network.dto.auth.SocialCompleteBody) = error("unused")
        override suspend fun niceConfig() = error("unused")
        override suspend fun startNice(body: com.muyeon.network.dto.account.ReturnToBody) = error("unused")
        override suspend fun niceResult(token: String) = error("unused")
        override suspend fun addRole(body: com.muyeon.network.dto.auth.AddRoleBody) = error("unused")
        override suspend fun submitRoleVerification(role: String, body: com.muyeon.network.dto.auth.RoleVerificationBody) = error("unused")
    }
}
