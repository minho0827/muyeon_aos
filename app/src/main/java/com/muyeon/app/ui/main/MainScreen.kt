package com.muyeon.app.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.navigation.Screen
import com.muyeon.app.navigation.WebPaths
import com.muyeon.app.ui.performance.PerformanceListScreen
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/**
 * 탭 셸 — 하단 탭바 + 탭 화면(안쪽 NavHost, 탭마다 스크롤·칩 상태를 지킨다).
 *   PaceERA MainScreen 과 같은 구조. 탭 위 상세(글 상세 등)는 바깥 [com.muyeon.app.navigation.AppNavHost] 가 띄운다.
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenDetail: (String) -> Unit,
    onMyLongPress: () -> Unit,
    /** 웹 페이지 창(로그인 등). */
    onOpenWeb: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tabNav = rememberNavController()

    // 선택 탭 → 안쪽 NavHost 이동(탭 상태 저장·복원).
    LaunchedEffect(state.selectedKey) {
        val route = MainTab.fromKey(state.selectedKey)?.route ?: return@LaunchedEffect
        if (tabNav.currentDestination?.route == route) return@LaunchedEffect
        tabNav.navigate(route) {
            popUpTo(tabNav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = AppColors.Surface,
        bottomBar = {
            MainTabBarHost(viewModel, selectedKey = state.selectedKey, onMyLongPress = onMyLongPress)
        },
    ) { inner ->
        CompositionLocalProvider(LocalTabReselect provides viewModel.reselect) {
            NavHost(tabNav, startDestination = Screen.TabHome.route, modifier = Modifier.padding(inner)) {
                composable(Screen.TabHome.route) { TabPlaceholder(MainTab.HOME) }
                composable(Screen.TabLessons.route) { TabPlaceholder(MainTab.LESSONS) }
                composable(Screen.TabCommunity.route) { TabPlaceholder(MainTab.COMMUNITY) }
                composable(Screen.TabPerformance.route) {
                    PerformanceListScreen(
                        onOpenDetail = { id -> onOpenDetail(Screen.PerformanceDetail.build(id)) },
                        onLogin = { onOpenWeb(WebPaths.login(MainTab.PERFORMANCE.webPath)) },
                    )
                }
                composable(Screen.TabMy.route) { TabPlaceholder(MainTab.MY) }
            }
        }
    }
}

/** 탭 화면 자리 — 2단계(탭별 이식)에서 각 기능 화면으로 바뀐다. */
@Composable
private fun TabPlaceholder(tab: MainTab) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(tab.labelRes), style = AppTypography.Section16, color = AppColors.TextSub)
    }
}
