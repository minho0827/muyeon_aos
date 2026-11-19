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
import com.muyeon.app.auth.onAccountChanged

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
)

sealed interface DancerEvent {
    data class Message(val text: String) : DancerEvent
    data class MessageRes(val res: Int) : DancerEvent
    data object LoginRequired : DancerEvent
    data class OpenDetail(val id: Long) : DancerEvent
    data class OpenChat(val roomId: Long, val title: String) : DancerEvent
    data object ChatLimit : DancerEvent
    data object Blocked : DancerEvent
    /** ★ 2026-11-19 탈퇴한 무용수를 찜에서 뺐다 — 뒤로. */
    data object Unscrapped : DancerEvent
}

/**
 * 무용수 프로필 열람 — 웹 pages/muyeon/dancers/DancerList.js. 전공 칩 + 성별·영상·수상 칩, 커서 목록, 찜.
 *   상세는 누구나 연다(공연팀·학원·강사는 전체, 그 밖은 서버가 소개만). 전공·성별·영상 칩은 기억한다(웹 usePersistedState).
 */
class DancerListViewModel(
    private val dancers: DancersUseCase,
    private val getLikedIds: GetLikedIdsUseCase,
    private val toggleScrap: ToggleScrapUseCase,
    observeScrap: ObserveScrapChangesUseCase,
    @Suppress("unused") activeType: ActiveTypeStore,
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
        // ★ 2026-11-09 계정 바뀜(로그아웃·다른 계정) — 앞 계정 찜을 비우고 새 계정 것으로.
        onAccountChanged { _state.update { s -> s.copy(likedIds = emptySet()) }; loadLikes() }
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

    /**
     * 줄 누르기 — 바로 상세. ★ 2026-11-15 서버가 일반회원·무용수·비회원에게도 '소개만'(limited) 내려 주므로
     *  예전 유형 확인(비회원 로그인·안내 팝업)은 없앴다. 제안·채팅 버튼만 상세에서 유형을 본다.
     */
    fun open(id: Long) {
        _events.tryEmit(DancerEvent.OpenDetail(id))
    }

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
    private fun loadLikes() {
        val epoch = com.muyeon.app.auth.accountEpochNow()
        viewModelScope.launch {
            val ids = getLikedIds(ScrapTarget.DANCER)
            if (epoch == com.muyeon.app.auth.accountEpochNow()) _state.update { it.copy(likedIds = ids) }
        }
    }

    companion object {
        const val ALL = "ALL"
        private const val K_MAJOR = "dancers.major"
        private const val K_GENDER = "dancers.gender"
        private const val K_VIDEO = "dancers.hasVideo"
    }
}

/** ★ 2026-11-19 탈퇴한 무용수 상세(404) 서버 코드. */
private const val MEMBER_WITHDRAWN = "MEMBER_WITHDRAWN"

data class DancerDetailUiState(
    val profile: DancerProfile? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** ★ 2026-11-19 탈퇴한 무용수(404 MEMBER_WITHDRAWN) — 안내 + [찜 목록에서 빼기]. */
    val withdrawn: Boolean = false,
    val liked: Boolean = false,
    /** 공연팀이면 [캐스팅 제안 보내기], 아니면 [채팅하기]. */
    val isTeam: Boolean = false,
    /** 지금 유형이 공연팀·학원·강사인가 — 아니면 아래 버튼을 그리지 않는다(소개만 보는 회원). */
    val canContact: Boolean = false,
    /** 로그인했나 — 신고·차단 메뉴는 로그인한 회원만. */
    val loggedIn: Boolean = false,
    /** 내 프로필인가(서버 self 또는 토큰 속 내 번호). */
    val isSelf: Boolean = false,
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
    private val activeType: ActiveTypeStore,
    private val tokens: TokenStore,
) : ViewModel() {
    private val _state = MutableStateFlow(viewerState(DancerDetailUiState()))
    val state: StateFlow<DancerDetailUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<DancerEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DancerEvent> = _events.asSharedFlow()

    init {
        load()
        viewModelScope.launch { _state.update { it.copy(liked = id in getLikedIds(ScrapTarget.DANCER)) } }
        onAccountChanged { _state.update { s -> s.copy(liked = false) }; load(); viewModelScope.launch { _state.update { it.copy(liked = id in getLikedIds(ScrapTarget.DANCER)) } } }
        loadCastingCalls()
    }

    private var seenType: String? = com.muyeon.domain.model.member.ActiveType.visible(activeType.current())

    private fun loadCastingCalls() {
        if (_state.value.isTeam) viewModelScope.launch {
            (dancers.myOpenCastingCalls() as? ApiResult.Success)?.let { r -> _state.update { it.copy(castingCalls = r.data) } }
        }
    }

    /**
     * ★ 2026-11-16 화면 안에서 유형이 바뀌었다(MY 두 번 누르기·관문 전환) — 버튼(제안/채팅)과 서버가 주는 공개 범위를 다시 맞춘다.
     *  같은 유형이면 아무것도 하지 않는다(처음 그릴 때).
     */
    fun onTypeChanged(type: String) {
        val t = com.muyeon.domain.model.member.ActiveType.visible(type)
        if (t == seenType) return
        seenType = t
        _state.update { it.copy(castingCalls = emptyList(), pickCall = false, confirmChat = false) }
        load()
        loadCastingCalls()
    }

    /** 보는 사람 기준 값(유형·로그인·본인) — 계정·유형이 바뀌면 다시 맞춘다. 판정은 필요 유형표(FeatureRoles). */
    private fun viewerState(s: DancerDetailUiState): DancerDetailUiState {
        val type = activeType.current()
        val me = tokens.myUserId()
        return s.copy(
            isTeam = com.muyeon.domain.model.member.FeatureRoles.allows(com.muyeon.domain.model.member.TypeFeature.CASTING_OFFER, type),
            canContact = com.muyeon.domain.model.member.FeatureRoles.allows(com.muyeon.domain.model.member.TypeFeature.RECRUIT_CHAT, type),
            loggedIn = tokens.isLoggedIn,
            isSelf = s.profile?.self == true || (me != null && me == id),
        )
    }

    /**
     * 상세 읽기. ★ 2026-11-15 유형을 미리 막지 않는다 — 일반회원·무용수·비회원은 서버가 소개만(limited) 준다.
     *  구직 공개 전인 무용수 등 서버가 거절하면 그 문구를 그대로 보여 준다.
     */
    fun load() {
        _state.update { viewerState(it).copy(isLoading = true, errorMessage = null, withdrawn = false) }
        viewModelScope.launch {
            when (val r = dancers.detail(id)) {
                is ApiResult.Success -> _state.update { viewerState(it.copy(profile = r.data, isLoading = false)) }
                is ApiResult.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = r.message, withdrawn = r.code == MEMBER_WITHDRAWN)
                }
            }
        }
    }

    /** ★ 2026-11-19 탈퇴한 무용수 찜 빼기 — 성공하면 [DancerEvent.Unscrapped] 로 화면을 닫는다. */
    fun unscrapWithdrawn() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val r = toggleScrap(ScrapTarget.DANCER, id, true)) {
                is ToggleScrapResult.Done -> { _state.update { it.copy(busy = false, liked = false) }; _events.tryEmit(DancerEvent.Unscrapped) }
                is ToggleScrapResult.Failed -> { _state.update { it.copy(busy = false) }; _events.tryEmit(DancerEvent.Message(r.message)) }
                ToggleScrapResult.LoginRequired -> { _state.update { it.copy(busy = false) }; _events.tryEmit(DancerEvent.LoginRequired) }
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
