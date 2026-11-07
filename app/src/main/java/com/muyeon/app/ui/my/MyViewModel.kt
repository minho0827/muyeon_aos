package com.muyeon.app.ui.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.navigation.collectAppResults
import com.muyeon.app.result.ResultKeys
import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.menu.AppMenuItem
import com.muyeon.domain.model.my.MyPageInfo
import com.muyeon.domain.usecase.menu.GetMyMenusUseCase
import com.muyeon.domain.usecase.my.GetBadgeCountsUseCase
import com.muyeon.domain.usecase.my.GetMyPageInfoUseCase
import com.muyeon.domain.usecase.my.LogoutUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyUiState(
    val activeType: String = ActiveType.GENERAL,
    val serverMenus: List<AppMenuItem>? = null,
    val info: MyPageInfo? = null,
    val confirmLogout: Boolean = false,
    /** 비공개 공고 안내(계정마다 한 번) — 웹 pendingNotice. */
    val pendingNotice: Boolean = false,
)

sealed interface MyEvent {
    data object LoggedOut : MyEvent
}

/**
 * 회원 MY — 웹 pages/muyeon/my/MyHome.js(MemberMyHome). 서버 메뉴 + 배지(알림·제안) + 멤버십 요약 + 비공개 공고 + 상담 채팅 + 캐스팅 자료.
 *   다른 화면이 돌려준 결과 키로 다시 읽는다(웹 useResultRefresh 와 같은 키).
 */
class MyViewModel(
    private val getMyMenus: GetMyMenusUseCase,
    private val getInfo: GetMyPageInfoUseCase,
    private val badges: GetBadgeCountsUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val tokens: TokenStore,
    private val activeTypeStore: ActiveTypeStore,
    private val prefs: KeyValueStore,
) : ViewModel() {
    private val _state = MutableStateFlow(MyUiState(activeType = currentType()))
    val state: StateFlow<MyUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<MyEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<MyEvent> = _events.asSharedFlow()

    init {
        viewModelScope.collectAppResults(ResultKeys.NOTIFICATIONS) { refreshBadges() }
        viewModelScope.collectAppResults(ResultKeys.QUOTES) { refreshBadges() }
        viewModelScope.collectAppResults(
            ResultKeys.LESSONS, ResultKeys.JOB_POSTINGS, ResultKeys.ACADEMY, ResultKeys.RESUME, ResultKeys.MEMBERSHIP, ResultKeys.PROFILE,
        ) { refresh() }
    }

    private fun currentType() = ActiveType.visible(activeTypeStore.current())

    /** 화면이 보일 때마다 — 유형이 바뀌었을 수 있다(서버가 유형별로 메뉴를 고른다). */
    fun refresh() {
        if (!tokens.isLoggedIn) return
        val type = currentType()
        _state.update { it.copy(activeType = type) }
        viewModelScope.launch {
            val menus = (getMyMenus(type) as? ApiResult.Success)?.data?.takeIf { it.isNotEmpty() }
            _state.update { it.copy(serverMenus = menus) }
        }
        viewModelScope.launch {
            val info = getInfo(type)
            _state.update { it.copy(info = info) }
            maybePendingNotice(info)
        }
    }

    private fun refreshBadges() {
        viewModelScope.launch {
            val n = badges.noti()
            val q = badges.quotes()
            _state.update { s -> s.copy(info = s.info?.copy(notiUnread = n, quoteUnread = q)) }
        }
    }

    /** 비공개 공고가 있고 아직 심사 중이 아니면 계정마다 한 번(웹 muyeon.notice.pendingVerify.<id>). */
    private fun maybePendingNotice(info: MyPageInfo) {
        val p = info.pending ?: return
        if (p.reviewing) return
        val key = tokens.myUserId()?.let { "muyeon.notice.pendingVerify.$it" } ?: return
        if (prefs.getString(key) == "1") return
        prefs.putString(key, "1")
        _state.update { it.copy(pendingNotice = true) }
    }

    fun dismissPendingNotice() = _state.update { it.copy(pendingNotice = false) }
    fun askLogout(ask: Boolean) = _state.update { it.copy(confirmLogout = ask) }

    fun logout() {
        _state.update { MyUiState() }
        logoutUseCase()
        _events.tryEmit(MyEvent.LoggedOut)
    }
}
