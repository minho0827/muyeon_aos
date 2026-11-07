package com.muyeon.network.dto.scrap

import com.squareup.moshi.JsonClass

/** `GET /me/scraps` 한 줄(대상 요약이 붙지만 찜 표시는 id 만 쓴다). */
@JsonClass(generateAdapter = true)
data class ScrapItemDto(
    val targetId: Long? = null,
    val targetType: String? = null,
)
