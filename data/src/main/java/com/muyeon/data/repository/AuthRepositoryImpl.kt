package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.data.common.safeUnitCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.ErrorKind
import com.muyeon.domain.common.UserFacingError
import com.muyeon.domain.model.auth.LoginSession
import com.muyeon.domain.repository.AuthRepository
import com.muyeon.network.api.AuthApi
import com.muyeon.network.dto.auth.FindIdBody
import com.muyeon.network.dto.auth.LoginBody
import com.muyeon.network.dto.auth.LoginResponseDto
import com.muyeon.network.dto.auth.ResetPasswordEmailBody
import com.muyeon.network.dto.auth.SendEmailCodeBody
import com.muyeon.network.dto.auth.TermsAgreeBody
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class AuthRepositoryImpl(private val api: AuthApi, private val moshi: Moshi) : AuthRepository {
    private val mapAdapter by lazy {
        moshi.adapter<Map<String, Any?>>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)).serializeNulls()
    }

    override suspend fun login(email: String, password: String): ApiResult<LoginSession> =
        when (val r = safeApiCall(moshi) { api.login(LoginBody(email, password)) }) {
            is ApiResult.Success -> toSession(r.data)
            is ApiResult.Error -> r
        }

    /** 응답 → 세션. 토큰이 비면 실패(서버 공통 문구). */
    internal fun toSession(dto: LoginResponseDto): ApiResult<LoginSession> {
        val token = dto.accessToken?.takeIf { it.isNotBlank() }
            ?: return ApiResult.Error(IllegalStateException("accessToken"), UserFacingError.server, kind = ErrorKind.SERVER)
        val user = dto.user.orEmpty()
        return ApiResult.Success(
            LoginSession(
                accessToken = token,
                sessionId = dto.sessionId?.takeIf { it.isNotBlank() },
                userJson = mapAdapter.toJson(user.mapValues { (_, v) -> intIfWhole(v) }),
                termsAgreed = user["termsAgreed"] as? Boolean,
                signupIncomplete = user["signupIncomplete"] == true,
            ),
        )
    }

    override suspend fun findId(name: String, phone: String): ApiResult<String> =
        safeApiCall(moshi) { api.findId(FindIdBody(name, phone)) }.map { it.email.orEmpty() }

    override suspend fun sendResetCode(email: String): ApiResult<Unit> =
        safeUnitCall(moshi) { api.sendEmailCode(SendEmailCodeBody(email, PURPOSE_RESET)) }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): ApiResult<Unit> =
        safeUnitCall(moshi) { api.resetPasswordEmail(ResetPasswordEmailBody(email, code, newPassword)) }

    override suspend fun agreeTerms(agreements: Map<String, Boolean>): ApiResult<Unit> =
        safeUnitCall(moshi) { api.agreeTerms(TermsAgreeBody(agreements)) }

    // ★ 2026-11-08 [b1] 가입·소셜 추가정보·본인확인·가입 직후 회원유형

    override suspend fun sendSignupCode(email: String): ApiResult<Unit> =
        safeUnitCall(moshi) { api.sendEmailCode(SendEmailCodeBody(email, PURPOSE_SIGNUP)) }

    override suspend fun verifySignupCode(email: String, code: String): ApiResult<Unit> =
        safeUnitCall(moshi) { api.verifyEmailCode(com.muyeon.network.dto.auth.VerifyEmailCodeBody(email, code, PURPOSE_SIGNUP)) }

    override suspend fun register(
        email: String, password: String, name: String, phone: String?, identityToken: String?, agreements: Map<String, Any>,
    ): ApiResult<LoginSession> =
        when (val r = safeApiCall(moshi) { api.register(com.muyeon.network.dto.auth.RegisterBody(email, password, name, phone, identityToken, agreements)) }) {
            is ApiResult.Success -> toSession(r.data)
            is ApiResult.Error -> r
        }

    override suspend fun completeSocial(name: String, identityToken: String?): ApiResult<String> =
        safeApiCall(moshi) { api.completeSocial(com.muyeon.network.dto.auth.SocialCompleteBody(name, identityToken)) }.map { body ->
            val user = runCatching { mapAdapter.fromJson(body.string()) }.getOrNull().orEmpty()
            mapAdapter.toJson(user.mapValues { (_, v) -> intIfWhole(v) })
        }

    override suspend fun niceConfig(): ApiResult<com.muyeon.domain.model.auth.NiceConfig> =
        safeApiCall(moshi) { api.niceConfig() }.map { com.muyeon.domain.model.auth.NiceConfig(it.available == true, it.required == true) }

    override suspend fun startNice(returnTo: String): ApiResult<String> =
        safeApiCall(moshi) { api.startNice(com.muyeon.network.dto.account.ReturnToBody(returnTo)) }.map { it.authUrl.orEmpty() }

    override suspend fun niceResult(token: String): ApiResult<com.muyeon.domain.model.auth.NiceIdentity> =
        safeApiCall(moshi) { api.niceResult(token) }.map { com.muyeon.domain.model.auth.NiceIdentity(token, it.name.orEmpty(), it.phone.orEmpty()) }

    override suspend fun addRole(role: String): ApiResult<Unit> =
        safeUnitCall(moshi) { api.addRole(com.muyeon.network.dto.auth.AddRoleBody(role)) }

    override suspend fun submitRoleVerification(
        role: String, urls: List<String>, academyName: String?, businessNumber: String?, startDate: String?,
    ): ApiResult<Boolean> = safeApiCall(moshi) {
        api.submitRoleVerification(
            role,
            com.muyeon.network.dto.auth.RoleVerificationBody(urls.map { com.muyeon.network.dto.auth.RoleDocumentBody("DOC", it) }, academyName, businessNumber, startDate),
        )
    }.map { it.autoApproved == true }

    private companion object {
        const val PURPOSE_RESET = "RESET"
        const val PURPOSE_SIGNUP = "SIGNUP"

        /** Moshi 는 숫자를 Double 로 읽는다 — 회원 번호 12 가 웹 저장소에 12.0 으로 들어가지 않게 정수는 정수로. */
        fun intIfWhole(v: Any?): Any? = when (v) {
            is Double -> if (v % 1.0 == 0.0 && kotlin.math.abs(v) < 9.0E15) v.toLong() else v
            is Map<*, *> -> v.mapValues { (_, x) -> intIfWhole(x) }
            is List<*> -> v.map { intIfWhole(it) }
            else -> v
        }
    }
}
