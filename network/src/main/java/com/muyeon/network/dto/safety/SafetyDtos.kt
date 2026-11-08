package com.muyeon.network.dto.safety

import com.squareup.moshi.JsonClass

/** POST /reports — reports CreateReportDto. */
@JsonClass(generateAdapter = true)
data class ReportBody(val targetType: String, val targetId: Long, val reason: String, val detail: String? = null)

/** GET /me/blocks 한 줄(blocks.service listBlocked) — id 는 차단한 회원 id. */
@JsonClass(generateAdapter = true)
data class BlockedUserDto(val id: Long? = null, val userId: Long? = null, val name: String? = null, val image: String? = null, val blockedAt: String? = null) {
    fun toDomain() = (id ?: userId)?.let {
        com.muyeon.domain.model.safety.BlockedUser(it, name?.takeIf { n -> n.isNotBlank() }, image?.takeIf { i -> i.isNotBlank() }, blockedAt)
    }
}
