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
) {
    fun toDomain() = kind?.let {
        CredentialItem(it, label.orEmpty(), desc?.takeIf { s -> s.isNotBlank() }, docs?.takeIf { s -> s.isNotBlank() }, status ?: "NONE",
            title?.takeIf { s -> s.isNotBlank() }, years?.let { y -> if (y % 1.0 == 0.0) y.toInt().toString() else y.toString() }, memo?.takeIf { s -> s.isNotBlank() })
    }
}

@JsonClass(generateAdapter = true)
data class MyCredentialsDto(val identity: CredentialIdentityDto? = null, val unsortedPending: Boolean? = null, val items: List<CredentialItemDto>? = null) {
    fun toDomain() = MyCredentials(identity?.verified == true, unsortedPending == true, items.orEmpty().mapNotNull { it.toDomain() })
}

@JsonClass(generateAdapter = true)
data class CredentialKeysBody(val keys: List<String>)

@JsonClass(generateAdapter = true)
data class DocumentKeyDto(val key: String? = null)
