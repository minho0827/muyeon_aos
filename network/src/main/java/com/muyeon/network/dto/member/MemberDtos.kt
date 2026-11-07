package com.muyeon.network.dto.member

import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.member.MyRoles
import com.squareup.moshi.JsonClass

/** `GET /me/roles` → `{ held, roles, activeType }` (roles 는 탭 셸에 필요 없어 받지 않는다). */
@JsonClass(generateAdapter = true)
data class MyRolesDto(
    val held: List<String>? = null,
    val activeType: String? = null,
    /** 유형별 동그라미 사진(없으면 null) — 탭바 MY 칸. */
    val images: Map<String, String?>? = null,
) {
    fun toDomain() = MyRoles(
        held.orEmpty(), activeType?.takeIf { it.isNotBlank() } ?: ActiveType.GENERAL,
        images.orEmpty().mapNotNull { (k, v) -> v?.takeIf { it.isNotBlank() }?.let { k.uppercase() to it } }.toMap(),
    )
}

@JsonClass(generateAdapter = true)
data class ActiveTypeBody(val type: String)

/** `GET /home/role-hero` → `{ image }`. */
@JsonClass(generateAdapter = true)
data class RoleHeroDto(val image: String? = null)
