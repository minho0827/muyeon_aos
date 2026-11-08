package com.muyeon.network.dto.policy

import com.muyeon.domain.model.policy.PolicyDoc
import com.muyeon.domain.model.policy.PolicyVersion
import com.squareup.moshi.JsonClass

/** `GET /policies/:docKey` · `/versions/:version`(policy-documents.service publicDoc·publicVersion). */
@JsonClass(generateAdapter = true)
data class PolicyDocDto(
    val title: String? = null,
    val version: String? = null,
    val effectiveDate: String? = null,
    val status: String? = null,
    val body: String? = null,
    val upcoming: PolicyVersionDto? = null,
    val history: List<PolicyVersionDto>? = null,
) {
    fun toDomain() = PolicyDoc(title, version, effectiveDate, status, body.orEmpty(), upcoming?.toDomain(), history.orEmpty().mapNotNull { it.toDomain() })
}

@JsonClass(generateAdapter = true)
data class PolicyVersionDto(val version: String? = null, val effectiveDate: String? = null, val changeSummary: String? = null) {
    fun toDomain() = version?.takeIf { it.isNotBlank() }?.let { PolicyVersion(it, effectiveDate, changeSummary) }
}

/** 기본 문구 묶음(assets policy_fallbacks.json — 웹 constants/policyContent.js 를 옮긴 iOS NativePolicyFallbacks.json 과 같은 파일). */
@JsonClass(generateAdapter = true)
data class PolicyFallbacksDto(val documents: Map<String, Doc>? = null) {
    @JsonClass(generateAdapter = true)
    data class Doc(val title: String? = null, val md: String? = null)
}
