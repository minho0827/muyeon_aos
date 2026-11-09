package com.muyeon.domain.model.search

/** 검색 한 페이지 — `{ items, hasMore, nextCursor, total }`. [total] 은 조건 전체 개수(모르면 null). */
data class SearchPage<T>(val items: List<T>, val nextCursor: String?, val total: Int?)

/**
 * 검색 필터 고른 값 — 섹션 키(status·genre·region·fee …)마다 값 여러 개.
 *   화면·도메인이 같이 쓴다. 빈 섹션은 들고 있지 않는다.
 */
data class SearchFilter(val values: Map<String, Set<String>> = emptyMap()) {
    val isEmpty: Boolean get() = values.values.all { it.isEmpty() }

    /** 고른 값 전체 개수. */
    val count: Int get() = values.values.sumOf { it.size }

    fun of(key: String): Set<String> = values[key].orEmpty()

    /** 값 하나 켜고 끄기. [single] 이면 그 섹션에 하나만(다시 누르면 해제). */
    fun toggle(key: String, value: String, single: Boolean = false): SearchFilter {
        val cur = of(key)
        val next = when {
            value in cur -> cur - value
            single -> setOf(value)
            else -> cur + value
        }
        return with(key, next)
    }

    fun with(key: String, set: Set<String>): SearchFilter =
        SearchFilter(if (set.isEmpty()) values - key else values + (key to set))

    /** [keys] 섹션만 비운다(null 이면 전부). */
    fun cleared(keys: Collection<String>? = null): SearchFilter =
        if (keys == null) SearchFilter() else SearchFilter(values - keys.toSet())

    /** 콤마로 이은 값 — [order] 순서(칩 순서)로. 없으면 null. */
    fun csv(key: String, order: List<String>): String? {
        val set = of(key)
        if (set.isEmpty()) return null
        return (order.filter { it in set } + set.filter { it !in order }).joinToString(",")
    }

    companion object {
        val Empty = SearchFilter()
    }
}

/** 검색어 정리 — 앞뒤 공백을 빼고, 2자 미만이면 보내지 않는다(서버도 무시). */
object SearchQueryText {
    const val MIN_LENGTH = 2
    fun normalize(q: String?): String? = q?.trim()?.replace(Regex("\\s+"), " ")?.takeIf { it.length >= MIN_LENGTH }
}

/** 공연 검색 조건 — `GET /performances/search`. 값은 서버 값 그대로(장르·시·도는 한글, 상태·정렬은 영문 코드). */
data class PerformanceSearchParams(
    val q: String?,
    val statuses: String?,
    val genres: String?,
    val regions: String?,
    val sort: String,
) {
    /** 쿼리 문자열 맵 — 빈 값은 뺀다. 커서·limit 는 따로. */
    fun toQuery(): Map<String, String> = buildMap {
        q?.let { put("q", it) }
        statuses?.let { put("statuses", it) }
        genres?.let { put("genres", it) }
        regions?.let { put("regions", it) }
        put("sort", sort)
    }
}
