package com.muyeon.network.dto.credential

import com.muyeon.domain.model.credential.CredentialItem
import com.muyeon.domain.model.credential.MyCredentials
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CredentialIdentityDto(val verified: Boolean? = null)

@JsonClass(generateAdapter = true)
data class CredentialItemDto(
    val kind: String? = null, val label: String? = null, val desc: String? = null, val docs: String? = null,
    val status: String? = null, val title: String? = null, val years: Double? = null, val memo: String? = null,
    // ★ 2026-11-10 신청 철회 — id(철회 주소) · withdrawnAt(status WITHDRAWN 일 때만)
    val id: Long? = null, val withdrawnAt: String? = null,
) {
    fun toDomain() = kind?.let {
        CredentialItem(it, label.orEmpty(), desc?.takeIf { s -> s.isNotBlank() }, docs?.takeIf { s -> s.isNotBlank() }, status ?: "NONE",
            title?.takeIf { s -> s.isNotBlank() }, years?.let { y -> if (y % 1.0 == 0.0) y.toInt().toString() else y.toString() }, memo?.takeIf { s -> s.isNotBlank() },
            id, withdrawnAt?.takeIf { s -> s.isNotBlank() })
    }
}

@JsonClass(generateAdapter = true)
data class MyCredentialsDto(val identity: CredentialIdentityDto? = null, val unsortedPending: Boolean? = null, val items: List<CredentialItemDto>? = null) {
    fun toDomain() = MyCredentials(identity?.verified == true, unsortedPending == true, items.orEmpty().mapNotNull { it.toDomain() })
}

@JsonClass(generateAdapter = true)
data class CredentialKeysBody(val keys: List<String>)

/** POST …/withdraw — 사유는 선택(빈칸이면 Moshi 가 빼고 보낸다). */
@JsonClass(generateAdapter = true)
data class CredentialWithdrawBody(val reason: String? = null)

@JsonClass(generateAdapter = true)
data class DocumentKeyDto(val key: String? = null)
