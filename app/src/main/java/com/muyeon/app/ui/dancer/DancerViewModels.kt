package com.muyeon.app.ui.dancer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorState
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.common.fetchCursorPages
import com.muyeon.domain.common.mergeCursorPage
import com.muyeon.domain.model.dancer.CastingCallOption
import com.muyeon.domain.model.dancer.DancerCard
import com.muyeon.domain.model.dancer.DancerFilter
import com.muyeon.domain.model.dancer.DancerProfile
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.usecase.dancer.DancersUseCase
import com.muyeon.domain.usecase.safety.BlockUserUseCase
import com.muyeon.domain.usecase.safety.ReportUseCase
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DancerListUiState(
    val filter: DancerFilter = DancerFilter(),
    val items: List<DancerCard> = emptyList(),
    val cursor: CursorState = CursorState.Initial,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val moreError: String? = null,
    val likedIds: Set<Long> = emptySet(),
    /** 상세를 볼 수 없는 유형이 눌렀다 — 안내 팝업. */
    val readNotice: Boolean = false,
)

sealed interface DancerEvent {
    data class Message(val text: String) : DancerEvent
    data class MessageRes(val res: Int) : DancerEvent
    data object LoginRequired : DancerEvent
    data class OpenDetail(val id: Long) : DancerEvent
    data class OpenChat(val roomId: Long, val title: String) : DancerEvent
    data object ChatLimit : DancerEvent
    data object Blocked : DancerEvent
}

/**
 * 무용수 프로필 열람 — 웹 pages/muyeon/dancers/DancerList.js. 전공 칩 + 성별·영상·수상 칩, 커서 목록, 찜.
 *   상세는 공연팀·학원·강사만(웹 dancer.read) — 그 밖이면 안내. 전공·성별·영상 칩은 기억한다(웹 usePersistedState).
 */
class DancerListViewModel(
    private val dancers: DancersUseCase,
    private val getLikedIds: GetLikedIdsUseCase,
    private val toggleScrap: ToggleScrapUseCase,
    observeScrap: ObserveScrapChangesUseCase,
    private val activeType: ActiveTypeStore,
    private val tokens: TokenStore,
    private val prefs: KeyValueStore,
) : ViewModel() {
    private val _state = MutableStateFlow(
        DancerListUiState(
            filter = DancerFilter(
                major = prefs.getString(K_MAJOR)?.takeIf { it != ALL },
                gender = prefs.getString(K_GENDER)?.takeIf { it != ALL },
                hasVideo = prefs.getString(K_VIDEO) == "1",
            ),
        ),
    )
    val state: StateFlow<DancerListUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<DancerEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DancerEvent> = _events.asSharedFlow()
    private var job: Job? = null

    init {
        refresh()
        loadLikes()
        viewModelScope.launch { observeScrap(ScrapTarget.DANCER).collect { c -> setLiked(c.id, c.liked) } }
    }

    fun setFilter(f: DancerFilter) {
        if (f == _state.value.filter) return
        prefs.putString(K_MAJOR, f.major ?: ALL)
        prefs.putString(K_GENDER, f.gender ?: ALL)
        prefs.putString(K_VIDEO, if (f.hasVideo) "1" else "0")
        _state.update { it.copy(filter = f, items = emptyList(), cursor = CursorState.Initial) }
        refresh()
    }

    fun refresh(pull: Boolean = false) {
        job?.cancel()
        _state.update { it.copy(isInitialLoading = !pull && it.items.isEmpty(), isRefreshing = pull, errorMessage = null, moreError = null) }
        val f = _state.value.filter
        job = viewModelScope.launch {
            val r = fetchCursorPages(null, emptySet<Long>(), { it.id }) { c -> dancers.page(f, c) }
            _state.update {
                it.copy(items = mergeCursorPage(it.items, r.items, replace = r.pages > 0) { d -> d.id }, cursor = it.cursor.applied(r),
                    isInitialLoading = false, isRefreshing = false, errorMessage = if (r.pages == 0) r.error else null)
            }
            if (pull) loadLikes()
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoadingMore || s.isInitialLoading || !s.cursor.hasMore) return
        _state.update { it.copy(isLoadingMore = true, moreError = null, cursor = it.cursor.retrying()) }
        job = viewModelScope.launch {
            val r = fetchCursorPages(s.cursor.nextCursor, s.items.mapTo(HashSet()) { it.id }, { it.id }) { c -> dancers.page(s.filter, c) }
            _state.update {
                it.copy(items = mergeCursorPage(it.items, r.items, replace = false) { d -> d.id }, cursor = it.cursor.applied(r),
                    isLoadingMore = false, moreError = r.error)
            }
        }
    }

    /** 줄 누르기 — 볼 수 있는 유형이면 상세, 비회원이면 로그인, 그 밖이면 안내(웹 gate("dancer.read")). */
    fun open(id: Long) {
        when {
            !tokens.isLoggedIn -> _events.tryEmit(DancerEvent.LoginRequired)
            activeType.current() in DancersUseCase.READ_TYPES -> _events.tryEmit(DancerEvent.OpenDetail(id))
            else -> _state.update { it.copy(readNotice = true) }
        }
    }

    fun dismissNotice() = _state.update { it.copy(readNotice = false) }

    fun toggleLike(id: Long) {
        val was = id in _state.value.likedIds
        if (tokens.isLoggedIn) setLiked(id, !was)
        viewModelScope.launch {
            when (val r = toggleScrap(ScrapTarget.DANCER, id, was)) {
                is ToggleScrapResult.Done -> Unit
                is ToggleScrapResult.Failed -> { setLiked(id, was); _events.tryEmit(DancerEvent.Message(r.message)) }
                ToggleScrapResult.LoginRequired -> _events.tryEmit(DancerEvent.LoginRequired)
            }
        }
    }

    private fun setLiked(id: Long, liked: Boolean) = _state.update { it.copy(likedIds = if (liked) it.likedIds + id else it.likedIds - id) }
    private fun loadLikes() { viewModelScope.launch { _state.update { it.copy(likedIds = getLikedIds(ScrapTarget.DANCER)) } } }

    companion object {
        const val ALL = "ALL"
        private const val K_MAJOR = "dancers.major"
        private const val K_GENDER = "dancers.gender"
        private const val K_VIDEO = "dancers.hasVideo"
    }
}

data class DancerDetailUiState(
    val profile: DancerProfile? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val liked: Boolean = false,
    /** 공연팀이면 [캐스팅 제안 보내기], 아니면 [채팅하기]. */
    val isTeam: Boolean = false,
    val castingCalls: List<CastingCallOption> = emptyList(),
    val pickCall: Boolean = false,
    val confirmChat: Boolean = false,
    val report: Boolean = false,
    val confirmBlock: Boolean = false,
    val busy: Boolean = false,
    /** 제안 보냄 · 찜 저장 안내. */
    val doneMessage: Int? = null,
)

/**
 * 무용수 상세 — 웹 DancerDetail.js. 공연팀은 캐스팅 제안(내 캐스팅 공고 고르기/공고 없이), 학원·강사는 채팅 시작(확인 팝업).
 *   신고(허위 프로필) · 차단(본인이 아니면). 서버가 권한을 다시 본다(403 은 문구 그대로).
 */
class DancerDetailViewModel(
    private val id: Long,
    private val dancers: DancersUseCase,
    private val getLikedIds: GetLikedIdsUseCase,
    private val toggleScrap: ToggleScrapUseCase,
    private val report: ReportUseCase,
    private val block: BlockUserUseCase,
    activeType: ActiveTypeStore,
) : ViewModel() {
    private val _state = MutableStateFlow(DancerDetailUiState(isTeam = activeType.current() == "TEAM"))
    val state: StateFlow<DancerDetailUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<DancerEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DancerEvent> = _events.asSharedFlow()

    init {
        load()
        viewModelScope.launch { _state.update { it.copy(liked = id in getLikedIds(ScrapTarget.DANCER)) } }
        if (_state.value.isTeam) viewModelScope.launch {
            (dancers.myOpenCastingCalls() as? ApiResult.Success)?.let { r -> _state.update { it.copy(castingCalls = r.data) } }
        }
    }

    fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = dancers.detail(id)) {
                is ApiResult.Success -> _state.update { it.copy(profile = r.data, isLoading = false) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, errorMessage = r.message) }
            }
        }
    }

    fun toggleLike() {
        val was = _state.value.liked
        _state.update { it.copy(liked = !was) }
        viewModelScope.launch {
            when (val r = toggleScrap(ScrapTarget.DANCER, id, was)) {
                is ToggleScrapResult.Done -> if (r.liked) _state.update { it.copy(doneMessage = com.muyeon.app.R.string.dancer_scrap_done) }
                is ToggleScrapResult.Failed -> { _state.update { it.copy(liked = was) }; _events.tryEmit(DancerEvent.Message(r.message)) }
                ToggleScrapResult.LoginRequired -> { _state.update { it.copy(liked = was) }; _events.tryEmit(DancerEvent.LoginRequired) }
            }
        }
    }

    /** [캐스팅 제안 보내기] — 내 캐스팅 공고가 있으면 고르게, 없으면 공고 없이 바로(웹 startOffer). */
    fun startOffer() { if (_state.value.castingCalls.isNotEmpty()) _state.update { it.copy(pickCall = true) } else sendOffer(null) }
    fun closePick() = _state.update { it.copy(pickCall = false) }

    fun sendOffer(callId: Long?) {
        if (_state.value.busy) return
        _state.update { it.copy(pickCall = false, busy = true) }
        viewModelScope.launch {
            when (val r = dancers.offer(id, callId)) {
                is ApiResult.Success -> _state.update { it.copy(doneMessage = com.muyeon.app.R.string.dancer_offer_done) }
                is ApiResult.Error -> _events.tryEmit(DancerEvent.Message(r.message))
            }
            _state.update { it.copy(busy = false) }
        }
    }

    fun askChat(ask: Boolean) = _state.update { it.copy(confirmChat = ask) }

    fun startChat() {
        if (_state.value.busy) return
        _state.update { it.copy(confirmChat = false, busy = true) }
        viewModelScope.launch {
            when (val r = dancers.directRoom(id)) {
                is ApiResult.Success -> if (r.data > 0) _events.tryEmit(DancerEvent.OpenChat(r.data, _state.value.profile?.name.orEmpty()))
                    else _events.tryEmit(DancerEvent.MessageRes(com.muyeon.app.R.string.dancer_chat_failed))
                is ApiResult.Error -> if (r.code == "RECRUIT_CHAT_LIMIT") _events.tryEmit(DancerEvent.ChatLimit) else _events.tryEmit(DancerEvent.Message(r.message))
            }
            _state.update { it.copy(busy = false) }
        }
    }

    fun showReport(show: Boolean) = _state.update { it.copy(report = show) }

    fun sendReport(reason: String, detail: String) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val r = report("DANCER", id, reason, detail)) {
                is ApiResult.Success -> { _state.update { it.copy(report = false) }; _events.tryEmit(DancerEvent.MessageRes(com.muyeon.app.R.string.dancer_reported)) }
                is ApiResult.Error -> _events.tryEmit(DancerEvent.Message(r.message))
            }
            _state.update { it.copy(busy = false) }
        }
    }

    fun askBlock(ask: Boolean) = _state.update { it.copy(confirmBlock = ask) }

    fun blockUser() {
        _state.update { it.copy(confirmBlock = false, busy = true) }
        viewModelScope.launch {
            when (val r = block(id)) {
                is ApiResult.Success -> _events.tryEmit(DancerEvent.Blocked)
                is ApiResult.Error -> _events.tryEmit(DancerEvent.Message(r.message))
            }
            _state.update { it.copy(busy = false) }
        }
    }

    fun dismissDone() = _state.update { it.copy(doneMessage = null) }
}
