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

    private companion object {
        const val PURPOSE_RESET = "RESET"

        /** Moshi 는 숫자를 Double 로 읽는다 — 회원 번호 12 가 웹 저장소에 12.0 으로 들어가지 않게 정수는 정수로. */
        fun intIfWhole(v: Any?): Any? = when (v) {
            is Double -> if (v % 1.0 == 0.0 && kotlin.math.abs(v) < 9.0E15) v.toLong() else v
            is Map<*, *> -> v.mapValues { (_, x) -> intIfWhole(x) }
            is List<*> -> v.map { intIfWhole(it) }
            else -> v
        }
    }
}
