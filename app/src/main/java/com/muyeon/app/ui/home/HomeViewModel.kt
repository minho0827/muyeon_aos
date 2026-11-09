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
import com.muyeon.domain.model.teacher.LessonGenres
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
    // 레슨 — 위 탭(추천 또는 장르). ★ 2026-11-09 장르 탭도 추천과 같은 칸들(그 장르로 거른다) — 아래 값은 지금 탭 것.
    val lessonTab: String = LESSON_RECOMMEND,
    val preview: HomeLessonPreview = HomeLessonPreview(emptyList(), emptyList(), emptyList()),
    val newest: List<HomeLesson> = emptyList(),
    val previewLoaded: Boolean = false,
    /** 추천 묶음을 못 읽었다(칸이 다 비었을 때만 '다시 시도'). */
    val previewFailed: Boolean = false,
    val likedLessons: Set<Long> = emptySet(),
    /**
     * ★ 2026-11-08 '레슨을 찾는 회원들' — 비회원·실패면 빈 목록(섹션 숨김, 웹과 같다).
     *   추천 탭 = GET /quotes/home 10건 · 장르 탭 = 그 장르만(QuoteFeedUseCase.page — 강사·학원은 서버 categoryId, 그 밖은 10건을 거름).
     */
    val quoteFeed: List<com.muyeon.domain.model.quotefeed.QuoteFeedItem> = emptyList(),
    /** 강사·학원 활동 중 — 카드 '제안 보냄' 표시. */
    val quoteFeedPro: Boolean = false,
    /** 장르 순서(사용자가 정한 것 — 순서 편집). 위 탭이 이 순서. */
    val genres: List<String> = com.muyeon.domain.model.teacher.LessonGenres.ALL,
) {
    /** 지금 레슨 탭의 장르(추천이면 null). */
    val lessonGenre: String? get() = lessonTab.takeIf { it != LESSON_RECOMMEND }

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
 *   레슨: 추천 탭 = 섹션(추천·레슨을 찾는 회원들·체험·원데이·새로 열린 레슨) / 장르 탭 = 같은 섹션을 그 장르로 거른 것.
 *         (★ 2026-11-09 배너·장르 그림 줄은 뺐다 — 장르는 위 탭 하나로.)
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
    private val quoteFeed: com.muyeon.domain.usecase.quotefeed.QuoteFeedUseCase,
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
    private var quoteJob: Job? = null

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
        viewModelScope.launch { HomeModeRequests.flow.collect { m -> HomeModeRequests.consumed(); setMode(m) } }
        viewModelScope.collectAppResults(ResultKeys.NOTIFICATIONS) { refreshUnread() }
        viewModelScope.collectAppResults(ResultKeys.LESSONS) { loadLessonHome() }
    }

    fun onSession(loggedIn: Boolean) {
        if (loggedIn != _state.value.isLoggedIn) {
            _state.update { it.copy(isLoggedIn = loggedIn) }
            loadLikes()
            loadQuoteFeed()
        } else if (quoteFeed.canBrowseAll != _state.value.quoteFeedPro) loadQuoteFeed() // 유형 전환하고 돌아옴
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
        else loadLessonHome(pull = true)
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
        // 탭을 바꾸면 비우고 그 탭 것으로 다시 읽는다(다른 장르 카드가 잠깐 보이지 않게).
        _state.update {
            it.copy(lessonTab = tab, preview = HomeLessonPreview(emptyList(), emptyList(), emptyList()), newest = emptyList(),
                previewLoaded = false, previewFailed = false, quoteFeed = emptyList(), isRefreshing = false)
        }
        loadLessonHome()
    }

    /** 지금 탭(추천 = 전체 · 장르 탭 = 그 장르)의 칸들 — 추천·체험·원데이(home-preview) · 새로 열린 레슨 · 레슨을 찾는 회원들. */
    private fun loadLessonHome(pull: Boolean = false) {
        lessonsJob?.cancel()
        val tab = _state.value.lessonTab
        val genre = _state.value.lessonGenre
        lessonsJob = viewModelScope.launch {
            val preview = async { lessons.preview(genre, regionFlow.value) }
            val newest = async { lessons.newest(genre) }
            val p = (preview.await() as? ApiResult.Success)?.data
            val n = newest.await()
            _state.update {
                if (it.lessonTab != tab) return@update it.copy(isRefreshing = if (pull) false else it.isRefreshing)
                it.copy(preview = p ?: it.preview, newest = n, previewLoaded = true, previewFailed = p == null,
                    isRefreshing = if (pull) false else it.isRefreshing)
            }
            loadLikes()
        }
        loadQuoteFeed()
    }

    /** '레슨을 찾는 회원들' — 실패해도 조용히(섹션만 숨김). 장르 탭은 그 장르(견적 categoryId)만. */
    private fun loadQuoteFeed() {
        quoteJob?.cancel()
        val tab = _state.value.lessonTab
        val category = _state.value.lessonGenre?.let { LessonGenres.QUOTE_CATEGORY[it] }
        quoteJob = viewModelScope.launch {
            val list = when {
                tab == HomeUiState.LESSON_RECOMMEND -> quoteFeed.home().getOrNull().orEmpty()
                category == null -> emptyList()
                else -> quoteFeed.page(category, 0).getOrNull()?.items.orEmpty().take(QUOTE_SECTION)
            }
            _state.update { if (it.lessonTab != tab) it else it.copy(quoteFeed = list, quoteFeedPro = quoteFeed.canBrowseAll) }
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
        /** 홈 '레슨을 찾는 회원들' 칸 — 웹 /quotes/home 과 같은 10건. */
        const val QUOTE_SECTION = 10

        /** 저장된 순서 + 새로 생긴 장르는 뒤에, 없어진 장르는 뺀다. */
        fun orderedGenres(saved: String?): List<String> {
            val all = com.muyeon.domain.model.teacher.LessonGenres.ALL
            val s = saved.orEmpty().split(",").map { it.trim() }.filter { it in all }.distinct()
            return s + all.filterNot { it in s }
        }
    }
}
