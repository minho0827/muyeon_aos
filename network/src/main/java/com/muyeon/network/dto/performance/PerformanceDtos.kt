package com.muyeon.network.dto.performance

import com.muyeon.domain.model.performance.PerformanceDetail
import com.muyeon.domain.model.performance.PerformanceStatus
import com.muyeon.domain.model.performance.PerformanceSummary
import com.squareup.moshi.JsonClass

/**
 * 공연 — performance.entity.ts + status(서비스 enrich). 목록은 description·images·details 를 뺀 같은 모양.
 *   ⚠️ 서버 null 칸은 전부 `?`.
 */
@JsonClass(generateAdapter = true)
data class PerformanceDto(
    val id: Long? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val genre: String? = null,
    val subGenre: String? = null,
    val status: String? = null,
    val company: String? = null,
    val posterUrl: String? = null,
    val imageUrl: String? = null,
    val images: List<String>? = null,
    val venue: String? = null,
    val address: String? = null,
    val region: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val showtimes: String? = null,
    val runtime: String? = null,
    val ageLimit: String? = null,
    val priceInfo: String? = null,
    val bookingUrl: String? = null,
    val description: String? = null,
    val cast: String? = null,
    val detailType: String? = null,
    val detailImages: List<String>? = null,
    val details: PerformanceDetailsDto? = null,
) {
    fun toSummary(): PerformanceSummary? {
        val pid = id ?: return null
        return PerformanceSummary(
            id = pid,
            title = title.orEmpty(),
            genre = genre?.takeIf { it.isNotBlank() },
            subGenre = subGenre?.takeIf { it.isNotBlank() },
            status = PerformanceStatus.of(status),
            // 웹 목록: posterUrl || imageUrl.
            posterUrl = (posterUrl ?: imageUrl)?.takeIf { it.isNotBlank() },
            company = company?.takeIf { it.isNotBlank() },
            venue = venue?.takeIf { it.isNotBlank() },
            startAt = startAt,
            endAt = endAt,
        )
    }

    fun toDetail(): PerformanceDetail? {
        val s = toSummary() ?: return null
        return PerformanceDetail(
            summary = s,
            subtitle = subtitle?.takeIf { it.isNotBlank() },
            images = images.orEmpty().filter { it.isNotBlank() },
            address = address?.takeIf { it.isNotBlank() },
            region = region?.takeIf { it.isNotBlank() },
            showtimes = showtimes?.takeIf { it.isNotBlank() },
            runtime = runtime?.takeIf { it.isNotBlank() },
            ageLimit = ageLimit?.takeIf { it.isNotBlank() },
            priceInfo = priceInfo?.takeIf { it.isNotBlank() },
            bookingUrl = bookingUrl?.takeIf { it.isNotBlank() },
            description = description?.takeIf { it.isNotBlank() },
            cast = cast?.takeIf { it.isNotBlank() },
            detailType = detailType,
            detailImages = detailImages.orEmpty().filter { it.isNotBlank() },
            source = details?.source,
            sourceName = details?.sourceName,
            sourceUrl = details?.sourceUrl?.takeIf { it.isNotBlank() },
            collectedAt = details?.collectedAt,
        )
    }
}

/** details(확장 필드) 중 화면이 쓰는 것만. */
@JsonClass(generateAdapter = true)
data class PerformanceDetailsDto(
    val source: String? = null,
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val collectedAt: String? = null,
)
