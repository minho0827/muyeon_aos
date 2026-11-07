package com.muyeon.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.member.RoleManageLaunch
import com.muyeon.domain.usecase.member.GetRoleManageLaunchUseCase
import com.muyeon.domain.model.menu.AppMenuItem
import com.muyeon.domain.usecase.member.GetMyRolesUseCase
import com.muyeon.domain.usecase.member.SwitchActiveTypeUseCase
import com.muyeon.domain.usecase.menu.GetTabMenusUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    /** 서버 탭 목록(또는 유형별 캐시·기본 목록). 비지 않는다. */
    val tabs: List<AppMenuItem> = emptyList(),
    /** 지금 보이는 앱 탭(NATIVE 탭만 선택 상태가 된다 — 웹·액티비티 탭은 위에 창을 연다). */
    val selectedKey: String = MainTab.HOME.key,
    val activeType: String = ActiveType.GENERAL,
    val isLoggedIn: Boolean = false,
    /** 고를 수 있는 유형(일반 + 보유). 비회원·조회 전이면 빈 목록. */
    val switchableTypes: List<String> = emptyList(),
    /** MY 탭 빨간 점(웹 useTypeBadges mine>0). MY 탭 이식 때 채운다. */
    val myDot: Boolean = false,
)

/** 화면이 한 번만 처리할 일. */
sealed interface MainEvent {
    /** 웹 탭(구인구직·대타) — 웹 창을 탭으로 연다. */
    data class OpenWebTab(val tab: MainTab, val path: String) : MainEvent
    /** 액티비티 탭(캐스팅) 또는 모르는 키의 웹 경로. */
    data class OpenActivityTab(val tab: MainTab) : MainEvent
    data class OpenWebPath(val path: String) : MainEvent
    data class TypeSwitched(val type: String) : MainEvent
    data object TypeSwitchFailed : MainEvent
    /** 유형 편집(회원유형 화면) — 역할 원문·배경 사진을 실어 연다. */
    data class OpenRoleManage(val launch: RoleManageLaunch, val activeType: String) : MainEvent
    data class Message(val text: String) : MainEvent
}

/**
 * 탭 셸 상태 — 웹 Footer.js·useTabMenus·ActiveTypeSheet 의 일을 맡는다.
 *   탭 목록은 활성유형이 바뀔 때마다 다시 받는다(서버가 X-Active-Type 으로 고른다).
 */
class MainViewModel(
    private val getTabMenus: GetTabMenusUseCase,
    private val getMyRoles: GetMyRolesUseCase,
    private val switchActiveType: SwitchActiveTypeUseCase,
    private val getRoleManageLaunch: GetRoleManageLaunchUseCase,
    private val tokens: TokenStore,
    private val activeTypeStore: ActiveTypeStore,
) : ViewModel() {

    private val _state = MutableStateFlow(
        MainUiState(
            activeType = currentType(),
            isLoggedIn = tokens.isLoggedIn,
            tabs = getTabMenus.cachedOrFallback(currentType()),
        ),
    )
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<MainEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<MainEvent> = _events.asSharedFlow()

    /** 탭 위 앱 화면 열기 요청(바깥 NavHost 경로) — 푸시·링크·웹 창에서 온 상세 주소. */
    private val _routes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val routes: SharedFlow<String> = _routes.asSharedFlow()

    fun openRoute(route: String) { _routes.tryEmit(route) }

    /** 지금 탭을 다시 눌렀다(키) — 탭 화면이 맨 위로/다음 칩(웹 emitTabReselect). */
    private val _reselect = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val reselect: SharedFlow<String> = _reselect.asSharedFlow()

    private var tabsJob: Job? = null

    init {
        refreshTabs()
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
            refreshTabs()
        }
        refreshRoles()
    }

    fun refreshTabs() {
        tabsJob?.cancel()
        tabsJob = viewModelScope.launch {
            val tabs = getTabMenus(_state.value.activeType)
            _state.update { s ->
                val keys = tabs.map { it.tabKey }
                // 지금 탭이 새 목록에 없으면(유형 전환으로 강사↔구인구직) 홈으로.
                s.copy(tabs = tabs, selectedKey = if (s.selectedKey in keys) s.selectedKey else MainTab.HOME.key)
            }
        }
    }

    private fun refreshRoles() {
        if (!tokens.isLoggedIn) return
        viewModelScope.launch {
            val r = getMyRoles()
            if (r is ApiResult.Success) {
                val type = ActiveType.visible(r.data.activeType)
                val changed = type != _state.value.activeType
                if (changed) activeTypeStore.save(type)
                _state.update { it.copy(switchableTypes = r.data.switchable, activeType = type) }
                if (changed) refreshTabs()
            }
        }
    }

    /** 탭 누름. 지금 탭이면 다시 누름 신호. */
    fun onTabTap(key: String) {
        val tab = MainTab.fromKey(key)
        val item = _state.value.tabs.firstOrNull { it.tabKey == key }
        when {
            tab == null -> item?.let { _events.tryEmit(MainEvent.OpenWebPath(it.path)) }
            tab.kind == MainTab.Kind.NATIVE ->
                if (_state.value.selectedKey == key) _reselect.tryEmit(key)
                else _state.update { it.copy(selectedKey = key) }
            tab.kind == MainTab.Kind.WEB -> _events.tryEmit(MainEvent.OpenWebTab(tab, item?.path ?: tab.webPath))
            else -> _events.tryEmit(MainEvent.OpenActivityTab(tab))
        }
    }

    /** 다른 화면에서 탭으로 돌아오게 할 때(웹 창에서 /community 로 이동 등). */
    fun selectTab(tab: MainTab) {
        if (tab.kind == MainTab.Kind.NATIVE) _state.update { it.copy(selectedKey = tab.key) }
    }

    /** MY 두 번 누르기 — 고를 유형이 딱 둘이면 다른 쪽으로(웹 Footer 와 같은 규칙). */
    fun onMyDoubleTap() {
        val types = _state.value.switchableTypes
        if (types.size != 2) return
        types.firstOrNull { it != _state.value.activeType }?.let(::switchTo)
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
        if (type == _state.value.activeType) return
        viewModelScope.launch {
            when (switchActiveType(type)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(activeType = ActiveType.visible(type)) }
                    _events.tryEmit(MainEvent.TypeSwitched(type))
                    refreshTabs()
                }
                is ApiResult.Error -> _events.tryEmit(MainEvent.TypeSwitchFailed)
            }
        }
    }

    companion object {
        const val TYPE_MANAGE = "MANAGE"
    }
}
