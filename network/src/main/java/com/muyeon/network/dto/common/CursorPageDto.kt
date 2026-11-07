package com.muyeon.network.dto.common

import com.squareup.moshi.JsonClass

/** 무용연 커서 목록 응답 `{ items, hasMore, nextCursor }` 공통 모양. */
@JsonClass(generateAdapter = true)
data class CursorPageDto<T>(
    val items: List<T>? = null,
    val hasMore: Boolean? = null,
    val nextCursor: String? = null,
) {
    /** 끝 판단은 nextCursor(hasMore=false 면 커서가 와도 끝으로 본다). */
    val next: String? get() = nextCursor?.takeIf { it.isNotBlank() && hasMore != false }
}
