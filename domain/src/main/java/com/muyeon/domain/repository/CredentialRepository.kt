package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.credential.CredentialDocument
import com.muyeon.domain.model.credential.MyCredentials

/** 경력·자격 확인 — 현황 · 서류 올리기(비공개 key) · 항목 제출. */
interface CredentialRepository {
    suspend fun mine(): ApiResult<MyCredentials>
    suspend fun upload(doc: CredentialDocument): ApiResult<String>
    suspend fun submit(kind: String, keys: List<String>): ApiResult<MyCredentials>
}
