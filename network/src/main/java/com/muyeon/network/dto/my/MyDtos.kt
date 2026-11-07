package com.muyeon.network.dto.my

import com.muyeon.domain.model.my.BenefitUsage
import com.muyeon.domain.model.my.MyMembership
import com.muyeon.domain.model.my.PendingPostings
import com.muyeon.domain.model.my.PendingRole
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MeDto(val name: String? = null)

@JsonClass(generateAdapter = true)
data class CountDto(val count: Int? = null)

@JsonClass(generateAdapter = true)
data class AllowedDto(val allowed: Boolean? = null)

@JsonClass(generateAdapter = true)
data class EligibleDto(val eligible: Boolean? = null)

@JsonClass(generateAdapter = true)
data class BenefitUsageDto(val key: String? = null, val label: String? = null, val kind: String? = null, val limit: Int? = null, val used: Int? = null)

@JsonClass(generateAdapter = true)
data class MembershipDto(val active: Boolean? = null, val tier: String? = null, val benefitUsage: List<BenefitUsageDto>? = null) {
    fun toDomain() = MyMembership(active == true, tier, benefitUsage.orEmpty().map { BenefitUsage(it.key, it.label, it.kind, it.limit, it.used) })
}

@JsonClass(generateAdapter = true)
data class PendingRoleDto(val role: String? = null, val status: String? = null)

@JsonClass(generateAdapter = true)
data class PendingSummaryDto(val total: Int? = null, val roles: List<PendingRoleDto>? = null) {
    fun toDomain() = PendingPostings(total ?: 0, roles.orEmpty().mapNotNull { r -> r.role?.let { PendingRole(it, r.status) } })
}
