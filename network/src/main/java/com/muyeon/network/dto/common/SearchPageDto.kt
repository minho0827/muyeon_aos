package com.muyeon.network.dto.common

import com.squareup.moshi.JsonClass

/** 검색 응답 `{ items, hasMore, nextCursor, total }` — 커서 목록 + 조건 전체 개수. */
@JsonClass(generateAdapter = true)
data class SearchPageDto<T>(
    val items: List<T>? = null,
    val hasMore: Boolean? = null,
    val nextCursor: String? = null,
    val total: Int? = null,
) {
    /** 끝 판단은 CursorPageDto 와 같다. */
    val next: String? get() = nextCursor?.takeIf { it.isNotBlank() && hasMore != false }
}

/** 검색 개수 응답 `{ total }`. */
@JsonClass(generateAdapter = true)
data class SearchCountDto(val total: Int? = null)
