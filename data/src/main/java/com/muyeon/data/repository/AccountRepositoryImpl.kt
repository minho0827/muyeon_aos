package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.account.MyAccount
import com.muyeon.domain.repository.AccountRepository
import com.muyeon.network.api.AccountApi
import com.muyeon.network.dto.account.IdentityTokenBody
import com.muyeon.network.dto.account.ReturnToBody
import com.muyeon.network.dto.account.UpdateAccountBody
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class AccountRepositoryImpl(private val api: AccountApi, private val moshi: Moshi) : AccountRepository {
    override suspend fun me(): ApiResult<MyAccount> = safeApiCall(moshi) { api.me() }.map { it.toDomain() }

    override suspend fun update(name: String?, image: String): ApiResult<MyAccount> =
        safeApiCall(moshi) { api.update(UpdateAccountBody(name, image)) }.map { it.toDomain() }

    override suspend fun uploadImage(jpeg: ByteArray): ApiResult<String> = safeApiCall(moshi) {
        api.uploadImage(MultipartBody.Part.createFormData("file", "avatar.jpg", jpeg.toRequestBody("image/jpeg".toMediaType())))
    }.map { it.url.orEmpty() }

    override suspend fun startIdentity(returnTo: String): ApiResult<String> =
        safeApiCall(moshi) { api.startIdentity(ReturnToBody(returnTo)) }.map { it.authUrl.orEmpty() }

    override suspend fun linkIdentity(token: String): ApiResult<MyAccount> =
        safeApiCall(moshi) { api.linkIdentity(IdentityTokenBody(token)) }.map { it.toDomain() }
}
