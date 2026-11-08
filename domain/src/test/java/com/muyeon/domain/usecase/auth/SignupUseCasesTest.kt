package com.muyeon.domain.usecase.auth

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.auth.LoginSession
import com.muyeon.domain.model.auth.NiceConfig
import com.muyeon.domain.model.auth.NiceIdentity
import com.muyeon.domain.model.auth.RoleSubmit
import com.muyeon.domain.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ★ 2026-11-08 [b1] 웹 Register.js · SocialSignup.js · consent.js · AppRouter __onRoleComplete 규칙. */
class SignupUseCasesTest {
    private class Repo(val autoApproved: Boolean = false) : AuthRepository {
        var registerPhone: String? = "unset"
        var registerAgreements: Map<String, Any>? = null
        val roles = mutableListOf<String>()
        var submitted = 0
        override suspend fun login(email: String, password: String): ApiResult<LoginSession> = ApiResult.Success(LoginSession("t", null, "{}", true, false))
        override suspend fun findId(name: String, phone: String) = ApiResult.Success("")
        override suspend fun sendResetCode(email: String) = ApiResult.Success(Unit)
        override suspend fun resetPassword(email: String, code: String, newPassword: String) = ApiResult.Success(Unit)
        override suspend fun agreeTerms(agreements: Map<String, Boolean>) = ApiResult.Success(Unit)
        override suspend fun sendSignupCode(email: String) = ApiResult.Success(Unit)
        override suspend fun verifySignupCode(email: String, code: String) = ApiResult.Success(Unit)
        override suspend fun register(email: String, password: String, name: String, phone: String?, identityToken: String?, agreements: Map<String, Any>): ApiResult<LoginSession> {
            registerPhone = phone; registerAgreements = agreements
            return ApiResult.Success(LoginSession("t", "s", "{}", true, false))
        }
        override suspend fun completeSocial(name: String, identityToken: String?) = ApiResult.Success("{}")
        override suspend fun niceConfig(): ApiResult<NiceConfig> = ApiResult.Error(IllegalStateException(), "x")
        override suspend fun startNice(returnTo: String) = ApiResult.Success("")
        override suspend fun niceResult(token: String) = ApiResult.Success(NiceIdentity(token, "", ""))
        override suspend fun addRole(role: String): ApiResult<Unit> { roles += role; return ApiResult.Success(Unit) }
        override suspend fun submitRoleVerification(role: String, urls: List<String>, academyName: String?, businessNumber: String?, startDate: String?): ApiResult<Boolean> {
            submitted++; return ApiResult.Success(autoApproved)
        }
    }

    private val allRequired = SignupAgreements.REQUIRED.associateWith { true }

    @Test fun `가입 확인 순서 — 웹 submit 그대로`() {
        val uc = RegisterUseCase(Repo())
        val id = NiceIdentity("tk", "홍길동", "010-****-1234")
        assertEquals(RegisterUseCase.Invalid.MISSING_FIELDS, uc.check("", "pw", "pw", "n", true, false, null, allRequired))
        assertEquals(RegisterUseCase.Invalid.EMAIL_UNVERIFIED, uc.check("a@b.co", "pw", "pw", "n", false, false, null, allRequired))
        assertEquals(RegisterUseCase.Invalid.IDENTITY_MISSING, uc.check("a@b.co", "pw", "pw", "n", true, true, null, allRequired))
        assertEquals(RegisterUseCase.Invalid.PASSWORD_SHORT, uc.check("a@b.co", "short", "short", "n", true, true, id, allRequired))
        assertEquals(RegisterUseCase.Invalid.PASSWORD_MISMATCH, uc.check("a@b.co", "password1", "password2", "n", true, false, null, allRequired))
        assertEquals(RegisterUseCase.Invalid.TERMS_MISSING, uc.check("a@b.co", "password1", "password1", "n", true, false, null, mapOf("terms" to true)))
        assertNull(uc.check("a@b.co", "password1", "password1", "n", true, false, null, allRequired))
        assertEquals(RegisterUseCase.CodeInvalid.EMAIL, uc.checkEmail("abc@naver"))
        assertEquals(RegisterUseCase.CodeInvalid.CODE, uc.checkCode("12345"))
    }

    @Test fun `가입 요청 — 본인확인 했으면 휴대폰 안 보냄 · 동의값은 toAgreements 모양`() = runTest {
        val repo = Repo()
        val uc = RegisterUseCase(repo)
        uc("a@b.co", "password1", "n", "01012345678", NiceIdentity("tk", "n", "010-****-5678"), allRequired + ("marketingSms" to true))
        assertNull(repo.registerPhone)
        assertEquals(true, repo.registerAgreements?.get("marketing"))
        assertEquals(SignupAgreements.VERSION, repo.registerAgreements?.get("version"))
        uc("a@b.co", "password1", "n", "", null, allRequired)
        assertNull(repo.registerPhone)
        uc("a@b.co", "password1", "n", "0101", null, allRequired)
        assertEquals("0101", repo.registerPhone)
    }

    @Test fun `본인확인 설정 조회 실패 = 미사용`() = runTest {
        assertEquals(NiceConfig(false, false), SignupIdentityUseCase(Repo()).config())
    }

    @Test fun `소셜 추가정보 — 기본 이름은 비우고 이름 · 본인인증 순서`() {
        val uc = SocialSignupUseCase(Repo())
        assertEquals("", uc.initialName("카카오회원"))
        assertEquals("김무용", uc.initialName("김무용"))
        assertEquals(SocialSignupUseCase.Invalid.NAME, uc.check("", true, null))
        assertEquals(SocialSignupUseCase.Invalid.IDENTITY_MISSING, uc.check("n", true, null))
        assertNull(uc.check("n", false, null))
    }

    @Test fun `가입 직후 회원유형 — 웹 __onRoleComplete 이동`() = runTest {
        val repo = Repo()
        val uc = CompleteRoleOnboardingUseCase(repo)
        assertEquals("/lessons", uc("HOBBY", emptyList(), null, null, null))
        assertEquals("/jobs", uc("TEACHER", emptyList(), null, null, null))
        assertEquals("/mypage", uc("ACADEMY", emptyList(), null, null, null))   // 건너뛰기
        assertEquals("/mypage?verifySubmitted=1", uc("TEAM", listOf("u"), null, null, null))
        assertEquals("/home", uc("SPACE", emptyList(), null, null, null))
        assertEquals(listOf("HOBBY", "TEACHER", "ACADEMY", "TEAM"), repo.roles)
        assertEquals(1, repo.submitted)
        assertEquals("/mypage", CompleteRoleOnboardingUseCase(Repo(autoApproved = true))("ACADEMY", emptyList(), "학원", "1234567890", "20200101"))
        assertEquals(RoleSubmit.NONE, uc.submit("ACADEMY", emptyList(), null, null, null))
    }

    @Test fun `하던 일로 돌아가는 주소`() {
        assertTrue(SignupRedirect.isTaskResume("/jobs/create?resume=pending"))
        assertTrue(SignupRedirect.isTaskResume("/quoteRequest?x=1"))
        assertFalse(SignupRedirect.isTaskResume("/home"))
        assertFalse(SignupRedirect.isTaskResume("//evil"))
    }
}
