package com.muyeon.data.repository

import android.content.Context
import com.muyeon.data.common.safeApiCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.policy.PolicyDoc
import com.muyeon.domain.repository.PolicyRepository
import com.muyeon.network.api.PolicyApi
import com.muyeon.network.dto.policy.PolicyFallbacksDto
import com.squareup.moshi.Moshi

/**
 * ★ 2026-11-08 (b4) 약관·정책 — 서버 문서(웹 callGetPolicy·callGetPolicyVersion).
 *   기본 문구는 assets/policy_fallbacks.json(웹 constants/policyContent.js 를 옮긴 것 — iOS Resources/NativePolicyFallbacks.json 과 같은 파일).
 */
class PolicyRepositoryImpl(context: Context, private val api: PolicyApi, private val moshi: Moshi) : PolicyRepository {
    private val appContext = context.applicationContext
    private val fallbacks: Map<String, PolicyFallbacksDto.Doc> by lazy {
        runCatching {
            appContext.assets.open(ASSET).bufferedReader().use { r -> moshi.adapter(PolicyFallbacksDto::class.java).fromJson(r.readText())?.documents }
        }.getOrNull().orEmpty()
    }

    override suspend fun current(docKey: String): ApiResult<PolicyDoc> = safeApiCall(moshi) { api.doc(docKey) }.map { it.toDomain() }

    override suspend fun version(docKey: String, version: String): ApiResult<PolicyDoc> =
        safeApiCall(moshi) { api.version(docKey, version) }.map { it.toDomain() }

    /** 웹 `POLICY_DOCS[key] || POLICY_DOCS[DEFAULT_DOC]`. */
    override fun fallback(docKey: String): String? = (fallbacks[docKey] ?: fallbacks[DEFAULT_DOC])?.md?.takeIf { it.isNotBlank() }

    private companion object {
        const val ASSET = "policy_fallbacks.json"
        const val DEFAULT_DOC = "terms"
    }
}
