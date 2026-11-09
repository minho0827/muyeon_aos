package com.muyeon.app.ui.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorState
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.common.fetchCursorPages
import com.muyeon.domain.common.mergeCursorPage
import com.muyeon.domain.model.performance.PerformanceCategory
import com.muyeon.domain.model.performance.PerformanceDetailFilter
import com.muyeon.domain.model.performance.PerformanceSummary
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.usecase.performance.GetPerformancePageUseCase
import com.muyeon.domain.usecase.scrap.GetLikedIdsUseCase
import com.muyeon.domain.usecase.scrap.ToggleScrapResult
import com.muyeon.domain.usecase.scrap.ToggleScrapUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PerformanceListUiState(
    /** [PerformanceListViewModel.ALL] 또는 장르 값. */
    val genre: String = PerformanceListViewModel.ALL,
    val detail: PerformanceDetailFilter = PerformanceDetailFilter.All,
    val items: List<PerformanceSummary> = emptyList(),
    val cursor: CursorState = CursorState.Initial,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** 첫 페이지 실패(빈 목록과 다른 화면). */
    val errorMessage: String? = null,
    /** 다음 페이지 실패 문구. */
    val moreError: String? = null,
    val likedIds: Set<Long> = emptySet(),
) {
    /** 두 번째 줄 — 고른 장르의 세부 분류만. ★ 11-09 태그(축제·가족) 칩은 뺐다(쓰는 공연이 거의 없음, 사용자 결정) —
     *  [전체]면 [전체] 하나뿐이라 줄이 안 보인다(size > 1 일 때만 그림). 예전에 고른 TAG 는 [전체]로 돌아간다. */
    val detailOptions: List<PerformanceDetailFilter>
        get() = listOf(PerformanceDetailFilter.All) +
            PerformanceCategory.SUB_GENRES[genre].orEmpty().map { PerformanceDetailFilter.Sub(it) }
}

sealed interface PerformanceListEvent {
    data class ScrapToast(val added: Boolean) : PerformanceListEvent
    data class Message(val text: String) : PerformanceListEvent
    /** 비회원 찜 — 로그인 창(돌아오면 목록으로). */
    data object LoginRequired : PerformanceListEvent
    data object ScrollToTop : PerformanceListEvent
}

/**
 * 공연 탭 — 웹 pages/muyeon/performance/PerformanceList.js.
 *   장르 칩(바레·발레핏 제외) + 세부 분류/태그 칩, 커서 무한 스크롤, 찜. 칩 선택은 기억한다(웹 usePersistedState).
 */
class PerformanceListViewModel(
    private val getPage: GetPerformancePageUseCase,
    private val getLikedIds: GetLikedIdsUseCase,
    private val toggleScrap: ToggleScrapUseCase,
    private val observeScrap: com.muyeon.domain.usecase.scrap.ObserveScrapChangesUseCase,
    private val prefs: KeyValueStore,
    private val tokens: TokenStore,
) : ViewModel() {

    val genreOptions: List<String> = listOf(ALL) + PerformanceCategory.GENRES

    private val _state = MutableStateFlow(
        PerformanceListUiState(
            genre = prefs.getString(KEY_GENRE)?.takeIf { it in genreOptions } ?: ALL,
            detail = PerformanceDetailFilter.fromKey(prefs.getString(KEY_DETAIL)),
        ).let { s -> if (s.detailOptions.none { it.key == s.detail.key }) s.copy(detail = PerformanceDetailFilter.All) else s },
    )
    val state: StateFlow<PerformanceListUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PerformanceListEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<PerformanceListEvent> = _events.asSharedFlow()

    private var loadJob: Job? = null
    private var wasLoggedIn = tokens.isLoggedIn

    init {
        refresh()
        loadLikes()
        // 상세에서 찜을 바꿨다 — 목록 하트도 맞춘다.
        viewModelScope.launch { observeScrap(ScrapTarget.PERFORMANCE).collect { c -> setLiked(c.id, c.liked) } }
    }

    /** 화면 복귀 — 로그인 상태가 바뀌었으면(웹 로그인 창에서 돌아옴) 찜을 다시 읽는다. */
    fun onResume() {
        if (tokens.isLoggedIn != wasLoggedIn) {
            wasLoggedIn = tokens.isLoggedIn
            loadLikes()
        }
    }

    fun selectGenre(genre: String) {
        val s = _state.value
        if (genre == s.genre) return
        prefs.putString(KEY_GENRE, genre)
        prefs.putString(KEY_DETAIL, PerformanceDetailFilter.All.key)
        // 장르를 바꾸면 두 번째 줄은 전체로(웹 changeGenre).
        _state.update { it.copy(genre = genre, detail = PerformanceDetailFilter.All, items = emptyList(), cursor = CursorState.Initial) }
        refresh()
    }

    fun selectDetail(detail: PerformanceDetailFilter) {
        if (detail.key == _state.value.detail.key) return
        prefs.putString(KEY_DETAIL, detail.key)
        _state.update { it.copy(detail = detail, items = emptyList(), cursor = CursorState.Initial) }
        refresh()
    }

    /** 탭 다시 누르기 — 다음 장르 칩(끝이면 처음), 맨 위로(웹 useTabReselect + nextChipValue). */
    fun onTabReselect() {
        val i = genreOptions.indexOf(_state.value.genre)
        selectGenre(genreOptions[(i + 1) % genreOptions.size])
        _events.tryEmit(PerformanceListEvent.ScrollToTop)
    }

    fun refresh(pull: Boolean = false) {
        loadJob?.cancel()
        _state.update { it.copy(isInitialLoading = !pull && it.items.isEmpty(), isRefreshing = pull, errorMessage = null, moreError = null) }
        loadJob = viewModelScope.launch {
            val s = _state.value
            val fetch = fetchCursorPages(null, emptySet<Long>(), { it.id }) { c -> getPage(s.genre.takeIf { it != ALL }, s.detail, c) }
            _state.update {
                it.copy(
                    items = mergeCursorPage(it.items, fetch.items, replace = fetch.pages > 0) { p -> p.id },
                    cursor = it.cursor.applied(fetch),
                    isInitialLoading = false,
                    isRefreshing = false,
                    errorMessage = if (fetch.pages == 0) fetch.error else null,
                )
            }
            if (pull) loadLikes()
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoadingMore || s.isInitialLoading || !s.cursor.hasMore) return
        _state.update { it.copy(isLoadingMore = true, moreError = null, cursor = it.cursor.retrying()) }
        viewModelScope.launch {
            val fetch = fetchCursorPages(s.cursor.nextCursor, s.items.mapTo(HashSet()) { it.id }, { it.id }) { c ->
                getPage(s.genre.takeIf { it != ALL }, s.detail, c)
            }
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

    fun toggleLike(id: Long) {
        val was = id in _state.value.likedIds
        // 먼저 바꿔 보이고(웹 낙관적 업데이트), 실패하면 되돌린다.
        if (tokens.isLoggedIn) _state.update { it.copy(likedIds = if (was) it.likedIds - id else it.likedIds + id) }
        viewModelScope.launch {
            when (val r = toggleScrap(ScrapTarget.PERFORMANCE, id, was)) {
                is ToggleScrapResult.Done -> _events.tryEmit(PerformanceListEvent.ScrapToast(r.liked))
                is ToggleScrapResult.Failed -> {
                    _state.update { it.copy(likedIds = if (was) it.likedIds + id else it.likedIds - id) }
                    _events.tryEmit(PerformanceListEvent.Message(r.message))
                }
                ToggleScrapResult.LoginRequired -> _events.tryEmit(PerformanceListEvent.LoginRequired)
            }
        }
    }

    /** 상세에서 찜을 바꾸고 돌아왔다. */
    fun setLiked(id: Long, liked: Boolean) = _state.update { it.copy(likedIds = if (liked) it.likedIds + id else it.likedIds - id) }

    private fun loadLikes() {
        viewModelScope.launch {
            val ids = getLikedIds(ScrapTarget.PERFORMANCE)
            _state.update { it.copy(likedIds = ids) }
        }
    }

    companion object {
        const val ALL = "ALL"
        private const val KEY_GENRE = "performance.genre"
        private const val KEY_DETAIL = "performance.detail"
    }
}
