package com.muyeon.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.CursorState
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.common.fetchCursorPages
import com.muyeon.domain.common.mergeCursorPage
import com.muyeon.domain.model.home.HomeFeedItem
import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.model.teacher.LessonGenres
import com.muyeon.domain.usecase.home.HomeFeedUseCase
import com.muyeon.domain.usecase.region.ObserveActiveRegionUseCase
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeClassicUiState(
    val activeType: String = ActiveType.DANCER,
    val domain: String = "ALL",
    val sub: String = "ALL",
    val grid: Boolean = true,
    val items: List<HomeFeedItem> = emptyList(),
    val cursor: CursorState = CursorState.Initial,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val moreError: String? = null,
    val applied: Set<Long> = emptySet(),
    val viewed: Set<Long> = emptySet(),
    val liked: Map<String, Set<Long>> = emptyMap(),
) {
    /** 영역 칩 — 무용수는 공간대관 없음(웹 visibleDomains). */
    val domains: List<String> get() = HOME_DOMAINS.filter { activeType != ActiveType.DANCER || it != "SPACE" }

    /** 고른 영역의 세부 칩(전체 + 장르·종류). 전체면 없음. */
    val subs: List<String> get() = when (domain) {
        "JOB", "SUB" -> listOf("ALL") + LessonGenres.ALL
        "CASTING" -> listOf("ALL") + LessonGenres.ALL + "행사"
        "SPACE" -> listOf("ALL", "발레", "한국무용", "현대무용", "다목적", "매매")
        "CLASS" -> listOf("ALL", "QUESTION", "CHAT", "SHOW_REVIEW", "MEETING", "MARKET")
        else -> emptyList()
    }

    companion object { val HOME_DOMAINS = listOf("ALL", "JOB", "SUB", "CASTING", "SPACE", "CLASS") }
}

/**
 * 무용수·공연팀 홈 — 웹 pages/muyeon/HomeClassic.js. "내 지역 신규" · 영역 칩 · 세부 칩 · 그리드/목록 · 통합 피드(커서) ·
 *   채용 카드 '지원함/열람함' · 찜(채용·캐스팅·공간). 칩·보기 방식은 기억한다(home.domain · home.sub · home.view).
 */
class HomeClassicViewModel(
    private val feed: HomeFeedUseCase,
    observeRegion: ObserveActiveRegionUseCase,
    private val getLikedIds: GetLikedIdsUseCase,
    private val toggleScrap: ToggleScrapUseCase,
    private val prefs: KeyValueStore,
    private val tokens: TokenStore,
    activeTypeStore: ActiveTypeStore,
) : ViewModel() {
    private val regionFlow = observeRegion()
    private val _state = MutableStateFlow(
        HomeClassicUiState(
            activeType = ActiveType.visible(activeTypeStore.current()),
            domain = prefs.getString(K_DOMAIN) ?: "ALL",
            sub = prefs.getString(K_SUB) ?: "ALL",
            grid = prefs.getString(K_VIEW) != "basic",
        ).let { s -> if (s.domain !in s.domains) s.copy(domain = "ALL", sub = "ALL") else s },
    )
    val state: StateFlow<HomeClassicUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()
    private var loadJob: Job? = null

    init {
        refresh()
        loadStatus()
        viewModelScope.launch { regionFlow.drop(1).collect { _state.update { it.copy(items = emptyList(), cursor = CursorState.Initial) }; refresh() } }
    }

    fun onResume() = loadStatus()

    fun selectDomain(d: String) {
        if (d == _state.value.domain) return
        prefs.putString(K_DOMAIN, d); prefs.putString(K_SUB, "ALL")
        _state.update { it.copy(domain = d, sub = "ALL", items = emptyList(), cursor = CursorState.Initial) }
        refresh()
    }

    fun selectSub(s: String) {
        if (s == _state.value.sub) return
        prefs.putString(K_SUB, s)
        _state.update { it.copy(sub = s, items = emptyList(), cursor = CursorState.Initial) }
        refresh()
    }

    fun setGrid(grid: Boolean) { prefs.putString(K_VIEW, if (grid) "half" else "basic"); _state.update { it.copy(grid = grid) } }

    fun refresh(pull: Boolean = false) {
        loadJob?.cancel()
        _state.update { it.copy(isInitialLoading = !pull && it.items.isEmpty(), isRefreshing = pull, errorMessage = null, moreError = null) }
        loadJob = viewModelScope.launch {
            val s = _state.value
            val f = fetchCursorPages(null, emptySet<String>(), { it.id }) { c -> feed.page(regionFlow.value, s.domain, s.sub, c) }
            _state.update {
                it.copy(items = if (f.pages > 0) f.items else it.items, cursor = it.cursor.applied(f), isInitialLoading = false, isRefreshing = false,
                    errorMessage = if (f.pages == 0) f.error else null)
            }
            loadLikes()
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoadingMore || s.isInitialLoading || !s.cursor.hasMore) return
        _state.update { it.copy(isLoadingMore = true, moreError = null, cursor = it.cursor.retrying()) }
        viewModelScope.launch {
            val f = fetchCursorPages(s.cursor.nextCursor, s.items.mapTo(HashSet()) { it.id }, { it.id }) { c -> feed.page(regionFlow.value, s.domain, s.sub, c) }
            _state.update { it.copy(items = mergeCursorPage(it.items, f.items, false) { x -> x.id }, cursor = it.cursor.applied(f), isLoadingMore = false, moreError = f.error) }
        }
    }

    /** 채용 카드의 '지원함/열람함'(로그인 회원만). */
    private fun loadStatus() {
        if (!tokens.isLoggedIn) return
        viewModelScope.launch { _state.update { it.copy(applied = feed.appliedJobIds(), viewed = feed.viewedJobIds()) } }
    }

    private fun loadLikes() {
        viewModelScope.launch {
            _state.update { it.copy(liked = mapOf("JOB" to getLikedIds(ScrapTarget.JOB), "CASTING" to getLikedIds(ScrapTarget.CASTING), "SPACE" to getLikedIds(ScrapTarget.SPACE))) }
        }
    }

    fun toggleLike(item: HomeFeedItem) {
        val id = item.entityId ?: return
        val target = when (item.domain) { "JOB" -> ScrapTarget.JOB; "CASTING" -> ScrapTarget.CASTING; "SPACE" -> ScrapTarget.SPACE; else -> return }
        val was = id in _state.value.liked[item.domain].orEmpty()
        set(item.domain, id, !was)
        viewModelScope.launch {
            when (val r = toggleScrap(target, id, was)) {
                is ToggleScrapResult.Done -> _events.tryEmit(HomeEvent.ScrapToast(r.liked))
                is ToggleScrapResult.Failed -> { set(item.domain, id, was); _events.tryEmit(HomeEvent.Message(r.message)) }
                ToggleScrapResult.LoginRequired -> _events.tryEmit(HomeEvent.LoginRequired)
            }
        }
    }

    private fun set(domain: String, id: Long, liked: Boolean) = _state.update { s ->
        val cur = s.liked[domain].orEmpty()
        s.copy(liked = s.liked + (domain to if (liked) cur + id else cur - id))
    }

    private companion object { const val K_DOMAIN = "home.domain"; const val K_SUB = "home.sub"; const val K_VIEW = "home.view" }
}
