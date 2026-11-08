package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.policy.PolicyDoc

/** ★ 2026-11-08 (b4) 약관·정책 문서(비로그인 허용). */
interface PolicyRepository {
    suspend fun current(docKey: String): ApiResult<PolicyDoc>
    suspend fun version(docKey: String, version: String): ApiResult<PolicyDoc>
    /** 서버를 못 부를 때의 기본 문구(웹 constants/policyContent.js 와 같은 본문) — 없는 문서면 기본(이용약관). */
    fun fallback(docKey: String): String?
}
