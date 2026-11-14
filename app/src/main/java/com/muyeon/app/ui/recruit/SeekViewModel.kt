package com.muyeon.app.ui.recruit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.ui.state.PagedList
import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.common.fetchCursorPages
import com.muyeon.domain.common.mergeCursorPage
import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.recruit.SeekFilter
import com.muyeon.domain.model.recruit.SeekerCard
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.usecase.recruit.SeekerListUseCase
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
import com.muyeon.app.auth.onAccountChanged

data class SeekUiState(
    val list: PagedList<SeekerCard> = PagedList(),
    val filter: SeekFilter = SeekFilter(),
    /** 강사 유형이면 필터가 [유형·분야], 그 밖은 [분야·대상·경력](웹 JobBoard seek). */
    val teacherView: Boolean = false,
    val likedTeachers: Set<Long> = emptySet(),
    val likedDancers: Set<Long> = emptySet(),
    val isRefreshing: Boolean = false,
)

/** 구직(이력서) 목록 — 웹 JobBoard tab=seek. 커서 30 · 필터 · 찜(강사·무용수 따로). 지역은 보내지 않는다(웹과 같다). */
class SeekViewModel(
    private val seekers: SeekerListUseCase,
    private val getLikedIds: GetLikedIdsUseCase,
    private val toggleScrap: ToggleScrapUseCase,
    private val tokens: TokenStore,
    activeType: ActiveTypeStore,
) : ViewModel() {
    private val _state = MutableStateFlow(SeekUiState(teacherView = ActiveType.visible(activeType.current()) == ActiveType.TEACHER))
    val state: StateFlow<SeekUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<RecruitEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<RecruitEvent> = _events.asSharedFlow()
    private var job: Job? = null

    init { refresh() }

    /** ★ 2026-11-14 화면이 다시 보일 때 — 받은 지 30초가 지났으면 다시 받는다(구인 HireViewModel 과 같은 규칙). */
    fun onResume() {
        val at = loadedAt
        if (at != null && System.currentTimeMillis() - at > 30_000L) refresh()
    }
    private var loadedAt: Long? = null

    fun applyFilter(f: SeekFilter) { _state.update { it.copy(filter = f, list = PagedList()) }; refresh() }

    fun refresh(pull: Boolean = false) {
        job?.cancel()
        if (pull) _state.update { it.copy(isRefreshing = true) }
        val f = _state.value.filter
        job = viewModelScope.launch {
            val r = fetchCursorPages(null, emptySet<String>(), { it.role + it.id }) { c -> seekers.page(f, c) }
            _state.update {
                it.copy(isRefreshing = false, list = it.list.copy(items = if (r.pages > 0) r.items else it.list.items, cursor = it.list.cursor.applied(r),
                    isInitialLoading = false, errorMessage = if (r.pages == 0) r.error else null, moreError = null))
            }
            if (r.pages > 0) loadedAt = System.currentTimeMillis()
            loadLikes()
        }
    }

    fun loadMore() {
        val l = _state.value.list
        if (l.isLoadingMore || l.isInitialLoading || !l.cursor.hasMore) return
        _state.update { it.copy(list = it.list.copy(isLoadingMore = true, moreError = null, cursor = it.list.cursor.retrying())) }
        val f = _state.value.filter
        job = viewModelScope.launch {
            val r = fetchCursorPages(l.cursor.nextCursor, l.items.mapTo(HashSet()) { it.role + it.id }, { it.role + it.id }) { c -> seekers.page(f, c) }
            _state.update { it.copy(list = it.list.copy(items = mergeCursorPage(it.list.items, r.items, false) { x -> x.role + x.id },
                cursor = it.list.cursor.applied(r), isLoadingMore = false, moreError = r.error)) }
        }
    }

    private fun loadLikes() {
        val epoch = com.muyeon.app.auth.accountEpochNow()
        viewModelScope.launch {
            val t = getLikedIds(ScrapTarget.TEACHER)
            val d = getLikedIds(ScrapTarget.DANCER)
            if (epoch == com.muyeon.app.auth.accountEpochNow()) _state.update { it.copy(likedTeachers = t, likedDancers = d) }
        }
    }

    fun toggleLike(c: SeekerCard) {
        val dancer = c.role == ActiveType.DANCER
        val was = c.id in (if (dancer) _state.value.likedDancers else _state.value.likedTeachers)
        if (tokens.isLoggedIn) set(dancer, c.id, !was)
        viewModelScope.launch {
            when (val r = toggleScrap(if (dancer) ScrapTarget.DANCER else ScrapTarget.TEACHER, c.id, was)) {
                is ToggleScrapResult.Done -> _events.tryEmit(RecruitEvent.ScrapToast(r.liked))
                is ToggleScrapResult.Failed -> { set(dancer, c.id, was); _events.tryEmit(RecruitEvent.Message(r.message)) }
                ToggleScrapResult.LoginRequired -> _events.tryEmit(RecruitEvent.LoginRequired)
            }
        }
    }

    private fun set(dancer: Boolean, id: Long, liked: Boolean) = _state.update {
        if (dancer) it.copy(likedDancers = if (liked) it.likedDancers + id else it.likedDancers - id)
        else it.copy(likedTeachers = if (liked) it.likedTeachers + id else it.likedTeachers - id)
    }
}
