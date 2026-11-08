package com.muyeon.network.dto.account

import com.muyeon.domain.model.account.MyAccount
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AccountProfileDto(val image: String? = null)

/** GET /auth/me — 개인정보 수정에 쓰는 칸만. */
@JsonClass(generateAdapter = true)
data class AccountDto(
    val email: String? = null, val name: String? = null, val phone: String? = null, val image: String? = null,
    val memberTypes: List<String>? = null, val nameLocked: Boolean? = null, val phoneVerified: Boolean? = null,
    val profile: AccountProfileDto? = null,
) {
    fun toDomain() = MyAccount(
        email = email.orEmpty(), name = name.orEmpty(), phone = phone.orEmpty(),
        image = (profile?.image ?: image)?.takeIf { it.isNotBlank() },
        memberTypes = memberTypes.orEmpty(), nameLocked = nameLocked == true, phoneVerified = phoneVerified == true,
    )
}

/** PATCH /auth/me — 이름은 잠기지 않았을 때만 보낸다(null 이면 Moshi 가 빼고 보낸다). */
@JsonClass(generateAdapter = true)
data class UpdateAccountBody(val name: String? = null, val image: String)

@JsonClass(generateAdapter = true)
data class IdentityTokenBody(val identityToken: String)

@JsonClass(generateAdapter = true)
data class ReturnToBody(val returnTo: String)

@JsonClass(generateAdapter = true)
data class AuthUrlDto(val authUrl: String? = null)

@JsonClass(generateAdapter = true)
data class NewPasswordBody(val newPassword: String)

/** GET /me/sub-trust 내역 한 줄. */
@JsonClass(generateAdapter = true)
data class SubTrustHistoryDto(
    val id: Long? = null, val createdAt: String? = null, val postingTitle: String? = null, val side: String? = null,
    val reason: String? = null, val points: Double? = null, val revokedAt: String? = null, val expiresAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class SubTrustDto(
    val points90d: Double? = null, val cancels90d: Double? = null, val restrictedUntil: String? = null,
    val history: List<SubTrustHistoryDto>? = null,
) {
    fun toDomain() = com.muyeon.domain.model.account.SubTrust(
        points90d?.toInt() ?: 0, cancels90d?.toInt() ?: 0, restrictedUntil?.takeIf { it.isNotBlank() },
        history.orEmpty().mapNotNull { h ->
            h.id?.let {
                com.muyeon.domain.model.account.SubTrustEntry(
                    it, h.createdAt, h.postingTitle?.takeIf { t -> t.isNotBlank() }, h.side,
                    h.reason?.contains(Regex("NO_?SHOW", RegexOption.IGNORE_CASE)) == true,
                    h.points?.toInt() ?: 0, h.revokedAt != null, h.expiresAt,
                )
            }
        },
    )
}

/** GET·PATCH /auth/me/profile — 레슨 전공(genres)만. */
@JsonClass(generateAdapter = true)
data class ProfileGenresDto(val genres: List<String>? = null)
