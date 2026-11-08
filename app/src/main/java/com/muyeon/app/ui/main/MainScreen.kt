package com.muyeon.app.ui.main

import com.muyeon.domain.model.member.ActiveType as ActiveTypeK

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.RecentActors
import com.muyeon.app.ui.components.WriteMenu
import androidx.compose.ui.unit.dp
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
    val recruitActions = androidx.compose.runtime.remember(state.isLoggedIn) {
        com.muyeon.app.ui.recruit.RecruitActions(openPath = onOpenPath, native = onNativeAction, login = { onOpenWeb(WebPaths.login(MainTab.JOBS.webPath)) }, selectTab = viewModel::selectTab)
    }

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
                            com.muyeon.app.ui.components.MainTopBar(stringResource(R.string.tab_casting), chat = true) {
                                com.muyeon.app.ui.components.TopBarIconButton(R.drawable.ic_web_filter, stringResource(R.string.casting_all), onClick = { onOpenDetail(r.castingList) })
                            }
                        },
                    )
                }
                composable(Screen.TabHome.route) {
                    HomeScreen(
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
                composable(Screen.TabPerformance.route) {
                    PerformanceListScreen(
                        onOpenDetail = { id -> onOpenDetail(Screen.PerformanceDetail.build(id)) },
                        onLogin = { onOpenWeb(WebPaths.login(MainTab.PERFORMANCE.webPath)) },
                    )
                }
                // 구인구직·공간 — 앱 화면을 만드는 동안 지금 쓰던 화면을 연다(개편 계획 3·4단계).
                // 채용 2계층 — 구인 · 구직 · 대타 목록(앱). 상세는 아직 웹 창.
                composable(Screen.TabJobs.route) { com.muyeon.app.ui.recruit.HireScreen(recruitActions) }
                composable(Screen.TabSeek.route) { com.muyeon.app.ui.recruit.SeekScreen(recruitActions) }
                composable(Screen.TabSubs.route) { com.muyeon.app.ui.recruit.SubsScreen(recruitActions) }
                composable(Screen.TabSpaces.route) {
                    SpaceRentTab(header = { actions -> TabSectionHeader(spaceTitle(), SPACE_SECTIONS, MainTab.SPACES, viewModel::selectTab, actions) })
                }
                composable(Screen.TabTransfers.route) {
                    TransferTab(header = { actions -> TabSectionHeader(spaceTitle(), SPACE_SECTIONS, MainTab.TRANSFERS, viewModel::selectTab, actions) })
                }
                composable(Screen.TabMy.route) {
                    MyScreen(
                        isLoggedIn = state.isLoggedIn,
                        activeType = "${state.activeType}#${state.typeEpoch}",
                        onOpenWeb = onOpenWeb,
                        onNativeAction = onNativeAction,
                        onRoleManage = { viewModel.onTypePicked(MainViewModel.TYPE_MANAGE) },
                        onOpenCasting = { viewModel.selectTab(MainTab.CASTING) },
                        onOpenCommunity = { onOpenDetail(Screen.CommunityList.route) },
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
                onItemDoubleClick = if (state.switchableTypes.size >= 2) ({ _ -> viewModel.onMyDoubleTap() }) else null,
                // 로그인 회원의 MY 칸만 길게(유형 시트)·두 번(유형 바꾸기) 누르기를 받는다 — 나머지 칸은 바로 반응.
                gestureItems = if (state.isLoggedIn) setOf(BarItem.MY) else emptySet(),
                backDescription = stringResource(R.string.tab_back),
                // 로그인 회원의 MY 칸 — 가진 유형 동그라미(인스타그램식). 비회원은 기존 아이콘.
                customIcon = { item, sel ->
                    if (item == BarItem.MY && state.isLoggedIn) {
                        com.muyeon.app.ui.main.bar.TypeAvatars(state.switchableTypes, state.activeType, state.typeImages, sel); true
                    } else false
                },
            )
        }
        // ★ 2026-11-08 채용 2계층(채용·대타공고·캐스팅) 공용 [+ 글쓰기] — 위 바 연필 대신. 탭바 위에 그려 어두운 막이 탭바까지 덮는다(PaceERA 런커 메뉴와 같은 모양).
        if (selectedTab.mode == BarMode.JOBS) {
            val canCastingCreate = com.muyeon.app.ui.castingstage.stageCanCreate()
            val stage = com.muyeon.app.ui.castingstage.StageRoutes.SHELL
            // ★ 2026-11-08 유형별 쓸 수 있는 글만(웹 constants/permissions.js 와 같다 — 사용자 지적: 일반 회원에게 강사프로필 등록이 보였다).
            //   채용공고 = 학원·공연팀 · 구직 프로필 = 강사·무용수 · 대타공고 = 학원·강사·무용수·공연팀 · 공연 공고 = 공연팀(canCastingCreate).
            val t = state.activeType
            val loggedIn = state.isLoggedIn
            val writeContext = androidx.compose.ui.platform.LocalContext.current
            val writeScope = androidx.compose.runtime.rememberCoroutineScope()
            val writeItems = buildList {
                if (loggedIn && t in setOf(ActiveTypeK.ACADEMY, ActiveTypeK.TEAM))
                    add(com.muyeon.app.ui.components.WriteMenuItem(stringResource(R.string.write_job), androidx.compose.ui.res.painterResource(R.drawable.ic_tab_note), com.muyeon.app.ui.theme.AppColors.Primary, "job") { onOpenPath("/jobs/create") })
                if (loggedIn && t in setOf(ActiveTypeK.TEACHER, ActiveTypeK.DANCER))
                    add(com.muyeon.app.ui.components.WriteMenuItem(stringResource(if (t == ActiveTypeK.DANCER) R.string.write_seek_dancer else R.string.write_seek), androidx.compose.ui.res.painterResource(R.drawable.ic_tab_my), androidx.compose.ui.graphics.Color(0xFF2E7DFF), "seek") {
                        // 이미 등록한 강사프로필이 있으면 이력서 관리, 없으면 등록(SeekProfileEntry).
                        (writeContext as? android.app.Activity)?.let { com.muyeon.app.ui.resume.SeekProfileEntry.open(it, writeScope, t == ActiveTypeK.DANCER) }
                    })
                if (loggedIn && t in setOf(ActiveTypeK.TEACHER, ActiveTypeK.ACADEMY, ActiveTypeK.DANCER, ActiveTypeK.TEAM))
                    add(com.muyeon.app.ui.components.WriteMenuItem(stringResource(R.string.write_sub), androidx.compose.ui.res.painterResource(R.drawable.ic_tab_paper), androidx.compose.ui.graphics.Color(0xFFD8564D), "sub") { onOpenPath("/subs/create") })
                if (canCastingCreate) add(com.muyeon.app.ui.components.WriteMenuItem(stringResource(R.string.write_casting), androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.RecentActors), androidx.compose.ui.graphics.Color(0xFF7B5CC4), "casting") { onOpenDetail(stage.create) })
            }
            // 쓸 수 있는 글이 없으면(일반·비회원) 버튼도 없다.
            if (writeItems.isNotEmpty()) WriteMenu(
                items = writeItems,
                bottom = tabBarBottomPadding() - 4.dp,
            )
        }
    }
}

/** 칸 이름 — MY 는 로그인 회원이면 활성유형 이름(웹 Footer TYPE_TAB_LABEL). */
@Composable
private fun barLabel(item: BarItem, mode: BarMode, loggedIn: Boolean, activeType: String): String = stringResource(
    when (item) {
        BarItem.HOME -> R.string.tab_home
        BarItem.PERFORMANCE -> R.string.tab_performance
        BarItem.SPACE -> R.string.tab_space
        BarItem.JOBS -> R.string.tab_recruit
        BarItem.SUBS -> R.string.tab_subs
        BarItem.CASTING -> R.string.tab_casting
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

/** 칸 그림 — 웹에서 옮긴 탭 아이콘(ic_tab_*), 없는 것(공간·캐스팅)은 머티리얼 둥근 아이콘. */
@Composable
private fun barIcon(item: BarItem): Painter = when (item) {
    BarItem.HOME, BarItem.BACK -> painterResource(R.drawable.ic_tab_home)
    BarItem.PERFORMANCE -> painterResource(R.drawable.ic_tab_star)
    BarItem.SPACE -> rememberVectorPainter(Icons.Rounded.MeetingRoom)
    BarItem.JOBS -> painterResource(R.drawable.ic_tab_note)
    BarItem.SUBS -> painterResource(R.drawable.ic_tab_paper)
    BarItem.CASTING -> rememberVectorPainter(Icons.Rounded.RecentActors)
    BarItem.MY -> painterResource(R.drawable.ic_tab_my)
}
