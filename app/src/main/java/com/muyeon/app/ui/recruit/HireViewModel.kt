package com.muyeon.app.ui.recruit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.ui.state.PagedList
import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.common.fetchCursorPages
import com.muyeon.domain.common.mergeCursorPage
import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.recruit.JobCard
import com.muyeon.domain.model.recruit.JobFilter
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.usecase.recruit.JobListUseCase
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
import com.muyeon.app.auth.onAccountChanged

data class HireUiState(
    val list: PagedList<JobCard> = PagedList(),
    val filter: JobFilter = JobFilter(),
    val grid: Boolean = true,
    val forMe: List<JobCard> = emptyList(),
    val applied: Set<Long> = emptySet(),
    val viewed: Set<Long> = emptySet(),
    val liked: Set<Long> = emptySet(),
    val isRefreshing: Boolean = false,
)

sealed interface RecruitEvent {
    data class ScrapToast(val added: Boolean) : RecruitEvent
    data class Message(val text: String) : RecruitEvent
    data object LoginRequired : RecruitEvent
}

/**
 * 구인(채용공고) 목록 — 웹 JobBoard tab=hire. 커서 30 · 경력·급여 필터(여러 개) · 그리드/목록(기억) ·
 *   '나에게 맞는 공고'(강사·무용수) · 지원함/열람함 · 찜. 지역은 정렬에만(MY 대표 지역).
 */
class HireViewModel(
    private val jobs: JobListUseCase,
    observeRegion: ObserveActiveRegionUseCase,
    private val getLikedIds: GetLikedIdsUseCase,
    private val toggleScrap: ToggleScrapUseCase,
    private val prefs: KeyValueStore,
    private val tokens: TokenStore,
    private val activeType: ActiveTypeStore,
) : ViewModel() {
    private val region = observeRegion()
    private val _state = MutableStateFlow(HireUiState(grid = prefs.getString(K_VIEW) != "basic"))
    val state: StateFlow<HireUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<RecruitEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<RecruitEvent> = _events.asSharedFlow()
    private var job: Job? = null

    init {
        refresh()
        viewModelScope.launch { region.drop(1).collect { refresh() } }
        // ★ 2026-11-09 계정 바뀜 — 지원함·열람함·찜·나에게 맞는 공고를 비우고 다시.
        onAccountChanged { _state.update { s -> s.copy(forMe = emptyList(), applied = emptySet(), viewed = emptySet(), liked = emptySet()) }; loadSide() }
    }

    /**
     * 화면이 다시 보일 때(iOS onResume 1:1).
     *  ★ 2026-11-14 목록을 받은 지 30초가 지났으면 목록도 다시 받는다 — 종전엔 처음 한 번만 받아,
     *   다른 사람이 새로 올린 공고가 당겨서 새로고침해야 보였다. 받는 동안 보던 목록은 그대로 둔다(깜빡임 없음).
     */
    fun onResume() {
        val at = loadedAt
        if (at != null && System.currentTimeMillis() - at > STALE_AFTER_MS) refresh() else loadSide()
    }
    private var loadedAt: Long? = null

    fun setGrid(grid: Boolean) { prefs.putString(K_VIEW, if (grid) "half" else "basic"); _state.update { it.copy(grid = grid) } }

    fun applyFilter(f: JobFilter) { _state.update { it.copy(filter = f, list = PagedList()) }; refresh() }

    fun refresh(pull: Boolean = false) {
        job?.cancel()
        if (pull) _state.update { it.copy(isRefreshing = true) }
        val f = _state.value.filter
        job = viewModelScope.launch {
            val r = fetchCursorPages(null, emptySet<Long>(), { it.id }) { c -> jobs.page(region.value, f, c) }
            _state.update {
                it.copy(isRefreshing = false, list = it.list.copy(items = if (r.pages > 0) r.items else it.list.items, cursor = it.list.cursor.applied(r),
                    isInitialLoading = false, errorMessage = if (r.pages == 0) r.error else null, moreError = null))
            }
            if (r.pages > 0) loadedAt = System.currentTimeMillis()
        }
        loadSide()
    }

    fun loadMore() {
        val l = _state.value.list
        if (l.isLoadingMore || l.isInitialLoading || !l.cursor.hasMore) return
        _state.update { it.copy(list = it.list.copy(isLoadingMore = true, moreError = null, cursor = it.list.cursor.retrying())) }
        val f = _state.value.filter
        job = viewModelScope.launch {
            val r = fetchCursorPages(l.cursor.nextCursor, l.items.mapTo(HashSet()) { it.id }, { it.id }) { c -> jobs.page(region.value, f, c) }
            _state.update { it.copy(list = it.list.copy(items = mergeCursorPage(it.list.items, r.items, false) { x -> x.id },
                cursor = it.list.cursor.applied(r), isLoadingMore = false, moreError = r.error)) }
        }
    }

    /** 나에게 맞는 공고(강사·무용수) · 지원함/열람함 · 찜 — 로그인 회원만. */
    private fun loadSide() {
        if (!tokens.isLoggedIn) { _state.update { it.copy(forMe = emptyList(), applied = emptySet(), viewed = emptySet(), liked = emptySet()) }; return }
        val type = ActiveType.visible(activeType.current())
        viewModelScope.launch {
            // ★ 2026-11-14 '나에게 맞는 공고' 줄을 없애 더는 부르지 않는다.
            val forMe = emptyList<com.muyeon.domain.model.recruit.JobCard>()
            _state.update { it.copy(forMe = forMe, applied = jobs.appliedIds(), viewed = jobs.viewedIds(), liked = getLikedIds(ScrapTarget.JOB)) }
        }
    }

    fun toggleLike(id: Long) {
        val was = id in _state.value.liked
        if (tokens.isLoggedIn) set(id, !was)
        viewModelScope.launch {
            when (val r = toggleScrap(ScrapTarget.JOB, id, was)) {
                is ToggleScrapResult.Done -> _events.tryEmit(RecruitEvent.ScrapToast(r.liked))
                is ToggleScrapResult.Failed -> { set(id, was); _events.tryEmit(RecruitEvent.Message(r.message)) }
                ToggleScrapResult.LoginRequired -> _events.tryEmit(RecruitEvent.LoginRequired)
            }
        }
    }

    private fun set(id: Long, liked: Boolean) = _state.update { it.copy(liked = if (liked) it.liked + id else it.liked - id) }

    private companion object {
        const val K_VIEW = "recruit.hire.view"
        const val STALE_AFTER_MS = 30_000L
    }
}
