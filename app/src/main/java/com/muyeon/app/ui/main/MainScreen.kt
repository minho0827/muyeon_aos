package com.muyeon.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.muyeon.app.R
import com.muyeon.app.ui.main.bar.BarItem
import com.muyeon.app.ui.main.bar.BarMode
import com.muyeon.app.ui.main.bar.MorphingTabBar
import com.muyeon.domain.model.member.ActiveType
import androidx.compose.foundation.layout.fillMaxSize
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
import com.muyeon.app.ui.my.MyScreen
import com.muyeon.app.ui.community.CommunityListScreen
import com.muyeon.app.ui.lessons.LessonTabScreen
import com.muyeon.app.ui.home.HomeActions
import com.muyeon.app.ui.home.HomeScreen
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
    /** 기존 앱 화면 열기 — 웹 브리지와 같은 액션 이름·데이터(NativeActionRouter 표). */
    onNativeAction: (String, Map<String, String>) -> Unit,
    /** 웹 주소 열기 — 앱으로 옮긴 주소면 앱 화면, 아니면 웹 창(NativeRoutes). */
    onOpenPath: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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

    // 안쪽 NavHost 가 뒤로가기로 탭을 되돌리면(공연 → 홈) 선택 상태도 따라간다 — 화면과 탭바가 어긋나지 않게.
    LaunchedEffect(tabNav) {
        tabNav.currentBackStackEntryFlow.collect { entry ->
            MainTab.fromRoute(entry.destination.route)?.let(viewModel::selectTab)
        }
    }

    // ★ 2026-11-08 토스 방식 탭바 — 탭 화면은 전체 화면으로 깔고 그 위에 바를 얹는다(1계층 붙은 바 · 2계층 떠 있는 캡슐).
    //   화면 맨 아래 여백은 각 목록이 tabBarBottomPadding() 으로 둔다.
    val selectedTab = MainTab.fromKey(state.selectedKey) ?: MainTab.HOME

    Box(Modifier.fillMaxSize().background(AppColors.Surface)) {
        Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalTabReselect provides viewModel.reselect) {
            NavHost(
                tabNav, startDestination = Screen.TabHome.route,
                enterTransition = com.muyeon.app.ui.common.nav.MuyeonNav.tabEnter, exitTransition = com.muyeon.app.ui.common.nav.MuyeonNav.tabExit,
                popEnterTransition = com.muyeon.app.ui.common.nav.MuyeonNav.tabEnter, popExitTransition = com.muyeon.app.ui.common.nav.MuyeonNav.tabExit,
            ) {
                // 캐스팅·무대 첫 화면 — 상세·목록은 바깥 NavHost(탭 위 화면, StageRoutes.SHELL)로 밀려 들어온다.
                composable(Screen.TabCasting.route) {
                    val r = com.muyeon.app.ui.castingstage.StageRoutes.SHELL
                    val canCreate = com.muyeon.app.ui.castingstage.stageCanCreate()
                    com.muyeon.app.ui.castingstage.screens.CastingStageScreen(
                        api = com.muyeon.app.ui.castingstage.rememberStageApi(),
                        onClose = null,
                        onOpenCasting = { onOpenDetail(r.casting(it)) },
                        onOpenNews = { item, cat ->
                            com.muyeon.app.ui.castingstage.CastingStageSession.newsCache[item.id] = item to cat
                            onOpenDetail(r.news(item.id))
                        },
                        onCastingList = { onOpenDetail(r.castingList) },
                        onNewsList = { no, code -> onOpenDetail(r.newsList(no, code)) },
                        onIssues = { onOpenDetail(r.issues) },
                        onCreate = if (canCreate) ({ onOpenDetail(r.create) }) else null,
                        reloadKey = com.muyeon.app.ui.castingstage.CastingStageSession.reloadKey,
                        header = {
                            TabSectionHeader(communityTitle(), COMMUNITY_SECTIONS, MainTab.CASTING, viewModel::selectTab) {
                                if (canCreate) com.muyeon.app.ui.components.TopBarIconButton(R.drawable.ic_web_edit, stringResource(R.string.casting_create), onClick = { onOpenDetail(r.create) })
                                com.muyeon.app.ui.components.TopBarIconButton(R.drawable.ic_web_filter, stringResource(R.string.casting_all), onClick = { onOpenDetail(r.castingList) })
                            }
                        },
                    )
                }
                composable(Screen.TabHome.route) {
                    HomeScreen(
                        activeType = state.activeType,
                        isLoggedIn = state.isLoggedIn,
                        actions = HomeActions(
                            openPath = onOpenPath,
                            native = onNativeAction,
                            openTab = viewModel::selectTab,
                            login = { onOpenWeb(WebPaths.login(MainTab.HOME.webPath)) },
                            dial = { tel -> context.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + tel.replace("-", "")))) },
                            mail = { mail -> runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:$mail"))) } },
                        ),
                    )
                }
                composable(Screen.TabLessons.route) {
                    LessonTabScreen(
                        onOpenProfile = { id -> onNativeAction("openPublicProfile", mapOf("userId" to id.toString())) },
                        onRequestLesson = { category, teacherId ->
                            onNativeAction("openQuoteWizard", mapOf("category" to category, "targetTeacherId" to teacherId.toString()))
                        },
                        onLogin = { onOpenWeb(WebPaths.login(MainTab.LESSONS.webPath)) },
                    )
                }
                composable(Screen.TabCommunity.route) {
                    CommunityListScreen(
                        onOpenPost = { id -> onOpenDetail(Screen.CommunityPost.build(id)) },
                        // 글쓰기는 로그인 필요(웹 community.write 게이트) — 비회원은 로그인 뒤 글쓰기로.
                        onWrite = { onOpenWeb(if (state.isLoggedIn) WebPaths.COMMUNITY_WRITE else WebPaths.login(WebPaths.COMMUNITY_WRITE)) },
                        header = {
                            TabSectionHeader(communityTitle(), COMMUNITY_SECTIONS, MainTab.COMMUNITY, viewModel::selectTab) {
                                com.muyeon.app.ui.components.TopBarIconButton(R.drawable.ic_web_edit, stringResource(R.string.community_write), onClick = {
                                    onOpenWeb(if (state.isLoggedIn) WebPaths.COMMUNITY_WRITE else WebPaths.login(WebPaths.COMMUNITY_WRITE))
                                })
                            }
                        },
                    )
                }
                composable(Screen.TabPerformance.route) {
                    PerformanceListScreen(
                        onOpenDetail = { id -> onOpenDetail(Screen.PerformanceDetail.build(id)) },
                        onLogin = { onOpenWeb(WebPaths.login(MainTab.PERFORMANCE.webPath)) },
                        header = { TabSectionHeader(communityTitle(), COMMUNITY_SECTIONS, MainTab.PERFORMANCE, viewModel::selectTab) },
                    )
                }
                // 구인구직·공간 — 앱 화면을 만드는 동안 지금 쓰던 화면을 연다(개편 계획 3·4단계).
                composable(Screen.TabJobs.route) { InterimTabScreen(stringResource(R.string.tab_jobs_hire)) { onOpenWeb("/jobs?tab=hire") } }
                composable(Screen.TabSeek.route) { InterimTabScreen(stringResource(R.string.tab_jobs_seek)) { onOpenWeb("/jobs?tab=seek") } }
                composable(Screen.TabSubs.route) { InterimTabScreen(stringResource(R.string.tab_subs)) { onOpenWeb("/subs") } }
                composable(Screen.TabSpaces.route) {
                    InterimTabScreen(stringResource(R.string.tab_space_rent), header = { TabSectionHeader(spaceTitle(), SPACE_SECTIONS, MainTab.SPACES, viewModel::selectTab) }) {
                        onNativeAction("openSpaces", emptyMap())
                    }
                }
                composable(Screen.TabTransfers.route) {
                    InterimTabScreen(stringResource(R.string.tab_transfer), header = { TabSectionHeader(spaceTitle(), SPACE_SECTIONS, MainTab.TRANSFERS, viewModel::selectTab) }) {
                        onNativeAction("openStudioTransfers", emptyMap())
                    }
                }
                composable(Screen.TabMy.route) {
                    MyScreen(
                        isLoggedIn = state.isLoggedIn,
                        onOpenWeb = onOpenWeb,
                        onNativeAction = onNativeAction,
                        onRoleManage = { viewModel.onTypePicked(MainViewModel.TYPE_MANAGE) },
                        onOpenCasting = { viewModel.selectTab(MainTab.CASTING) },
                        onOpenTab = viewModel::selectTab,
                        onLoggedOut = viewModel::onLoggedOut,
                    )
                }
            }
        }
        }
        FloatingTabBar(floating = selectedTab.mode != BarMode.MAIN, modifier = Modifier.align(Alignment.BottomCenter)) {
            MorphingTabBar(
                mode = selectedTab.mode,
                selected = selectedTab.barItem,
                label = { item, mode -> barLabel(item, mode, state.isLoggedIn, state.activeType) },
                icon = { item -> barIcon(item) },
                dotOn = { item -> item == BarItem.MY && state.myDot },
                onItemClick = viewModel::onBarItem,
                onItemLongClick = { item -> if (item == BarItem.MY && state.isLoggedIn) onMyLongPress() },
                onItemDoubleClick = { item -> if (item == BarItem.MY && state.switchableTypes.size == 2) viewModel.onMyDoubleTap() else viewModel.onBarItem(item) },
                backDescription = stringResource(R.string.tab_back),
            )
        }
    }
}

/** 칸 이름 — 부모 칸은 2계층에서 첫 하위 탭 이름. MY 는 로그인 회원이면 활성유형 이름(웹 Footer TYPE_TAB_LABEL). */
@Composable
private fun barLabel(item: BarItem, mode: BarMode, loggedIn: Boolean, activeType: String): String {
    val main = mode == BarMode.MAIN
    return stringResource(
        when (item) {
            BarItem.HOME -> R.string.tab_home
            BarItem.COMMUNITY -> R.string.tab_community
            BarItem.SPACE -> R.string.tab_space
            BarItem.JOBS -> if (main) R.string.tab_recruit else R.string.tab_jobs_hire
            BarItem.SEEK -> R.string.tab_jobs_seek
            BarItem.SUBS -> R.string.tab_subs_short
            BarItem.BACK -> R.string.tab_back
            BarItem.MY -> if (!loggedIn) R.string.tab_my else when (activeType) {
                ActiveType.TEACHER -> R.string.tab_my_teacher
                ActiveType.ACADEMY -> R.string.tab_my_academy
                ActiveType.DANCER -> R.string.tab_my_dancer
                ActiveType.TEAM -> R.string.tab_my_team
                else -> R.string.tab_my_general
            }
        },
    )
}

/** 칸 그림 — 웹에서 옮긴 탭 아이콘(ic_tab_*), 없는 것(공간·구직)은 머티리얼 둥근 아이콘. */
@Composable
private fun barIcon(item: BarItem): Painter = when (item) {
    BarItem.HOME, BarItem.BACK -> painterResource(R.drawable.ic_tab_home)
    BarItem.COMMUNITY -> painterResource(R.drawable.ic_tab_community)
    BarItem.SPACE -> rememberVectorPainter(Icons.Rounded.MeetingRoom)
    BarItem.JOBS -> painterResource(R.drawable.ic_tab_note)
    BarItem.SEEK -> rememberVectorPainter(Icons.Rounded.PersonSearch)
    BarItem.SUBS -> painterResource(R.drawable.ic_tab_paper)
    BarItem.MY -> painterResource(R.drawable.ic_tab_my)
}
