package com.muyeon.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.common.CursorState
import com.muyeon.domain.common.fetchCursorPages
import com.muyeon.domain.common.mergeCursorPage
import com.muyeon.domain.model.search.SearchFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState<T>(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.Empty,
    val sort: String,
    val items: List<T> = emptyList(),
    val total: Int? = null,
    val cursor: CursorState = CursorState.Initial,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    /** 첫 페이지 실패. */
    val error: String? = null,
    val moreError: String? = null,
    /** 열린 시트의 'N개 보기' 개수(시트 안 임시 선택 기준). null = 아직 모름. */
    val draftCount: Int? = null,
)

/**
 * 검색 결과 한 도메인 — 도메인마다 하나(탭을 오가도 결과·필터를 지킨다).
 *   검색어는 300ms 쉬면 부르고, 필터·정렬은 바로 다시 부른다. 커서로 이어 받는다.
 */
class SearchViewModel<T : Any>(private val domain: SearchDomain<T>) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState<T>(sort = domain.defaultSort))
    val state: StateFlow<SearchUiState<T>> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var queryJob: Job? = null
    private var countJob: Job? = null
    private var started = false

    /** 검색어 바뀜 — 처음엔 바로, 그 뒤엔 [QUERY_DEBOUNCE_MS] 쉬고 부른다. */
    fun setQuery(q: String) {
        if (started && q == _state.value.query) return
        _state.update { it.copy(query = q) }
        queryJob?.cancel()
        if (!started) { started = true; refresh(); return }
        queryJob = viewModelScope.launch { delay(QUERY_DEBOUNCE_MS); refresh() }
    }

    fun applyFilter(filter: SearchFilter) {
        if (filter == _state.value.filter) return
        _state.update { it.copy(filter = filter) }
        refresh()
    }

    fun selectSort(sort: String) {
        if (sort == _state.value.sort) return
        _state.update { it.copy(sort = sort) }
        refresh()
    }

    /** 시트 안 선택이 바뀜 — 같은 조건 개수를 잠깐 쉬고 받는다. */
    fun requestCount(draft: SearchFilter) {
        countJob?.cancel()
        _state.update { it.copy(draftCount = null) }
        countJob = viewModelScope.launch {
            delay(COUNT_DEBOUNCE_MS)
            val n = domain.count(_state.value.query, draft)
            _state.update { it.copy(draftCount = n) }
        }
    }

    fun refresh() {
        loadJob?.cancel()
        queryJob?.cancel()
        _state.update { it.copy(isLoading = true, error = null, moreError = null, cursor = CursorState.Initial) }
        loadJob = viewModelScope.launch {
            val s = _state.value
            var total: Int? = null
            val fetch = fetchCursorPages(null, emptySet<Any>(), domain::idOf) { c -> page(s, c) { total = it } }
            _state.update {
                it.copy(
                    items = if (fetch.pages > 0) mergeCursorPage(emptyList(), fetch.items, replace = true, domain::idOf) else emptyList(),
                    total = total,
                    cursor = it.cursor.applied(fetch),
                    isLoading = false,
                    error = if (fetch.pages == 0) fetch.error else null,
                )
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoading || s.isLoadingMore || !s.cursor.hasMore) return
        _state.update { it.copy(isLoadingMore = true, moreError = null, cursor = it.cursor.retrying()) }
        loadJob = viewModelScope.launch {
            val fetch = fetchCursorPages(s.cursor.nextCursor, s.items.mapTo(HashSet<Any>()) { domain.idOf(it) }, domain::idOf) { c -> page(s, c) {} }
            _state.update {
                it.copy(
                    items = mergeCursorPage(it.items, fetch.items, replace = false, domain::idOf),
                    cursor = it.cursor.applied(fetch),
                    isLoadingMore = false,
                    moreError = fetch.error,
                )
            }
        }
    }

    /** 도메인 한 페이지 → 공통 커서 모양. 개수는 [onTotal] 로. */
    private suspend fun page(s: SearchUiState<T>, cursor: String?, onTotal: (Int?) -> Unit): ApiResult<CursorPage<T>> =
        domain.search(s.query, s.filter, s.sort, cursor).map { p ->
            onTotal(p.total)
            CursorPage(p.items, p.nextCursor)
        }

    companion object {
        const val QUERY_DEBOUNCE_MS = 300L
        const val COUNT_DEBOUNCE_MS = 200L
    }
}
