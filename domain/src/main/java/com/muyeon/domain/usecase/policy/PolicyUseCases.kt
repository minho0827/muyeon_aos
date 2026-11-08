package com.muyeon.domain.usecase.policy

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.policy.PolicyView
import com.muyeon.domain.repository.PolicyRepository

/**
 * ★ 2026-11-08 (b4) 약관 문서 — 웹 Policy.js: 현재 문서 + (?version= 이면) 그 버전.
 *   서버 현재 문서를 못 읽으면 기본 문구로(약관 화면이 비면 안 된다). 버전을 요청했는데 못 읽으면 현재(또는 기본) 본문을 보인다(웹과 같다).
 *   기본 문구도 없으면 Error.
 */
class GetPolicyUseCase(private val repo: PolicyRepository) {
    suspend operator fun invoke(docKey: String, version: String?): ApiResult<PolicyView> {
        val current = repo.current(docKey).getOrNull()?.takeIf { it.body.isNotBlank() }
        val shown = version?.takeIf { it.isNotBlank() }?.let { repo.version(docKey, it).getOrNull()?.takeIf { d -> d.body.isNotBlank() } }
        val body = shown?.body ?: current?.body ?: repo.fallback(docKey)
            ?: return ApiResult.Error(IllegalStateException("policy unavailable"))
        return ApiResult.Success(PolicyView(body, current, shown))
    }
}
