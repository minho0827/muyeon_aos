package com.muyeon.app.data.api_endpoint

import com.muyeon.app.data.models.login.LoginRequest
import com.muyeon.app.data.models.login.LoginResponse
import com.muyeon.app.data.models.qr.QrRequest
import com.muyeon.app.data.models.qr.QrResponse
import com.muyeon.app.data.models.token.RefreshTokenResponse
import com.muyeon.app.data.models.token.TokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
interface ApiService {
    @POST("user-service/user/auth/refreshTokenCheck")

    suspend fun refreshTokenCheck(@Body requestBody: TokenRequest): Response<RefreshTokenResponse>

    @POST("auth/signin")
    suspend fun login(@Body requestBody: LoginRequest): Response<LoginResponse>

    @POST("api/reserve/external/selectQrReserve")
    suspend fun checkQrCode(@Body requestBody: QrRequest): Response<QrResponse>
}