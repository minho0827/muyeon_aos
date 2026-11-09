package com.muyeon.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.member.RoleManageLaunch
import com.muyeon.domain.usecase.member.GetRoleManageLaunchUseCase
import com.muyeon.domain.usecase.member.GetMyRolesUseCase
import com.muyeon.domain.usecase.member.SwitchActiveTypeUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class MainUiState(
    /** 지금 보이는 앱 탭. 탭바 모드·칸은 여기서 계산한다(MainTab.mode·barItem). */
    val selectedKey: String = MainTab.HOME.key,
    /** 2계층(구인구직·커뮤니티·공간)에서 ← 를 누르면 돌아갈 1계층 탭(홈·MY). */
    val lastMainKey: String = MainTab.HOME.key,
    val activeType: String = ActiveType.GENERAL,
    val isLoggedIn: Boolean = false,
    /** 고를 수 있는 유형(일반 + 보유). 비회원·조회 전이면 빈 목록. */
    val switchableTypes: List<String> = emptyList(),
    /** 유형 → 동그라미 사진(탭바 MY 칸 — 인스타그램식). 없는 유형은 아이콘. */
    val typeImages: Map<String, String> = emptyMap(),
    /** 서버 유형 저장이 끝날 때마다 +1 — MY 가 머리글·메뉴를 다시 읽는 신호. */
    val typeEpoch: Int = 0,
    /** MY 탭 빨간 점(웹 useTypeBadges mine>0). MY 탭 이식 때 채운다. */
    val myDot: Boolean = false,
)

/** 화면이 한 번만 처리할 일. */
sealed interface MainEvent {
    data class OpenWebPath(val path: String) : MainEvent
    data class TypeSwitched(val type: String) : MainEvent
    data object TypeSwitchFailed : MainEvent
    /** 유형 편집(회원유형 화면) — 역할 원문·배경 사진을 실어 연다. */
    data class OpenRoleManage(val launch: RoleManageLaunch, val activeType: String) : MainEvent
    data class Message(val text: String) : MainEvent
}

/**
 * 탭 셸 상태 — 탭 고르기 · 탭바 모드 · 유형 시트(웹 ActiveTypeSheet).
 *   ★ 2026-11-08 모든 회원·비회원이 같은 탭(MainTab) — 유형별 서버 탭 목록은 쓰지 않는다.
 */
class MainViewModel(
    private val getMyRoles: GetMyRolesUseCase,
    private val switchActiveType: SwitchActiveTypeUseCase,
    private val getRoleManageLaunch: GetRoleManageLaunchUseCase,
    private val detectRegion: com.muyeon.domain.usecase.region.DetectRegionUseCase,
    private val tokens: TokenStore,
    private val activeTypeStore: ActiveTypeStore,
) : ViewModel() {

    private val _state = MutableStateFlow(
        MainUiState(
            activeType = currentType(),
            isLoggedIn = tokens.isLoggedIn,
        ),
    )
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<MainEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<MainEvent> = _events.asSharedFlow()

    /** 탭 위 앱 화면 열기 요청(바깥 NavHost 경로) — 푸시·링크·웹 창에서 온 상세 주소. */
    // ★ 2026-11-08 Channel — 앱을 막 켠 순간(푸시·링크) 화면이 아직 받기 전이면 SharedFlow 는 그냥 버렸다(상세 대신 홈).
    //  Channel 은 받을 때까지 쥐고 있다가 한 번만 넘긴다.
    private val _routes = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val routes: kotlinx.coroutines.flow.Flow<String> = _routes.receiveAsFlow()

    fun openRoute(route: String) { _routes.trySend(route) }

    /** 지금 탭을 다시 눌렀다(키) — 탭 화면이 맨 위로/다음 칩(웹 emitTabReselect). */
    private val _reselect = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val reselect: SharedFlow<String> = _reselect.asSharedFlow()

    init {
        refreshRoles()
    }

    private fun currentType(): String = ActiveType.visible(activeTypeStore.current())

    /** 화면 복귀 — 웹 창에서 로그인·로그아웃·유형 전환을 했을 수 있다. 바뀌었으면 다시 받는다. */
    fun onResume() {
        val type = currentType()
        val loggedIn = tokens.isLoggedIn
        val s = _state.value
        if (type != s.activeType || loggedIn != s.isLoggedIn) {
            _state.update { it.copy(activeType = type, isLoggedIn = loggedIn, switchableTypes = if (loggedIn) it.switchableTypes else emptyList()) }
        }
        refreshRoles()
    }

    /** 대표 지역이 아직 없으면 좌표로 정한다(웹 RegionProvider 첫 실행과 같다). */
    fun detectRegionIfNeeded(coords: suspend () -> com.muyeon.domain.model.region.Coordinates?) {
        viewModelScope.launch { detectRegion(coords) }
    }

    private fun refreshRoles() {
        if (!tokens.isLoggedIn) return
        viewModelScope.launch {
            val r = getMyRoles()
            if (r is ApiResult.Success) {
                val type = ActiveType.visible(r.data.activeType)
                val changed = type != _state.value.activeType
                if (changed) activeTypeStore.save(type)
                _state.update { it.copy(switchableTypes = r.data.switchable, activeType = type, typeImages = r.data.images) }
            }
        }
    }

    /** 탭 누름. 지금 탭이면 다시 누름 신호. */
    fun onTabTap(key: String) {
        val tab = MainTab.fromKey(key) ?: return
        if (_state.value.selectedKey == key) _reselect.tryEmit(key) else selectTab(tab)
    }

    /** 탭바 칸 누름 — ← 는 1계층의 직전 탭(홈·MY)으로, 나머지는 그 칸의 탭으로. */
    fun onBarItem(item: com.muyeon.app.ui.main.bar.BarItem) {
        val current = MainTab.fromKey(_state.value.selectedKey)
        // ★ 2026-11-09 공연 칸을 다시 누르면 공연 ↔ 캐스팅을 오간다(사용자 요청 — 공연·캐스팅 통합 탭).
        if (item == com.muyeon.app.ui.main.bar.BarItem.PERFORMANCE && (current == MainTab.PERFORMANCE || current == MainTab.CASTING)) {
            selectTab(if (current == MainTab.PERFORMANCE) MainTab.CASTING else MainTab.PERFORMANCE); return
        }
        // 지금 칸을 다시 눌렀다(커뮤니티 안 공연을 보다가 커뮤니티 칸 등) — 그 화면 맨 위로(다시 누름 신호).
        if (current != null && current.barItem == item && item != com.muyeon.app.ui.main.bar.BarItem.BACK) { _reselect.tryEmit(current.key); return }
        val tab = MainTab.forBarItem(item)
        if (tab == null) backToMain() else onTabTap(tab.key)
    }

    /** 2계층에서 1계층으로. 이미 1계층이면 false(뒤로가기 처리를 넘긴다). */
    fun backToMain(): Boolean {
        val s = _state.value
        val tab = MainTab.fromKey(s.selectedKey) ?: return false
        if (tab.mode == com.muyeon.app.ui.main.bar.BarMode.MAIN) return false
        selectTab(MainTab.fromKey(s.lastMainKey) ?: MainTab.HOME)
        return true
    }

    /** MY 에서 로그아웃 — 홈으로(웹 MyHome: logout → /home). */
    fun onLoggedOut() {
        _state.update { it.copy(isLoggedIn = false, activeType = ActiveType.GENERAL, switchableTypes = emptyList(), selectedKey = MainTab.HOME.key, lastMainKey = MainTab.HOME.key) }
    }

    /** 탭 고르기(다른 화면·링크에서 돌아올 때 포함). 1계층 탭이면 ← 의 돌아갈 곳으로 기억한다. */
    fun selectTab(tab: MainTab) {
        // ★ 2026-11-08 웹 /lessons(전체 메뉴 '레슨 찾기' 등)는 홈의 레슨 쪽으로 — 예전 강사 목록 탭으로 가던 것.
        if (tab == MainTab.LESSONS) {
            com.muyeon.app.ui.home.HomeModeRequests.request(com.muyeon.app.ui.home.HomeMode.LESSONS)
            return selectTab(MainTab.HOME)
        }
        _state.update {
            it.copy(selectedKey = tab.key, lastMainKey = if (tab.mode == com.muyeon.app.ui.main.bar.BarMode.MAIN && tab != MainTab.LESSONS) tab.key else it.lastMainKey)
        }
    }

    /** MY 두 번 누르기 — 다음 유형으로 하나씩 돌아가며 바꾼다(인스타그램 계정 전환처럼, 2026-11-08 사용자 요청). */
    fun onMyDoubleTap() {
        val types = _state.value.switchableTypes
        if (types.size < 2) return
        val i = types.indexOf(_state.value.activeType)
        switchTo(types[(i + 1).mod(types.size)])
    }

    /** 유형 시트에서 고름. "MANAGE" = 유형 편집. */
    fun onTypePicked(type: String) {
        if (type == TYPE_MANAGE) openRoleManage() else switchTo(type)
    }

    private fun openRoleManage() {
        viewModelScope.launch {
            when (val r = getRoleManageLaunch()) {
                is ApiResult.Success -> _events.tryEmit(MainEvent.OpenRoleManage(r.data, _state.value.activeType))
                is ApiResult.Error -> _events.tryEmit(MainEvent.Message(r.message))
            }
        }
    }

    private fun switchTo(type: String) {
        val before = _state.value.activeType
        if (type == before) return
        // 탭바 동그라미가 누르는 즉시 움직이게 먼저 바꾸고, 서버가 거절하면 되돌린다.
        _state.update { it.copy(activeType = ActiveType.visible(type)) }
        viewModelScope.launch {
            when (switchActiveType(type)) {
                is ApiResult.Success -> {
                    // 저장소(헤더용 유형)는 서버 저장 뒤에 바뀐다 — MY 가 이 값을 보고 다시 읽도록 한 번 더 알린다.
                    _state.update { it.copy(activeType = ActiveType.visible(type), typeEpoch = it.typeEpoch + 1) }
                    _events.tryEmit(MainEvent.TypeSwitched(type))
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(activeType = before) }
                    _events.tryEmit(MainEvent.TypeSwitchFailed)
                }
            }
        }
    }

    companion object {
        const val TYPE_MANAGE = "MANAGE"
    }
}
