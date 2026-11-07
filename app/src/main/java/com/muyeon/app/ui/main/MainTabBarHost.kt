package com.muyeon.app.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.domain.model.member.ActiveType

/**
 * 탭바 + 탭 셸 상태 연결 — 앱 탭 화면(MainScreen)과 웹 탭 창(WebViewActivity: 구인구직·대타)이 같이 쓴다.
 *   두 곳의 탭바가 똑같이 보이고 똑같이 동작하게 하려는 것.
 *
 * @param selectedKey 선택 칸. 앱 탭이면 상태의 selectedKey, 웹 창이면 그 웹 탭 키.
 * @param onTap 누름 처리. 기본은 셸 상태(앱 탭 화면). 웹 창은 자기 처리(앱 탭으로 돌아가기 등)를 넘긴다.
 */
@Composable
fun MainTabBarHost(
    viewModel: MainViewModel,
    selectedKey: String?,
    onMyLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    onTap: (String) -> Unit = viewModel::onTabTap,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val items = state.tabs.map { menu ->
        val tab = MainTab.fromKey(menu.tabKey)
        TabBarItem(
            key = menu.tabKey,
            label = tabLabel(tab, menu.title, state.isLoggedIn, state.activeType),
            iconRes = MainTab.iconFor(menu.icon, tab),
            showDot = tab == MainTab.MY && state.myDot,
        )
    }
    val myKeys = if (state.isLoggedIn) setOf(MainTab.MY.key) else emptySet()
    MainTabBar(
        items = items,
        selectedKey = selectedKey,
        onTap = onTap,
        modifier = modifier,
        longPressKeys = myKeys,
        doubleTapKeys = if (state.switchableTypes.size == 2) myKeys else emptySet(),
        onLongPress = { onMyLongPress() },
        onDoubleTap = { viewModel.onMyDoubleTap() },
    )
}

/** 탭 이름 — 서버 문구 우선, 없으면 기본. MY 는 로그인 회원이면 활성유형 이름(웹 Footer TYPE_TAB_LABEL). */
@Composable
private fun tabLabel(tab: MainTab?, serverTitle: String?, loggedIn: Boolean, activeType: String): String {
    if (tab == MainTab.MY && loggedIn) {
        return stringResource(
            when (activeType) {
                ActiveType.TEACHER -> R.string.tab_my_teacher
                ActiveType.ACADEMY -> R.string.tab_my_academy
                ActiveType.DANCER -> R.string.tab_my_dancer
                ActiveType.TEAM -> R.string.tab_my_team
                else -> R.string.tab_my_general
            },
        )
    }
    return serverTitle ?: tab?.let { stringResource(it.labelRes) }.orEmpty()
}
