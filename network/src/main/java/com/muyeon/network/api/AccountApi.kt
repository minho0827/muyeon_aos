package com.muyeon.network.api

import com.muyeon.network.dto.account.AccountDto
import com.muyeon.network.dto.account.AuthUrlDto
import com.muyeon.network.dto.account.IdentityTokenBody
import com.muyeon.network.dto.account.ReturnToBody
import com.muyeon.network.dto.account.UpdateAccountBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

/** 출처: auth.controller.ts(me · me/identity) · identity.controller.ts(nice/link/start) · uploads.controller.ts. */
interface AccountApi {
    @GET(ApiEndpoints.Account.ME) suspend fun me(): Response<AccountDto>
    @PATCH(ApiEndpoints.Account.ME) suspend fun update(@Body body: UpdateAccountBody): Response<AccountDto>
    @POST(ApiEndpoints.Account.LINK_IDENTITY) suspend fun linkIdentity(@Body body: IdentityTokenBody): Response<AccountDto>
    @POST(ApiEndpoints.Account.NICE_LINK_START) suspend fun startIdentity(@Body body: ReturnToBody): Response<AuthUrlDto>

    @GET(ApiEndpoints.Account.PROFILE) suspend fun profile(): Response<com.muyeon.network.dto.account.ProfileGenresDto>
    @PATCH(ApiEndpoints.Account.PROFILE) suspend fun updateGenres(@Body body: com.muyeon.network.dto.account.ProfileGenresDto): Response<okhttp3.ResponseBody>
    @PATCH(ApiEndpoints.Account.PASSWORD) suspend fun changePassword(@Body body: com.muyeon.network.dto.account.NewPasswordBody): Response<okhttp3.ResponseBody>
    @GET(ApiEndpoints.Account.SUB_TRUST) suspend fun subTrust(): Response<com.muyeon.network.dto.account.SubTrustDto>

    @GET(ApiEndpoints.Account.WITHDRAWAL_INFO) suspend fun withdrawalInfo(): Response<okhttp3.ResponseBody>
    @POST(ApiEndpoints.Account.WITHDRAW) suspend fun withdraw(@Body body: okhttp3.RequestBody): Response<okhttp3.ResponseBody>
}
