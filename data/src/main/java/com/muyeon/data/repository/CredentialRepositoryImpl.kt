package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.credential.CredentialDocument
import com.muyeon.domain.model.credential.MyCredentials
import com.muyeon.domain.repository.CredentialRepository
import com.muyeon.network.api.CredentialApi
import com.muyeon.network.dto.credential.CredentialKeysBody
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class CredentialRepositoryImpl(private val api: CredentialApi, private val moshi: Moshi) : CredentialRepository {
    override suspend fun mine(): ApiResult<MyCredentials> = safeApiCall(moshi) { api.mine() }.map { it.toDomain() }

    override suspend fun upload(doc: CredentialDocument): ApiResult<String> = safeApiCall(moshi) {
        api.uploadDocument(MultipartBody.Part.createFormData("file", doc.fileName, doc.bytes.toRequestBody(doc.mimeType.toMediaType())))
    }.map { it.key.orEmpty() }

    override suspend fun submit(kind: String, keys: List<String>): ApiResult<MyCredentials> =
        safeApiCall(moshi) { api.submit(kind, CredentialKeysBody(keys)) }.map { it.toDomain() }
}
