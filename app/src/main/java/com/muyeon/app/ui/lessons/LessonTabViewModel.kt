package com.muyeon.app.ui.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.navigation.collectAppResults
import com.muyeon.app.result.ResultKeys
import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.CursorState
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.common.fetchCursorPages
import com.muyeon.domain.common.mergeCursorPage
import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.model.teacher.LessonGenres
import com.muyeon.domain.model.teacher.TeacherCard
import com.muyeon.domain.repository.UiFlagKeys
import com.muyeon.domain.usecase.region.ObserveActiveRegionUseCase
import com.muyeon.domain.usecase.scrap.GetLikedIdsUseCase
import com.muyeon.domain.usecase.scrap.ObserveScrapChangesUseCase
import com.muyeon.domain.usecase.scrap.ToggleScrapResult
import com.muyeon.domain.usecase.scrap.ToggleScrapUseCase
import com.muyeon.domain.usecase.teacher.GetLessonTeachersPageUseCase
import com.muyeon.domain.usecase.teacher.inSido
import com.muyeon.domain.usecase.uiflag.MarkGuideSeenUseCase
import com.muyeon.domain.usecase.uiflag.ShouldShowGuideUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LessonTabUiState(
    /** [LessonTabViewModel.ALL] 또는 장르. */
    val genre: String = LessonTabViewModel.ALL,
    /** true = 2분할 그리드(웹 half), false = 한 줄(basic). */
    val grid: Boolean = true,
    /** 시·도 필터(받은 목록 안에서 거른다). null = 전체. */
    val sido: String? = null,
    val region: ActiveRegion? = null,
    val items: List<TeacherCard> = emptyList(),
    val cursor: CursorState = CursorState.Initial,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val moreError: String? = null,
    val likedIds: Set<Long> = emptySet(),
    /** 레슨 문의(DM) 버튼 — 일반 유형 회원에게만(웹 강사 탭: activeType === GENERAL, 비회원은 안 보임). */
    val showDm: Boolean = false,
    val showGuide: Boolean = false,
    val showTutorial: Boolean = false,
    val showHelp: Boolean = false,
    val showFilter: Boolean = false,
) {
    val shown: List<TeacherCard> get() = items.inSido(sido)
}

sealed interface LessonTabEvent {
    data class OpenProfile(val userId: Long) : LessonTabEvent
    data class RequestLesson(val category: String, val teacherId: Long) : LessonTabEvent
    data class ScrapToast(val added: Boolean) : LessonTabEvent
    data class Message(val text: String) : LessonTabEvent
    data object LoginRequired : LessonTabEvent
    data object ScrollToTop : LessonTabEvent
}

/**
 * 강사 탭 — 웹 pages/muyeon/LessonTab.js. 레슨을 여는 강사(대표 지역 기준) · 장르 칩 · 그리드/목록 · 시·도 필터 · 찜 ·
 *   레슨 문의 · 안내 배너 · 첫 방문 튜토리얼(4단계) · 이용 안내(확인 → 튜토리얼 다시 보기).
 */
class LessonTabViewModel(
    private val getPage: GetLessonTeachersPageUseCase,
    observeRegion: ObserveActiveRegionUseCase,
    private val getLikedIds: GetLikedIdsUseCase,
    private val toggleScrap: ToggleScrapUseCase,
    observeScrap: ObserveScrapChangesUseCase,
    private val shouldShowGuide: ShouldShowGuideUseCase,
    private val markGuideSeen: MarkGuideSeenUseCase,
    private val prefs: KeyValueStore,
    private val tokens: TokenStore,
    private val activeType: ActiveTypeStore,
) : ViewModel() {
    val genres: List<String> = listOf(ALL) + LessonGenres.ALL
    val sidoOptions: List<String> = LessonGenres.SIDO

    private val regionFlow = observeRegion()

    private val _state = MutableStateFlow(
        LessonTabUiState(
            genre = prefs.getString(KEY_GENRE)?.takeIf { it in genres } ?: ALL,
            grid = prefs.getString(KEY_VIEW) != VIEW_BASIC,
            sido = prefs.getString(KEY_SIDO)?.takeIf { it in LessonGenres.SIDO },
            region = regionFlow.value,
            showDm = dmVisible(),
            showGuide = shouldShowGuide.cached(UiFlagKeys.LESSON_TAB_GUIDE),
        ),
    )
    val state: StateFlow<LessonTabUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LessonTabEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<LessonTabEvent> = _events.asSharedFlow()

    private var loadJob: Job? = null
    private var wasLoggedIn = tokens.isLoggedIn
    /** 이용 안내 [확인] 으로 다시 보는 중 — 끝나도 본 기록·배너를 건드리지 않는다(웹 replaying). */
    private var replaying = false
    private var tutorialChecked = false

    init {
        refresh()
        loadLikes()
        viewModelScope.launch { observeScrap(ScrapTarget.TEACHER).collect { c -> setLiked(c.id, c.liked) } }
        // 대표 지역이 바뀌면(웹 지역 고르기·처음 위치 찾기) 다시 받는다.
        viewModelScope.launch { regionFlow.drop(1).collect { r -> _state.update { it.copy(region = r, items = emptyList(), cursor = CursorState.Initial) }; refresh() } }
        // 견적을 보내고 오면 다시(웹 useResultRefresh QUOTES).
        viewModelScope.collectAppResults(ResultKeys.QUOTES) { refresh(silent = true) }
    }

    private fun dmVisible(): Boolean = tokens.isLoggedIn && ActiveType.visible(activeType.current()) == ActiveType.GENERAL

    fun onResume() {
        _state.update { it.copy(showDm = dmVisible()) }
        if (tokens.isLoggedIn != wasLoggedIn) { wasLoggedIn = tokens.isLoggedIn; loadLikes() }
    }

    fun selectGenre(g: String) {
        if (g == _state.value.genre) return
        prefs.putString(KEY_GENRE, g)
        _state.update { it.copy(genre = g, items = emptyList(), cursor = CursorState.Initial) }
        refresh()
    }

    fun setGrid(grid: Boolean) {
        prefs.putString(KEY_VIEW, if (grid) VIEW_HALF else VIEW_BASIC)
        _state.update { it.copy(grid = grid) }
    }

    fun openFilter(open: Boolean) = _state.update { it.copy(showFilter = open) }
    fun setSido(sido: String?) {
        prefs.putString(KEY_SIDO, sido)
        _state.update { it.copy(sido = sido) }
    }

    fun onTabReselect() {
        val i = genres.indexOf(_state.value.genre)
        selectGenre(genres[(i + 1) % genres.size])
        _events.tryEmit(LessonTabEvent.ScrollToTop)
    }

    fun refresh(pull: Boolean = false, silent: Boolean = false) {
        loadJob?.cancel()
        _state.update { it.copy(isInitialLoading = !pull && !silent && it.items.isEmpty(), isRefreshing = pull, errorMessage = null, moreError = null) }
        loadJob = viewModelScope.launch {
            val s = _state.value
            val fetch = fetchCursorPages(null, emptySet<Long>(), { it.id }) { c -> getPage(s.genre.takeIf { it != ALL }, s.region, c) }
            _state.update {
                it.copy(
                    items = if (fetch.pages > 0) fetch.items else it.items,
                    cursor = it.cursor.applied(fetch),
                    isInitialLoading = false,
                    isRefreshing = false,
                    errorMessage = if (fetch.pages == 0) fetch.error else null,
                )
            }
            if (pull) loadLikes()
            maybeStartTutorial()
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoadingMore || s.isInitialLoading || !s.cursor.hasMore) return
        _state.update { it.copy(isLoadingMore = true, moreError = null, cursor = it.cursor.retrying()) }
        viewModelScope.launch {
            val fetch = fetchCursorPages(s.cursor.nextCursor, s.items.mapTo(HashSet()) { it.id }, { it.id }) { c -> getPage(s.genre.takeIf { it != ALL }, s.region, c) }
            _state.update {
                it.copy(items = mergeCursorPage(it.items, fetch.items, replace = false) { t -> t.id }, cursor = it.cursor.applied(fetch), isLoadingMore = false, moreError = fetch.error)
            }
        }
    }

    // ── 카드 ──
    fun openProfile(t: TeacherCard) = _events.tryEmit(LessonTabEvent.OpenProfile(t.id))

    /** 레슨 문의 — 첫 장르로 문의 카테고리(웹 requestLesson). 예시 프로필은 막는다. */
    fun requestLesson(t: TeacherCard) {
        if (t.isDemoProfile) return
        val g = t.genres.firstOrNull()
        _events.tryEmit(LessonTabEvent.RequestLesson(LessonGenres.QUOTE_CATEGORY[g].orEmpty(), t.id))
    }

    fun toggleLike(id: Long) {
        val was = id in _state.value.likedIds
        if (tokens.isLoggedIn) setLiked(id, !was)
        viewModelScope.launch {
            when (val r = toggleScrap(ScrapTarget.TEACHER, id, was)) {
                is ToggleScrapResult.Done -> _events.tryEmit(LessonTabEvent.ScrapToast(r.liked))
                is ToggleScrapResult.Failed -> { setLiked(id, was); _events.tryEmit(LessonTabEvent.Message(r.message)) }
                ToggleScrapResult.LoginRequired -> _events.tryEmit(LessonTabEvent.LoginRequired)
            }
        }
    }

    private fun setLiked(id: Long, liked: Boolean) = _state.update { it.copy(likedIds = if (liked) it.likedIds + id else it.likedIds - id) }

    private fun loadLikes() {
        viewModelScope.launch { _state.update { it.copy(likedIds = getLikedIds(ScrapTarget.TEACHER)) } }
    }

    // ── 안내 ──
    /** 목록이 처음 그려진 뒤 한 번 — 서버 플래그를 합친 뒤 판정(웹: 목록 렌더 후 400ms). */
    private suspend fun maybeStartTutorial() {
        if (tutorialChecked || _state.value.items.isEmpty()) return
        tutorialChecked = true
        val guide = shouldShowGuide(UiFlagKeys.LESSON_TAB_GUIDE)
        val tutorial = !_state.value.showTutorial && shouldShowGuide.cached(UiFlagKeys.LESSON_TAB)
        _state.update { it.copy(showGuide = guide) }
        if (tutorial) {
            delay(TUTORIAL_DELAY_MS)
            _state.update { it.copy(showTutorial = true) }
        }
    }

    fun closeGuide() {
        _state.update { it.copy(showGuide = false) }
        viewModelScope.launch { markGuideSeen(UiFlagKeys.LESSON_TAB_GUIDE) }
    }

    fun closeTutorial() {
        _state.update { it.copy(showTutorial = false) }
        if (replaying) { replaying = false; return }
        viewModelScope.launch { markGuideSeen(UiFlagKeys.LESSON_TAB) }
        // 튜토리얼이 같은 내용을 안내하므로 위 배너도 함께 닫는다(웹과 같다).
        closeGuide()
    }

    fun openHelp() = _state.update { it.copy(showHelp = true) }

    /** 이용 안내 [확인] — 닫고 튜토리얼을 한 번 다시 보여 준다(본 기록은 그대로). */
    fun closeHelpAndReplay() {
        _state.update { it.copy(showHelp = false) }
        replaying = true
        viewModelScope.launch {
            delay(HELP_REPLAY_DELAY_MS)
            _state.update { it.copy(showTutorial = true) }
        }
    }

    companion object {
        const val ALL = "ALL"
        private const val KEY_GENRE = "lesson.genre"
        private const val KEY_VIEW = "lesson.view"
        private const val KEY_SIDO = "lesson.regionFilter"
        private const val VIEW_HALF = "half"
        private const val VIEW_BASIC = "basic"
        private const val TUTORIAL_DELAY_MS = 400L
        private const val HELP_REPLAY_DELAY_MS = 250L
    }
}
