package com.muyeon.domain.usecase.credential

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.credential.CredentialDocument
import com.muyeon.domain.model.credential.MyCredentials
import com.muyeon.domain.repository.CredentialRepository

/** 경력·자격 확인(웹 CredentialsManage) — 서류는 한 항목에 최대 [MAX_FILES]장, 하나씩 올려 key 를 모은 뒤 제출. */
class CredentialsUseCase(private val repo: CredentialRepository) {
    suspend fun load() = repo.mine()

    /** 신청 철회 — 확인 중·확인 완료만(아니면 서버 409 문구 그대로). */
    suspend fun withdraw(kind: String, id: Long?, reason: String? = null) = repo.withdraw(kind, id, reason)

    suspend fun submit(kind: String, docs: List<CredentialDocument>): ApiResult<MyCredentials> {
        if (docs.isEmpty()) return ApiResult.Error(IllegalArgumentException("docs"), "서류를 한 장 이상 골라 주세요.")
        val keys = ArrayList<String>()
        for (d in docs.take(MAX_FILES)) {
            when (val r = repo.upload(d)) {
                is ApiResult.Success -> if (r.data.isNotBlank()) keys.add(r.data)
                is ApiResult.Error -> return r
            }
        }
        if (keys.isEmpty()) return ApiResult.Error(IllegalStateException("keys"), "서류를 올리지 못했어요. 잠시 후 다시 시도해 주세요.")
        return repo.submit(kind, keys)
    }

    companion object { const val MAX_FILES = 5 }
}
