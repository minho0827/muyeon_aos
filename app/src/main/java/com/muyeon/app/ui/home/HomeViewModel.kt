package com.muyeon.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.navigation.collectAppResults
import com.muyeon.app.result.ResultKeys
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorState
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.common.fetchCursorPages
import com.muyeon.domain.common.mergeCursorPage
import com.muyeon.domain.model.home.HomeLesson
import com.muyeon.domain.model.home.HomeLessonPreview
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.model.teacher.TeacherCard
import com.muyeon.domain.usecase.home.HomeArtistsUseCase
import com.muyeon.domain.usecase.home.HomeLessonsUseCase
import com.muyeon.domain.usecase.my.GetBadgeCountsUseCase
import com.muyeon.domain.usecase.region.ObserveActiveRegionUseCase
import com.muyeon.domain.usecase.scrap.GetLikedIdsUseCase
import com.muyeon.domain.usecase.scrap.ObserveScrapChangesUseCase
import com.muyeon.domain.usecase.scrap.ToggleScrapResult
import com.muyeon.domain.usecase.scrap.ToggleScrapUseCase
import com.muyeon.app.ui.state.PagedList
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 홈 위 토글 — 강사(에이전시형 그리드) | 레슨(뷰티컬리 배치). */
enum class HomeMode { TEACHERS, LESSONS }

data class HomeUiState(
    val mode: HomeMode = HomeMode.TEACHERS,
    val isLoggedIn: Boolean = false,
    val region: ActiveRegion? = null,
    val unread: Int = 0,
    val isRefreshing: Boolean = false,
    // 강사
    val teacherGenre: String = HomeArtistsUseCase.ALL,
    val artists: PagedList<TeacherCard> = PagedList(),
    // 레슨 — 위 탭(추천 또는 장르)
    val lessonTab: String = LESSON_RECOMMEND,
    val preview: HomeLessonPreview = HomeLessonPreview(emptyList(), emptyList(), emptyList()),
    val newest: List<HomeLesson> = emptyList(),
    val previewLoaded: Boolean = false,
    val genreLessons: PagedList<HomeLesson> = PagedList(),
    val likedLessons: Set<Long> = emptySet(),
    /** 장르 순서(사용자가 정한 것 — 순서 편집). 위 탭·장르 그림 줄이 이 순서. */
    val genres: List<String> = com.muyeon.domain.model.teacher.LessonGenres.ALL,
) {
    companion object { const val LESSON_RECOMMEND = "RECOMMEND" }
}

sealed interface HomeEvent {
    data class ScrapToast(val added: Boolean) : HomeEvent
    data class Message(val text: String) : HomeEvent
    data object LoginRequired : HomeEvent
}

/**
 * ★ 2026-11-08 홈(모든 회원·비회원 같은 화면) — 위 토글 [강사 | 레슨].
 *   강사: 에이전시형 큰 사진 그리드(커서, 장르 탭). 대표 지역은 가까운 순 정렬에만 쓴다(지역 고르기는 MY).
 *   레슨: 추천 탭 = 배너·장르 아이콘·섹션(추천·체험·원데이·새로 열린 레슨) / 장르 탭 = 그 장르 레슨 그리드(커서).
 *   토글·탭 선택은 기억한다(home.mode).
 */
class HomeViewModel(
    private val artists: HomeArtistsUseCase,
    private val lessons: HomeLessonsUseCase,
    observeRegion: ObserveActiveRegionUseCase,
    private val badges: GetBadgeCountsUseCase,
    private val getLikedIds: GetLikedIdsUseCase,
    private val toggleScrap: ToggleScrapUseCase,
    observeScrap: ObserveScrapChangesUseCase,
    private val prefs: KeyValueStore,
    private val tokens: TokenStore,
) : ViewModel() {
    private val regionFlow = observeRegion()
    private val _state = MutableStateFlow(
        HomeUiState(
            mode = if (prefs.getString(K_MODE) == HomeMode.LESSONS.name) HomeMode.LESSONS else HomeMode.TEACHERS,
            isLoggedIn = tokens.isLoggedIn,
            region = regionFlow.value,
            genres = orderedGenres(prefs.getString(K_ORDER)),
        ),
    )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()
    private var artistsJob: Job? = null
    private var lessonsJob: Job? = null

    init {
        refreshArtists()
        loadLessonHome()
        viewModelScope.launch {
            regionFlow.drop(1).collect { r -> _state.update { it.copy(region = r) }; refreshArtists(); loadLessonHome() }
        }
        viewModelScope.launch { observeScrap(ScrapTarget.LESSON).collect { c -> setLiked(c.id, c.liked) } }
        // ★ 2026-11-08 실시간 — 소켓으로 서버 알림이 오면(또는 다시 붙으면) 알림 종 점을 바로 다시 읽는다.
        viewModelScope.launch {
            com.muyeon.app.chat.socket.ChatEventBus.events.collect { e ->
                if (e is com.muyeon.app.chat.socket.ChatEvent.ServerNotification || e is com.muyeon.app.chat.socket.ChatEvent.Reconnected) refreshUnread()
            }
        }
        viewModelScope.collectAppResults(ResultKeys.NOTIFICATIONS) { refreshUnread() }
        viewModelScope.collectAppResults(ResultKeys.LESSONS) { loadLessonHome() }
    }

    fun onSession(loggedIn: Boolean) {
        if (loggedIn != _state.value.isLoggedIn) {
            _state.update { it.copy(isLoggedIn = loggedIn) }
            loadLikes()
        }
        refreshUnread()
    }

    /** 순서 편집 저장 — 기억한다(home.genreOrder). */
    fun saveGenreOrder(order: List<String>) {
        prefs.putString(K_ORDER, order.joinToString(","))
        _state.update { it.copy(genres = orderedGenres(prefs.getString(K_ORDER))) }
    }

    fun resetGenreOrder() {
        prefs.putString(K_ORDER, "")
        _state.update { it.copy(genres = com.muyeon.domain.model.teacher.LessonGenres.ALL) }
    }

    fun setMode(mode: HomeMode) {
        prefs.putString(K_MODE, mode.name)
        _state.update { it.copy(mode = mode) }
    }

    fun pullRefresh() {
        _state.update { it.copy(isRefreshing = true) }
        if (_state.value.mode == HomeMode.TEACHERS) refreshArtists(pull = true)
        else if (_state.value.lessonTab == HomeUiState.LESSON_RECOMMEND) loadLessonHome(pull = true) else refreshGenreLessons(pull = true)
        refreshUnread()
    }

    private fun refreshUnread() {
        if (!tokens.isLoggedIn) { _state.update { it.copy(unread = 0) }; return }
        viewModelScope.launch { _state.update { it.copy(unread = badges.noti()) } }
    }

    // ── 강사 ─────────────────────────────────────────────────────────────
    fun selectTeacherGenre(g: String) {
        if (g == _state.value.teacherGenre) return
        _state.update { it.copy(teacherGenre = g, artists = PagedList()) }
        refreshArtists()
    }

    private fun refreshArtists(pull: Boolean = false) {
        artistsJob?.cancel()
        val s = _state.value
        artistsJob = viewModelScope.launch {
            val f = fetchCursorPages(null, emptySet<Long>(), { it.id }) { c -> artists.page(s.teacherGenre, regionFlow.value, c) }
            _state.update {
                it.copy(
                    isRefreshing = if (pull) false else it.isRefreshing,
                    artists = it.artists.copy(
                        items = if (f.pages > 0) f.items else it.artists.items, cursor = it.artists.cursor.applied(f),
                        isInitialLoading = false, errorMessage = if (f.pages == 0) f.error else null, moreError = null,
                    ),
                )
            }
        }
    }

    fun loadMoreArtists() {
        val a = _state.value.artists
        if (a.isLoadingMore || a.isInitialLoading || !a.cursor.hasMore) return
        _state.update { it.copy(artists = it.artists.copy(isLoadingMore = true, moreError = null, cursor = it.artists.cursor.retrying())) }
        val genre = _state.value.teacherGenre
        artistsJob = viewModelScope.launch {
            val f = fetchCursorPages(a.cursor.nextCursor, a.items.mapTo(HashSet()) { it.id }, { it.id }) { c -> artists.page(genre, regionFlow.value, c) }
            _state.update {
                it.copy(artists = it.artists.copy(items = mergeCursorPage(it.artists.items, f.items, false) { x -> x.id },
                    cursor = it.artists.cursor.applied(f), isLoadingMore = false, moreError = f.error))
            }
        }
    }

    // ── 레슨 ─────────────────────────────────────────────────────────────
    fun selectLessonTab(tab: String) {
        if (tab == _state.value.lessonTab) return
        _state.update { it.copy(lessonTab = tab, genreLessons = PagedList()) }
        if (tab != HomeUiState.LESSON_RECOMMEND) refreshGenreLessons()
    }

    private fun loadLessonHome(pull: Boolean = false) {
        viewModelScope.launch {
            val preview = async { lessons.preview(regionFlow.value) }
            val newest = async { lessons.newest() }
            val p = (preview.await() as? ApiResult.Success)?.data
            _state.update {
                it.copy(preview = p ?: it.preview, newest = newest.await(), previewLoaded = true, isRefreshing = if (pull) false else it.isRefreshing)
            }
            loadLikes()
        }
    }

    private fun refreshGenreLessons(pull: Boolean = false) {
        lessonsJob?.cancel()
        val genre = _state.value.lessonTab
        lessonsJob = viewModelScope.launch {
            val f = fetchCursorPages(null, emptySet<Long>(), { it.id }) { c -> lessons.page(genre, c) }
            _state.update {
                it.copy(
                    isRefreshing = if (pull) false else it.isRefreshing,
                    genreLessons = it.genreLessons.copy(
                        items = if (f.pages > 0) f.items else it.genreLessons.items, cursor = it.genreLessons.cursor.applied(f),
                        isInitialLoading = false, errorMessage = if (f.pages == 0) f.error else null, moreError = null,
                    ),
                )
            }
            loadLikes()
        }
    }

    fun loadMoreGenreLessons() {
        val g = _state.value.genreLessons
        if (g.isLoadingMore || g.isInitialLoading || !g.cursor.hasMore) return
        _state.update { it.copy(genreLessons = it.genreLessons.copy(isLoadingMore = true, moreError = null, cursor = it.genreLessons.cursor.retrying())) }
        val genre = _state.value.lessonTab
        lessonsJob = viewModelScope.launch {
            val f = fetchCursorPages(g.cursor.nextCursor, g.items.mapTo(HashSet()) { it.id }, { it.id }) { c -> lessons.page(genre, c) }
            _state.update {
                it.copy(genreLessons = it.genreLessons.copy(items = mergeCursorPage(it.genreLessons.items, f.items, false) { x -> x.id },
                    cursor = it.genreLessons.cursor.applied(f), isLoadingMore = false, moreError = f.error))
            }
        }
    }

    // ── 찜 ───────────────────────────────────────────────────────────────
    private fun loadLikes() {
        if (!tokens.isLoggedIn) return
        viewModelScope.launch { _state.update { it.copy(likedLessons = getLikedIds(ScrapTarget.LESSON)) } }
    }

    private fun setLiked(id: Long, liked: Boolean) = _state.update {
        it.copy(likedLessons = if (liked) it.likedLessons + id else it.likedLessons - id)
    }

    fun toggleLessonLike(id: Long) {
        val was = id in _state.value.likedLessons
        if (tokens.isLoggedIn) setLiked(id, !was)
        viewModelScope.launch {
            when (val r = toggleScrap(ScrapTarget.LESSON, id, was)) {
                is ToggleScrapResult.Done -> _events.tryEmit(HomeEvent.ScrapToast(r.liked))
                is ToggleScrapResult.Failed -> { setLiked(id, was); _events.tryEmit(HomeEvent.Message(r.message)) }
                ToggleScrapResult.LoginRequired -> _events.tryEmit(HomeEvent.LoginRequired)
            }
        }
    }

    private companion object {
        const val K_MODE = "home.mode"
        const val K_ORDER = "home.genreOrder"

        /** 저장된 순서 + 새로 생긴 장르는 뒤에, 없어진 장르는 뺀다. */
        fun orderedGenres(saved: String?): List<String> {
            val all = com.muyeon.domain.model.teacher.LessonGenres.ALL
            val s = saved.orEmpty().split(",").map { it.trim() }.filter { it in all }.distinct()
            return s + all.filterNot { it in s }
        }
    }
}
