package com.muyeon.domain.model.performance

/** 공연 상태 — 서버가 기간으로 계산한다(performances.service statusOf). 모르는 값은 null. */
enum class PerformanceStatus { UPCOMING, ONGOING, ENDED;
    companion object { fun of(v: String?): PerformanceStatus? = entries.firstOrNull { it.name == v } }
}

/** 공연 목록 한 줄 — `GET /performances/page` items. 날짜는 서버 ISO 문자열 그대로(화면이 한국 날짜로). */
data class PerformanceSummary(
    val id: Long,
    val title: String,
    val genre: String?,
    val subGenre: String?,
    val status: PerformanceStatus?,
    val posterUrl: String?,
    val company: String?,
    val venue: String?,
    val startAt: String?,
    val endAt: String?,
)

/** 공연 상세 — `GET /performances/:id`. */
data class PerformanceDetail(
    val summary: PerformanceSummary,
    val subtitle: String?,
    val images: List<String>,
    val address: String?,
    val region: String?,
    val showtimes: String?,
    val runtime: String?,
    val ageLimit: String?,
    val priceInfo: String?,
    val bookingUrl: String?,
    val description: String?,
    val cast: String?,
    /** "IMAGE" 면 정보 칸 대신 [detailImages] 만 보인다(웹과 같다). */
    val detailType: String?,
    val detailImages: List<String>,
    /** details.source — "KOPIS" | "WEB" | null. */
    val source: String?,
    val sourceName: String?,
    val sourceUrl: String?,
    val collectedAt: String?,
)

/**
 * 공연 분류 — 백엔드 performance-category.ts · 웹 mock/muyeonMock.js 와 1:1(코드만. 화면 이름은 앱 strings.xml).
 *   장르 값은 서버 값 그대로(한글). 목록 칩에서는 바레·발레핏을 뺀다(웹 PerformanceList).
 */
object PerformanceCategory {
    val GENRES = listOf("발레", "한국무용", "현대무용", "실용무용", "뮤지컬", "기타")
    val SUB_GENRES: Map<String, List<String>> = mapOf(
        "발레" to listOf("CLASSIC_FULL", "GALA", "CONTEMPORARY_BALLET"),
        "한국무용" to listOf("TRADITIONAL", "CREATIVE_KOREAN"),
        "실용무용" to listOf("STREET", "KPOP"),
    )
    val TAGS = listOf("FESTIVAL", "FAMILY")
}

/** 목록 두 번째 줄 선택 — 세부 분류 하나 또는 태그 하나(웹 "SUB:코드" / "TAG:코드" / "ALL"). */
sealed interface PerformanceDetailFilter {
    val key: String
    data object All : PerformanceDetailFilter { override val key = "ALL" }
    data class Sub(val code: String) : PerformanceDetailFilter { override val key = "SUB:$code" }
    data class Tag(val code: String) : PerformanceDetailFilter { override val key = "TAG:$code" }

    companion object {
        fun fromKey(key: String?): PerformanceDetailFilter = when {
            key == null -> All
            key.startsWith("SUB:") -> Sub(key.removePrefix("SUB:"))
            key.startsWith("TAG:") -> Tag(key.removePrefix("TAG:"))
            else -> All
        }
    }
}
