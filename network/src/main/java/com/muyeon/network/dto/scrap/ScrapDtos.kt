package com.muyeon.network.dto.scrap

import com.squareup.moshi.JsonClass

/**
 * ★ 2026-11-08 찜 모아보기(웹 Likes) — `GET /me/scraps` 한 줄 + 카드 요약(scraps.service pick*). 종류마다 채우는 칸이 달라 전부 선택값.
 */
@JsonClass(generateAdapter = true)
data class ScrapEntryDto(
    val targetType: String? = null,
    val targetId: Long? = null,
    val item: ScrapSummaryDto? = null,
) {
    fun toDomain(): com.muyeon.domain.model.scrap.LikedItem? {
        val type = targetType ?: return null
        val id = targetId ?: return null
        val it = item ?: return null
        return com.muyeon.domain.model.scrap.LikedItem(
            targetType = type, targetId = id,
            title = (it.title ?: it.name).orEmpty(),
            image = it.image?.takeIf { s -> s.isNotBlank() },
            region = it.region?.takeIf { s -> s.isNotBlank() },
            genre = it.genre?.takeIf { s -> s.isNotBlank() },
            venue = it.venue?.takeIf { s -> s.isNotBlank() },
            price = it.price?.toInt(),
            pricePerHour = it.pricePerHour?.toInt(),
            fields = it.fields.orEmpty(),
            genres = it.genres.orEmpty(),
            career = it.career,
            salary = it.salary,
            category = it.category,
            status = it.status,
            // ★ 2026-11-19 탈퇴한 강사·무용수 — 서버가 이름을 '탈퇴한 회원'으로 바꿔 보낸다.
            withdrawn = it.withdrawn == true && type in setOf("TEACHER", "DANCER"),
        )
    }
}

@JsonClass(generateAdapter = true)
data class ScrapSummaryDto(
    val title: String? = null, val name: String? = null, val image: String? = null, val region: String? = null,
    val genre: String? = null, val venue: String? = null, val price: Double? = null, val pricePerHour: Double? = null,
    val fields: List<String>? = null, val genres: List<String>? = null, val career: String? = null, val salary: String? = null,
    val category: String? = null, val status: String? = null,
    /** ★ 2026-11-19 TEACHER·DANCER 대상이 탈퇴했으면 true(사진·장르·경력은 비어 온다). */
    val withdrawn: Boolean? = null,
)

/** `GET /me/scraps` 한 줄(대상 요약이 붙지만 찜 표시는 id 만 쓴다). */
@JsonClass(generateAdapter = true)
data class ScrapItemDto(
    val targetId: Long? = null,
    val targetType: String? = null,
)
