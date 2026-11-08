package com.muyeon.app.ui.quotefeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.model.quotefeed.QuoteFeedCategories
import com.muyeon.domain.model.quotefeed.QuoteFeedItem
import com.muyeon.domain.usecase.quotefeed.QuoteFeedUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuoteFeedUiState(
    /** [QuoteFeedViewModel.ALL] 또는 장르 id(ballet …). */
    val categoryId: String = QuoteFeedViewModel.ALL,
    val grid: Boolean = true,
    /** 강사·학원 활동 중 — 서버 페이지 목록 · '제안 보냄' 표시. 아니면 최근 10건. */
    val canBrowseAll: Boolean = false,
    val items: List<QuoteFeedItem> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** 첫 페이지 실패(빈 목록과 다른 화면). */
    val errorMessage: String? = null,
    val moreError: String? = null,
)

/**
 * ★ 2026-11-08 '레슨을 찾는 회원들' 전체보기 — 레슨 전체보기(LessonListViewModel)와 같은 틀:
 *   장르 칩(전체 + 견적 장르) · 그리드/목록 전환(기억한다) · 아래로 내리면 다음 페이지.
 *   강사·학원은 GET /quotes/available(웹 /availableQuotes 목록), 그 밖은 /quotes/home 10건(QuoteFeedUseCase).
 */
class QuoteFeedViewModel(
    private val feed: QuoteFeedUseCase,
    private val prefs: KeyValueStore,
) : ViewModel() {

    val categoryOptions: List<String> = listOf(ALL) + QuoteFeedCategories.ALL

    private val _state = MutableStateFlow(
        QuoteFeedUiState(
            categoryId = prefs.getString(K_CATEGORY)?.takeIf { it in categoryOptions } ?: ALL,
            grid = prefs.getString(K_VIEW) != "basic",
            canBrowseAll = feed.canBrowseAll,
        ),
    )
    val state: StateFlow<QuoteFeedUiState> = _state.asStateFlow()
    private var loadJob: Job? = null
    private var started = false

    /** 화면이 보일 때마다 — 처음이면 읽고, 유형이 바뀌었으면(전환하고 돌아옴) 다시 읽는다. */
    fun onResume() {
        val pro = feed.canBrowseAll
        if (!started || pro != _state.value.canBrowseAll) {
            started = true
            _state.update { it.copy(canBrowseAll = pro) }
            refresh()
        }
    }

    fun selectCategory(id: String) {
        if (id == _state.value.categoryId) return
        prefs.putString(K_CATEGORY, id)
        _state.update { it.copy(categoryId = id, items = emptyList(), page = 0, hasMore = false) }
        refresh()
    }

    fun setGrid(grid: Boolean) {
        prefs.putString(K_VIEW, if (grid) "half" else "basic")
        _state.update { it.copy(grid = grid) }
    }

    private fun categoryParam() = _state.value.categoryId.takeIf { it != ALL }

    fun refresh(pull: Boolean = false) {
        loadJob?.cancel()
        _state.update { it.copy(isInitialLoading = !pull && it.items.isEmpty(), isRefreshing = pull, errorMessage = null, moreError = null) }
        val cat = categoryParam()
        loadJob = viewModelScope.launch {
            when (val r = feed.page(cat, 0)) {
                is ApiResult.Success -> _state.update {
                    it.copy(items = r.data.items, page = r.data.page, hasMore = r.data.hasMore, isInitialLoading = false, isRefreshing = false)
                }
                is ApiResult.Error -> _state.update { it.copy(isInitialLoading = false, isRefreshing = false, errorMessage = r.message) }
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoadingMore || s.isInitialLoading || !s.hasMore) return
        _state.update { it.copy(isLoadingMore = true, moreError = null) }
        val cat = categoryParam()
        loadJob = viewModelScope.launch {
            when (val r = feed.page(cat, s.page + 1)) {
                is ApiResult.Success -> _state.update { cur ->
                    val seen = cur.items.mapTo(HashSet()) { it.id }
                    cur.copy(items = cur.items + r.data.items.filter { it.id !in seen }, page = r.data.page, hasMore = r.data.hasMore, isLoadingMore = false)
                }
                is ApiResult.Error -> _state.update { it.copy(isLoadingMore = false, moreError = r.message) }
            }
        }
    }

    /** 이미 제안을 보낸 요청(강사·학원 활동 중일 때만 — 웹 HomeQuoteRequests sent). */
    fun sent(item: QuoteFeedItem): Boolean = _state.value.canBrowseAll && !item.isMine && !item.canRespond

    companion object {
        const val ALL = "ALL"
        private const val K_CATEGORY = "quoteFeed.category"
        private const val K_VIEW = "quoteFeed.view"
    }
}
