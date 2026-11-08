package com.muyeon.app.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.navigation.collectAppResults
import com.muyeon.domain.common.CursorState
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.common.fetchCursorPages
import com.muyeon.domain.common.mergeCursorPage
import com.muyeon.domain.model.community.CommunityCategory
import com.muyeon.domain.model.community.CommunityPostSummary
import com.muyeon.domain.usecase.community.GetCommunityPostsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunityListUiState(
    val category: String = CommunityListViewModel.ALL,
    val items: List<CommunityPostSummary> = emptyList(),
    val cursor: CursorState = CursorState.Initial,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val moreError: String? = null,
)

sealed interface CommunityListEvent {
    data object ScrollToTop : CommunityListEvent
}

/**
 * 커뮤니티 탭 — 웹 pages/muyeon/community/CommunityHome.js. 카테고리 칩(기억) + 최신순 목록.
 *   웹은 페이지 없이 전부 받지만 앱은 같은 데이터를 커서로 나눠 받는다(/community/posts/page).
 */
class CommunityListViewModel(
    private val getPosts: GetCommunityPostsUseCase,
    private val prefs: KeyValueStore,
    private val tokens: TokenStore,
) : ViewModel() {
    val categories: List<String> = listOf(ALL) + CommunityCategory.ALL_CODES

    private val _state = MutableStateFlow(CommunityListUiState(category = prefs.getString(KEY)?.takeIf { it in categories } ?: ALL))
    val state: StateFlow<CommunityListUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CommunityListEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<CommunityListEvent> = _events.asSharedFlow()

    private var loadJob: Job? = null
    private var wasLoggedIn = tokens.isLoggedIn

    init {
        refresh()
        // 글 상세에서 지우거나 웹 창에서 새 글을 쓰고 돌아오면 다시 받는다.
        viewModelScope.collectAppResults(RESULT_COMMUNITY) { refresh(silent = true) }
    }

    /** 로그인·로그아웃 뒤 — 차단한 사람 글이 걸러지는지가 달라진다. */
    fun onResume() {
        if (tokens.isLoggedIn != wasLoggedIn) { wasLoggedIn = tokens.isLoggedIn; refresh(silent = true) }
    }

    fun select(category: String) {
        if (category == _state.value.category) return
        prefs.putString(KEY, category)
        _state.update { it.copy(category = category, items = emptyList(), cursor = CursorState.Initial) }
        refresh()
    }

    /** 탭 다시 누르기 — 다음 카테고리 칩 + 맨 위로(웹 useTabReselect). */
    fun onTabReselect() {
        val i = categories.indexOf(_state.value.category)
        select(categories[(i + 1) % categories.size])
        _events.tryEmit(CommunityListEvent.ScrollToTop)
    }

    fun refresh(pull: Boolean = false, silent: Boolean = false) {
        loadJob?.cancel()
        _state.update { it.copy(isInitialLoading = !pull && !silent && it.items.isEmpty(), isRefreshing = pull, errorMessage = null, moreError = null) }
        loadJob = viewModelScope.launch {
            val c = _state.value.category.takeIf { it != ALL }
            val fetch = fetchCursorPages(null, emptySet<Long>(), { it.id }) { cur -> getPosts(c, cur) }
            _state.update {
                it.copy(
                    items = if (fetch.pages > 0) mergeCursorPage(it.items, fetch.items, replace = true) { p -> p.id } else it.items,
                    cursor = it.cursor.applied(fetch),
                    isInitialLoading = false,
                    isRefreshing = false,
                    errorMessage = if (fetch.pages == 0) fetch.error else null,
                )
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoadingMore || s.isInitialLoading || !s.cursor.hasMore) return
        _state.update { it.copy(isLoadingMore = true, moreError = null, cursor = it.cursor.retrying()) }
        viewModelScope.launch {
            val c = s.category.takeIf { it != ALL }
            val fetch = fetchCursorPages(s.cursor.nextCursor, s.items.mapTo(HashSet()) { it.id }, { it.id }) { cur -> getPosts(c, cur) }
            _state.update {
                it.copy(
                    items = mergeCursorPage(it.items, fetch.items, replace = false) { p -> p.id },
                    cursor = it.cursor.applied(fetch),
                    isLoadingMore = false,
                    moreError = fetch.error,
                )
            }
        }
    }

    companion object {
        const val ALL = "ALL"
        private const val KEY = "community.category"

        /** ★ 2026-11-08 묶음 0 — 웹 /community/:category 로 열 때 그 칩을 먼저 골라 둔다(목록이 열리며 읽는다). */
        fun preselect(prefs: KeyValueStore, category: String) {
            if (category in CommunityCategory.ALL_CODES) prefs.putString(KEY, category)
        }
        /** 앱 안 결과 키 — 글을 지우거나 새로 썼다(웹 창 포함). */
        const val RESULT_COMMUNITY = "COMMUNITY"
    }
}
