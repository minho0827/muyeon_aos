package com.muyeon.app.ui.lessonlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.CursorState
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.common.fetchCursorPages
import com.muyeon.domain.common.mergeCursorPage
import com.muyeon.domain.model.home.HomeLesson
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.model.teacher.LessonGenres
import com.muyeon.domain.usecase.home.HomeLessonsUseCase
import com.muyeon.domain.usecase.region.ObserveActiveRegionUseCase
import com.muyeon.domain.usecase.scrap.GetLikedIdsUseCase
import com.muyeon.domain.usecase.scrap.ObserveScrapChangesUseCase
import com.muyeon.domain.usecase.scrap.ToggleScrapResult
import com.muyeon.domain.usecase.scrap.ToggleScrapUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.muyeon.app.auth.onAccountChanged

data class LessonListUiState(
    /** [LessonListViewModel.ALL] 또는 장르. */
    val genre: String = LessonListViewModel.ALL,
    val grid: Boolean = true,
    val items: List<HomeLesson> = emptyList(),
    val cursor: CursorState = CursorState.Initial,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** 첫 페이지 실패(빈 목록과 다른 화면). */
    val errorMessage: String? = null,
    val moreError: String? = null,
    val likedIds: Set<Long> = emptySet(),
)

sealed interface LessonListEvent {
    data class ScrapToast(val added: Boolean) : LessonListEvent
    data class Message(val text: String) : LessonListEvent
    data object LoginRequired : LessonListEvent
}

/**
 * ★ 2026-11-08 홈 '전체보기' 레슨 목록 — 웹 pages/muyeon/lessons/LessonListView.js(/lessonList?title=&type=).
 *   장르 칩(전체 + 레슨 장르) · 그리드/목록 전환 · 커서 무한 스크롤 · 찜. 칩·보기 방식은 기억한다(웹 usePersistedState).
 *
 * @param type popular(추천 = 정규) · experience(학원 체험) · oneday(7일 안 원데이) · 그 밖은 조건 없음
 * @param initialGenre 홈 장르 탭에서 왔으면 그 장르 칩으로 연다(기억한 칩보다 먼저, 기억은 바꾸지 않는다).
 */
class LessonListViewModel(
    private val type: String?,
    initialGenre: String?,
    private val lessons: HomeLessonsUseCase,
    observeRegion: ObserveActiveRegionUseCase,
    private val getLikedIds: GetLikedIdsUseCase,
    private val toggleScrap: ToggleScrapUseCase,
    observeScrap: ObserveScrapChangesUseCase,
    private val prefs: KeyValueStore,
    private val tokens: TokenStore,
) : ViewModel() {

    val genreOptions: List<String> = listOf(ALL) + LessonGenres.ALL
    private val region = observeRegion()

    private val _state = MutableStateFlow(
        LessonListUiState(
            genre = initialGenre?.takeIf { it in genreOptions } ?: prefs.getString(K_GENRE)?.takeIf { it in genreOptions } ?: ALL,
            grid = prefs.getString(K_VIEW) != "basic",
        ),
    )
    val state: StateFlow<LessonListUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LessonListEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<LessonListEvent> = _events.asSharedFlow()

    private var loadJob: Job? = null
    private var wasLoggedIn = tokens.isLoggedIn

    init {
        refresh()
        loadLikes()
        viewModelScope.launch { observeScrap(ScrapTarget.LESSON).collect { c -> setLiked(c.id, c.liked) } }
        viewModelScope.launch { region.drop(1).collect { refresh() } }
        // ★ 2026-11-09 계정 바뀜(로그아웃·다른 계정) — 앞 계정 찜을 비우고 새 계정 것으로.
        onAccountChanged { wasLoggedIn = it.loggedIn; _state.update { s -> s.copy(likedIds = emptySet()) }; loadLikes() }
    }

    fun onResume() {
        if (tokens.isLoggedIn != wasLoggedIn) {
            wasLoggedIn = tokens.isLoggedIn
            loadLikes()
        }
    }

    fun selectGenre(genre: String) {
        if (genre == _state.value.genre) return
        prefs.putString(K_GENRE, genre)
        _state.update { it.copy(genre = genre, items = emptyList(), cursor = CursorState.Initial) }
        refresh()
    }

    fun setGrid(grid: Boolean) {
        prefs.putString(K_VIEW, if (grid) "half" else "basic")
        _state.update { it.copy(grid = grid) }
    }

    private fun genreParam() = _state.value.genre.takeIf { it != ALL }

    fun refresh(pull: Boolean = false) {
        loadJob?.cancel()
        _state.update { it.copy(isInitialLoading = !pull && it.items.isEmpty(), isRefreshing = pull, errorMessage = null, moreError = null) }
        val genre = genreParam()
        loadJob = viewModelScope.launch {
            val f = fetchCursorPages(null, emptySet<Long>(), { it.id }) { c -> lessons.listPage(type, genre, region.value, c) }
            _state.update {
                it.copy(
                    items = mergeCursorPage(it.items, f.items, replace = f.pages > 0) { l -> l.id },
                    cursor = it.cursor.applied(f),
                    isInitialLoading = false, isRefreshing = false,
                    errorMessage = if (f.pages == 0) f.error else null,
                )
            }
            if (pull) loadLikes()
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoadingMore || s.isInitialLoading || !s.cursor.hasMore) return
        _state.update { it.copy(isLoadingMore = true, moreError = null, cursor = it.cursor.retrying()) }
        val genre = genreParam()
        loadJob = viewModelScope.launch {
            val f = fetchCursorPages(s.cursor.nextCursor, s.items.mapTo(HashSet()) { it.id }, { it.id }) { c -> lessons.listPage(type, genre, region.value, c) }
            _state.update {
                it.copy(
                    items = mergeCursorPage(it.items, f.items, replace = false) { l -> l.id },
                    cursor = it.cursor.applied(f), isLoadingMore = false, moreError = f.error,
                )
            }
        }
    }

    fun toggleLike(id: Long) {
        val was = id in _state.value.likedIds
        if (tokens.isLoggedIn) setLiked(id, !was)
        viewModelScope.launch {
            when (val r = toggleScrap(ScrapTarget.LESSON, id, was)) {
                is ToggleScrapResult.Done -> _events.tryEmit(LessonListEvent.ScrapToast(r.liked))
                is ToggleScrapResult.Failed -> { setLiked(id, was); _events.tryEmit(LessonListEvent.Message(r.message)) }
                ToggleScrapResult.LoginRequired -> _events.tryEmit(LessonListEvent.LoginRequired)
            }
        }
    }

    private fun setLiked(id: Long, liked: Boolean) = _state.update { it.copy(likedIds = if (liked) it.likedIds + id else it.likedIds - id) }

    private fun loadLikes() {
        val epoch = com.muyeon.app.auth.accountEpochNow()
        viewModelScope.launch {
            val ids = getLikedIds(ScrapTarget.LESSON)
            if (epoch == com.muyeon.app.auth.accountEpochNow()) _state.update { it.copy(likedIds = ids) }
        }
    }

    companion object {
        const val ALL = "ALL"
        private const val K_GENRE = "lessonList.genre"
        private const val K_VIEW = "lessonList.view"
    }
}
