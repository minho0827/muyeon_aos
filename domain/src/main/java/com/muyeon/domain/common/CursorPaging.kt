package com.muyeon.domain.common

/**
 * 커서 목록 공통 규칙(PaceERA CursorPaging 을 무용연 서버 계약에 맞춤).
 *
 *   무용연 서버 계약(`/teachers/page`, `/performances/page`, `/home/feed/page` …):
 *   - 요청: `cursor`(첫 페이지는 생략) + `limit`
 *   - 응답: `{ items, hasMore, nextCursor }`
 *   - 끝 판단은 `nextCursor == null`(또는 hasMore=false). 받은 개수로 판단하지 않는다.
 */
object CursorPaging {
    const val DEFAULT_LIMIT = 30

    /** 새 항목 0개 페이지를 몇 장까지 이어 받는가 — 넘으면 [CursorState.stalled] 로 [더 보기]. */
    const val MAX_EMPTY_PAGES = 3
}

/** 서버 한 페이지 — 목록과 다음 커서. */
data class CursorPage<T>(val items: List<T>, val nextCursor: String?)

/**
 * 커서 목록 진행 상태 — 화면 상태(UiState)에 그대로 넣어 쓴다.
 *   [loaded] 첫 페이지를 받은 적이 있는가 / [nextCursor] 다음 요청 커서(null 이면 끝)
 *   [failed] 마지막 다음 페이지 요청이 실패(저절로 다시 부르지 않고 [다시 시도])
 *   [stalled] 빈 페이지만 연달아 받음([더 보기])
 */
data class CursorState(
    val loaded: Boolean = false,
    val nextCursor: String? = null,
    val failed: Boolean = false,
    val stalled: Boolean = false,
) {
    val hasMore: Boolean get() = loaded && nextCursor != null
    val canAutoLoad: Boolean get() = hasMore && !failed && !stalled
    val needsTap: Boolean get() = hasMore && (failed || stalled)

    fun retrying(): CursorState = copy(failed = false, stalled = false)

    fun applied(fetch: CursorFetch<*>): CursorState {
        if (fetch.pages == 0) return copy(failed = fetch.error != null)
        return CursorState(
            loaded = true,
            nextCursor = fetch.nextCursor,
            failed = fetch.error != null,
            stalled = fetch.error == null && fetch.items.isEmpty() && fetch.nextCursor != null,
        )
    }

    companion object {
        val Initial = CursorState()
    }
}

/** 받은 페이지를 붙인다. [replace] 면 첫 페이지라 통째로 바꾼다. 이미 있는 id 는 버린다(앞의 값을 지킨다). */
fun <T, K> mergeCursorPage(current: List<T>, incoming: List<T>, replace: Boolean, idOf: (T) -> K): List<T> {
    if (replace) return incoming.distinctBy(idOf)
    if (incoming.isEmpty()) return current
    val seen = current.mapTo(HashSet(), idOf)
    val added = incoming.filter { seen.add(idOf(it)) }
    return if (added.isEmpty()) current else current + added
}

/** [fetchCursorPages] 한 번의 결과. [pages] 0 이면 첫 요청부터 실패. */
data class CursorFetch<T>(
    val items: List<T>,
    val nextCursor: String?,
    val pages: Int,
    val error: String? = null,
)

/**
 * "다음 페이지" 공통 루프 — 새 항목이 하나라도 붙거나, 끝이거나, 빈 페이지를 [maxPages] 장 받을 때까지 이어 받는다.
 *   [start] 가 null 이면 첫 페이지부터(이때 [existingIds] 는 비워서 준다).
 */
suspend fun <T, K> fetchCursorPages(
    start: String?,
    existingIds: Set<K>,
    idOf: (T) -> K,
    maxPages: Int = CursorPaging.MAX_EMPTY_PAGES,
    fetch: suspend (cursor: String?) -> ApiResult<CursorPage<T>>,
): CursorFetch<T> {
    val seen = HashSet(existingIds)
    val collected = ArrayList<T>()
    var cursor = start
    var next: String? = start
    var pages = 0
    while (true) {
        when (val r = fetch(cursor)) {
            is ApiResult.Success -> {
                pages++
                next = r.data.nextCursor
                r.data.items.filterTo(collected) { seen.add(idOf(it)) }
                if (collected.isNotEmpty() || next == null || pages >= maxPages) {
                    return CursorFetch(collected, next, pages)
                }
                cursor = next
            }
            is ApiResult.Error -> return CursorFetch(collected, next, pages, r.message)
        }
    }
}
