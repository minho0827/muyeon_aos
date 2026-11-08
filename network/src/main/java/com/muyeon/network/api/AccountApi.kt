package com.muyeon.network.api

import com.muyeon.network.dto.account.AccountDto
import com.muyeon.network.dto.account.AuthUrlDto
import com.muyeon.network.dto.account.IdentityTokenBody
import com.muyeon.network.dto.account.ReturnToBody
import com.muyeon.network.dto.account.UpdateAccountBody
import com.muyeon.network.dto.account.UploadUrlDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part

/** 출처: auth.controller.ts(me · me/identity) · identity.controller.ts(nice/link/start) · uploads.controller.ts. */
interface AccountApi {
    @GET(ApiEndpoints.Account.ME) suspend fun me(): Response<AccountDto>
    @PATCH(ApiEndpoints.Account.ME) suspend fun update(@Body body: UpdateAccountBody): Response<AccountDto>
    @POST(ApiEndpoints.Account.LINK_IDENTITY) suspend fun linkIdentity(@Body body: IdentityTokenBody): Response<AccountDto>
    @POST(ApiEndpoints.Account.NICE_LINK_START) suspend fun startIdentity(@Body body: ReturnToBody): Response<AuthUrlDto>

    @PATCH(ApiEndpoints.Account.PASSWORD) suspend fun changePassword(@Body body: com.muyeon.network.dto.account.NewPasswordBody): Response<okhttp3.ResponseBody>
    @GET(ApiEndpoints.Account.SUB_TRUST) suspend fun subTrust(): Response<com.muyeon.network.dto.account.SubTrustDto>

    @Multipart
    @POST(ApiEndpoints.Account.UPLOAD_IMAGE)
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<UploadUrlDto>
}
