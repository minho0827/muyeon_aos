package com.muyeon.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.model.home.HomeQuote
import com.muyeon.domain.model.home.TeacherHomePreview
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.usecase.home.HomeSwipeUseCase
import com.muyeon.domain.usecase.home.TeacherHomeUseCase
import com.muyeon.domain.usecase.my.GetMyPageInfoUseCase
import com.muyeon.domain.usecase.region.ObserveActiveRegionUseCase
import com.muyeon.domain.usecase.scrap.GetLikedIdsUseCase
import com.muyeon.domain.usecase.scrap.ToggleScrapResult
import com.muyeon.domain.usecase.scrap.ToggleScrapUseCase
import com.muyeon.domain.model.home.HomeNotice
import com.muyeon.domain.model.home.HomeRecruit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class TeacherHomeUiState(
    val name: String? = null,
    val preview: TeacherHomePreview? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val quotes: List<HomeQuote> = emptyList(),
    val subNotice: List<HomeRecruit> = emptyList(),
    val notices: List<HomeNotice> = emptyList(),
    /** 찜 — 종류(JOB·SUB·CASTING)별 id. */
    val liked: Map<String, Set<Long>> = emptyMap(),
    val likedQuotes: Set<Long> = emptySet(),
    val isRefreshing: Boolean = false,
)

/** 강사 홈 — 웹 pages/muyeon/TeacherHome.js. "📍 내 지역 신규" · 추천 학원 · 추천 공고 · 급구 · 레슨을 찾는 회원 · 하단. */
class TeacherHomeViewModel(
    private val preview: TeacherHomeUseCase,
    private val home: HomeSwipeUseCase,
    private val info: GetMyPageInfoUseCase,
    observeRegion: ObserveActiveRegionUseCase,
    private val getLikedIds: GetLikedIdsUseCase,
    private val toggleScrap: ToggleScrapUseCase,
    private val tokens: TokenStore,
) : ViewModel() {
    private val regionFlow = observeRegion()
    private val _state = MutableStateFlow(TeacherHomeUiState())
    val state: StateFlow<TeacherHomeUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        load()
        viewModelScope.launch { regionFlow.drop(1).collect { load() } }
    }

    fun load(pull: Boolean = false) {
        _state.update { it.copy(loading = it.preview == null, error = null, isRefreshing = pull) }
        val region: ActiveRegion? = regionFlow.value
        viewModelScope.launch {
            // 이름은 본인확인 실명(웹 user.name).
            launch { val n = info(com.muyeon.domain.model.member.ActiveType.TEACHER).name; _state.update { it.copy(name = n) } }
            launch { _state.update { it.copy(quotes = home.homeQuotes(), subNotice = home.recruitNotice(HomeRecruit.Kind.SUB), notices = home.notices()) } }
            val today = LocalDate.now(ZoneId.of("Asia/Seoul")).toString()
            when (val r = preview(region, today)) {
                is ApiResult.Success -> _state.update { it.copy(preview = r.data, loading = false, isRefreshing = false) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, isRefreshing = false, error = r.message) }
            }
            val liked = mapOf(
                "JOB" to getLikedIds(ScrapTarget.JOB), "SUB" to getLikedIds(ScrapTarget.SUB), "CASTING" to getLikedIds(ScrapTarget.CASTING),
            )
            _state.update { it.copy(liked = liked, likedQuotes = getLikedIds(ScrapTarget.QUOTE)) }
        }
    }

    fun toggleLike(kind: String, id: Long) {
        val target = when (kind) { "JOB" -> ScrapTarget.JOB; "SUB" -> ScrapTarget.SUB; "CASTING" -> ScrapTarget.CASTING; else -> return }
        val was = id in _state.value.liked[kind].orEmpty()
        set(kind, id, !was)
        viewModelScope.launch {
            when (val r = toggleScrap(target, id, was)) {
                is ToggleScrapResult.Done -> _events.tryEmit(HomeEvent.ScrapToast(r.liked))
                is ToggleScrapResult.Failed -> { set(kind, id, was); _events.tryEmit(HomeEvent.Message(r.message)) }
                ToggleScrapResult.LoginRequired -> _events.tryEmit(HomeEvent.LoginRequired)
            }
        }
    }

    fun toggleQuoteLike(id: Long) {
        val was = id in _state.value.likedQuotes
        _state.update { it.copy(likedQuotes = if (was) it.likedQuotes - id else it.likedQuotes + id) }
        viewModelScope.launch {
            val r = toggleScrap(ScrapTarget.QUOTE, id, was)
            if (r is ToggleScrapResult.Done) _events.tryEmit(HomeEvent.ScrapToast(r.liked))
            else _state.update { it.copy(likedQuotes = if (was) it.likedQuotes + id else it.likedQuotes - id) }
        }
    }

    private fun set(kind: String, id: Long, liked: Boolean) = _state.update { s ->
        val cur = s.liked[kind].orEmpty()
        s.copy(liked = s.liked + (kind to if (liked) cur + id else cur - id))
    }
}
