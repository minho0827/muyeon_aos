package com.muyeon.network.api

import com.muyeon.network.dto.auth.FindIdBody
import com.muyeon.network.dto.auth.FindIdDto
import com.muyeon.network.dto.auth.LoginBody
import com.muyeon.network.dto.auth.LoginResponseDto
import com.muyeon.network.dto.auth.ResetPasswordEmailBody
import com.muyeon.network.dto.auth.SendEmailCodeBody
import com.muyeon.network.dto.auth.TermsAgreeBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** 출처: auth.controller.ts(login · find-id · email/send-code · reset-password-email · terms/agree · email/verify-code · register · me/social-complete) · identity.controller.ts · roles.controller.ts. */
interface AuthApi {
    @POST(ApiEndpoints.Auth.LOGIN) suspend fun login(@Body body: LoginBody): Response<LoginResponseDto>
    @POST(ApiEndpoints.Auth.FIND_ID) suspend fun findId(@Body body: FindIdBody): Response<FindIdDto>
    @POST(ApiEndpoints.Auth.EMAIL_SEND_CODE) suspend fun sendEmailCode(@Body body: SendEmailCodeBody): Response<ResponseBody>
    @POST(ApiEndpoints.Auth.RESET_PASSWORD_EMAIL) suspend fun resetPasswordEmail(@Body body: ResetPasswordEmailBody): Response<ResponseBody>
    @POST(ApiEndpoints.Auth.TERMS_AGREE) suspend fun agreeTerms(@Body body: TermsAgreeBody): Response<ResponseBody>

    // ★ 2026-11-08 [b1] 이메일 가입 · 소셜 추가정보 · 가입 전 본인확인 · 가입 직후 회원유형(identity.controller.ts · roles.controller.ts)
    @POST(ApiEndpoints.Auth.EMAIL_VERIFY_CODE) suspend fun verifyEmailCode(@Body body: com.muyeon.network.dto.auth.VerifyEmailCodeBody): Response<ResponseBody>
    @POST(ApiEndpoints.Auth.REGISTER) suspend fun register(@Body body: com.muyeon.network.dto.auth.RegisterBody): Response<LoginResponseDto>
    @PATCH(ApiEndpoints.Auth.SOCIAL_COMPLETE) suspend fun completeSocial(@Body body: com.muyeon.network.dto.auth.SocialCompleteBody): Response<ResponseBody>
    @GET(ApiEndpoints.Auth.NICE_CONFIG) suspend fun niceConfig(): Response<com.muyeon.network.dto.auth.NiceConfigDto>
    @POST(ApiEndpoints.Auth.NICE_START) suspend fun startNice(@Body body: com.muyeon.network.dto.account.ReturnToBody): Response<com.muyeon.network.dto.account.AuthUrlDto>
    @GET(ApiEndpoints.Auth.NICE_RESULT) suspend fun niceResult(@Query("token") token: String): Response<com.muyeon.network.dto.auth.NiceResultDto>
    @POST(ApiEndpoints.Auth.ROLES) suspend fun addRole(@Body body: com.muyeon.network.dto.auth.AddRoleBody): Response<ResponseBody>
    @POST(ApiEndpoints.Auth.ROLE_VERIFICATION) suspend fun submitRoleVerification(
        @Path("role") role: String, @Body body: com.muyeon.network.dto.auth.RoleVerificationBody,
    ): Response<com.muyeon.network.dto.auth.RoleVerificationResultDto>
}
