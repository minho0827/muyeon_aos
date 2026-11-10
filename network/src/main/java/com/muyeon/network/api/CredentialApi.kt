package com.muyeon.network.api

import com.muyeon.network.dto.credential.CredentialKeysBody
import com.muyeon.network.dto.credential.CredentialWithdrawBody
import com.muyeon.network.dto.credential.DocumentKeyDto
import com.muyeon.network.dto.credential.MyCredentialsDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/** 출처: credentials.controller.ts(me/credentials) · uploads.controller.ts(document — 공개 주소 없는 비공개 서류). */
interface CredentialApi {
    @GET(ApiEndpoints.Credentials.MINE) suspend fun mine(): Response<MyCredentialsDto>
    @POST(ApiEndpoints.Credentials.SUBMIT) suspend fun submit(@Path("kind") kind: String, @Body body: CredentialKeysBody): Response<MyCredentialsDto>
    @POST(ApiEndpoints.Credentials.WITHDRAW) suspend fun withdraw(@Path("kind") kind: String, @Body body: CredentialWithdrawBody): Response<MyCredentialsDto>
    @POST(ApiEndpoints.Credentials.WITHDRAW_BY_ID)
    suspend fun withdrawById(@Path("kind") kind: String, @Path("id") id: Long, @Body body: CredentialWithdrawBody): Response<MyCredentialsDto>

    @Multipart
    @POST(ApiEndpoints.Credentials.UPLOAD_DOCUMENT)
    suspend fun uploadDocument(@Part file: MultipartBody.Part): Response<DocumentKeyDto>
}
