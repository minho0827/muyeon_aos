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
import retrofit2.http.POST

/** 출처: auth.controller.ts(login · find-id · email/send-code · reset-password-email · terms/agree). */
interface AuthApi {
    @POST(ApiEndpoints.Auth.LOGIN) suspend fun login(@Body body: LoginBody): Response<LoginResponseDto>
    @POST(ApiEndpoints.Auth.FIND_ID) suspend fun findId(@Body body: FindIdBody): Response<FindIdDto>
    @POST(ApiEndpoints.Auth.EMAIL_SEND_CODE) suspend fun sendEmailCode(@Body body: SendEmailCodeBody): Response<ResponseBody>
    @POST(ApiEndpoints.Auth.RESET_PASSWORD_EMAIL) suspend fun resetPasswordEmail(@Body body: ResetPasswordEmailBody): Response<ResponseBody>
    @POST(ApiEndpoints.Auth.TERMS_AGREE) suspend fun agreeTerms(@Body body: TermsAgreeBody): Response<ResponseBody>
}
