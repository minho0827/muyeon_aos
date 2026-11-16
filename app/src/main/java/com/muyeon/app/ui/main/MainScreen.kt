package com.muyeon.app.ui.main

import com.muyeon.domain.model.member.ActiveType as ActiveTypeK

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.RecentActors
import androidx.compose.material.icons.filled.Apartment
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
import com.muyeon.domain.model.member.TypeFeature
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.testTag
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
                // ★ 2026-11-09 공연·캐스팅 통합 탭 — 캐스팅 칸(검색창 위 바 + [공연][캐스팅] 줄 + 캐스팅·무대 본문).
                //   상세·목록은 바깥 NavHost(탭 위 화면, StageRoutes.SHELL)로 밀려 들어온다.
                composable(Screen.TabCasting.route) {
                    val r = com.muyeon.app.ui.castingstage.StageRoutes.SHELL
                    // 계정이 바뀌면 새로 그린다(찜·공연팀 여부가 계정마다 다르다).
                    androidx.compose.runtime.key(com.muyeon.app.auth.accountEpoch()) {
                    com.muyeon.app.ui.castingstage.CastingHomeScreen(
                        api = com.muyeon.app.ui.castingstage.rememberStageApi(),
                        onSearch = { onOpenDetail(Screen.Search.build(com.muyeon.app.ui.search.SearchDomainKeys.CASTING)) },
                        onMode = { if (it == com.muyeon.app.ui.performance.components.PerfCastingMode.PERFORMANCE) viewModel.selectTab(MainTab.PERFORMANCE) },
                        onOpenCasting = { onOpenDetail(r.casting(it)) },
                        onOpenNews = { item, cat ->
                            com.muyeon.app.ui.castingstage.CastingStageSession.newsCache[item.id] = item to cat
                            onOpenDetail(r.news(item.id))
                        },
                        onCastingList = { onOpenDetail(r.castingList) },
                        onNewsList = { no, code -> onOpenDetail(r.newsList(no, code)) },
                        onIssues = { onOpenDetail(r.issues) },
                        onOpenIssue = { no -> onOpenDetail(r.issue(no)) },
                        reloadKey = com.muyeon.app.ui.castingstage.CastingStageSession.reloadKey,
                    )
                    }
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
                        onOpenDancer = { id -> onOpenPath("/dancers/$id") },
                        onRequestLesson = { category, teacherId ->
                            onNativeAction("openQuoteWizard", mapOf("category" to category, "targetTeacherId" to teacherId.toString()))
                        },
                        onLogin = { onOpenWeb(WebPaths.login(MainTab.LESSONS.webPath)) },
                    )
                }
                // ★ 2026-11-09 공연·캐스팅 통합 탭 — 공연 칸.
                composable(Screen.TabPerformance.route) {
                    com.muyeon.app.ui.performance.PerformanceHomeScreen(
                        onSearch = { onOpenDetail(Screen.Search.build(com.muyeon.app.ui.search.SearchDomainKeys.PERFORMANCE)) },
                        onMode = { if (it == com.muyeon.app.ui.performance.components.PerfCastingMode.CASTING) viewModel.selectTab(MainTab.CASTING) },
                        onOpenDetail = { id -> onOpenDetail(Screen.PerformanceDetail.build(id)) },
                        onLogin = { onOpenWeb(WebPaths.login(MainTab.PERFORMANCE.webPath)) },
                    )
                }
                // 구인구직·공간 — 앱 화면을 만드는 동안 지금 쓰던 화면을 연다(개편 계획 3·4단계).
                // 채용 2계층 — 구인 · 구직 · 대타 목록(앱). 상세는 아직 웹 창.
                composable(Screen.TabJobs.route) { com.muyeon.app.ui.recruit.HireScreen(recruitActions) }
                composable(Screen.TabSeek.route) { com.muyeon.app.ui.recruit.SeekScreen(recruitActions) }
                composable(Screen.TabSubs.route) { com.muyeon.app.ui.recruit.SubsScreen(recruitActions) }
                // ★ 2026-11-09 공간·양도 탭은 화면 안(remember)에 찜·내 번호를 쥔다 — 계정이 바뀌면 통째로 새로 그린다.
                composable(Screen.TabSpaces.route) {
                    androidx.compose.runtime.key(com.muyeon.app.auth.accountEpoch()) {
                        SpaceRentTab(header = { actions -> TabSectionHeader(spaceTitle(), SPACE_SECTIONS, MainTab.SPACES, viewModel::selectTab, actions) })
                    }
                }
                composable(Screen.TabTransfers.route) {
                    androidx.compose.runtime.key(com.muyeon.app.auth.accountEpoch()) {
                        TransferTab(header = { actions -> TabSectionHeader(spaceTitle(), SPACE_SECTIONS, MainTab.TRANSFERS, viewModel::selectTab, actions) })
                    }
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
                dotOn = { item -> (item == BarItem.MY && state.myDot) || (item == BarItem.PERFORMANCE && state.perfDot) },
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
        // ★ 2026-11-16 로그인 회원이면 늘 보인다(비회원만 숨김). 항목은 모두 두고, 지금 유형으로 못 쓰는 항목은 필요 유형을 회색 작은 글씨로 붙인다.
        //   누르면 공통 관문(TypeGate)을 지난다 — 가진 유형이면 "전환하고 작성", 없으면 "유형 추가" 안내. 필요 유형표는 :domain FeatureRoles.
        val gate = com.muyeon.app.ui.common.rememberTypeGate()
        val writeContext = androidx.compose.ui.platform.LocalContext.current
        val writeScope = androidx.compose.runtime.rememberCoroutineScope()
        if (selectedTab.mode == BarMode.JOBS) {
            val stage = com.muyeon.app.ui.castingstage.StageRoutes.SHELL
            val t = state.activeType
            val writeItems = com.muyeon.domain.model.member.WriteMenuRules.jobs(state.isLoggedIn, t).map { e ->
                when (e.feature) {
                    TypeFeature.JOB_CREATE -> com.muyeon.app.ui.components.WriteMenuItem(stringResource(R.string.write_job), painterResource(R.drawable.ic_tab_note), AppColors.Primary, "job", e.caption) {
                        gate.require(e.feature) { onOpenPath("/jobs/create") }
                    }
                    TypeFeature.SEEK_PROFILE -> com.muyeon.app.ui.components.WriteMenuItem(
                        stringResource(when (t) { ActiveTypeK.DANCER -> R.string.write_seek_dancer; ActiveTypeK.TEACHER -> R.string.write_seek; else -> R.string.write_seek_any }),
                        painterResource(R.drawable.ic_tab_my), androidx.compose.ui.graphics.Color(0xFF2E7DFF), "seek", e.caption,
                    ) {
                        gate.require(e.feature) {
                            // 이미 등록한 강사프로필이 있으면 이력서 관리, 없으면 등록(SeekProfileEntry). 전환 뒤 유형으로 고른다.
                            val dancer = com.muyeon.app.webview.ActiveRole.current(writeContext) == ActiveTypeK.DANCER
                            (writeContext as? android.app.Activity)?.let { com.muyeon.app.ui.resume.SeekProfileEntry.open(it, writeScope, dancer) }
                        }
                    }
                    TypeFeature.SUB_CREATE -> com.muyeon.app.ui.components.WriteMenuItem(stringResource(R.string.write_sub), painterResource(R.drawable.ic_tab_paper), androidx.compose.ui.graphics.Color(0xFFD8564D), "sub", e.caption) {
                        gate.require(e.feature) { onOpenPath("/subs/create") }
                    }
                    else -> com.muyeon.app.ui.components.WriteMenuItem(stringResource(R.string.write_casting), rememberVectorPainter(Icons.Rounded.RecentActors), androidx.compose.ui.graphics.Color(0xFF7B5CC4), "casting", e.caption) {
                        gate.require(e.feature) { onOpenDetail(stage.create) }
                    }
                }
            }
            if (writeItems.isNotEmpty()) WriteMenu(
                items = writeItems,
                bottom = tabBarBottomPadding() - 4.dp,
                header = com.muyeon.domain.model.member.WriteMenuRules.header(t),
            )
        }
        // ★ 2026-11-08 공간 탭(대관·양도양수) [+ 글쓰기] — 채용 탭과 같은 부품. 서버 등록 권한 그대로:
        //   공간 등록 POST /spaces = 학원·강사·무용수 · 양도 글 POST /studio-transfers = 학원·강사. 보고 있는 탭 항목이 먼저.
        // ★ 2026-11-16 로그인 회원이면 늘 보이고, 누르면 관문을 지난 뒤 등록 화면을 연다(등록 화면 안 유형 검사를 건너뛰지 않게).
        if (selectedTab == MainTab.SPACES || selectedTab == MainTab.TRANSFERS) {
            val t = state.activeType
            val writeItems = com.muyeon.domain.model.member.WriteMenuRules.spaces(state.isLoggedIn, t, transfersFirst = selectedTab == MainTab.TRANSFERS).map { e ->
                if (e.feature == TypeFeature.SPACE_CREATE)
                    com.muyeon.app.ui.components.WriteMenuItem(stringResource(R.string.write_space), rememberVectorPainter(Icons.Rounded.MeetingRoom), AppColors.Primary, "space", e.caption) {
                        gate.require(e.feature) { com.muyeon.app.ui.space.SpaceHubActivity.startEditor(writeContext, null) }
                    }
                else com.muyeon.app.ui.components.WriteMenuItem(stringResource(R.string.write_transfer), rememberVectorPainter(Icons.Filled.Apartment), androidx.compose.ui.graphics.Color(0xFF2E7DFF), "transfer", e.caption) {
                    gate.require(e.feature) { com.muyeon.app.ui.transfer.TransferActivity.startEditor(writeContext, null) }
                }
            }
            if (writeItems.isNotEmpty()) WriteMenu(
                items = writeItems,
                bottom = tabBarBottomPadding() - 4.dp,
                header = com.muyeon.domain.model.member.WriteMenuRules.header(t),
            )
        }
        com.muyeon.app.ui.common.TypeGateHost(gate)
        // ★ 2026-11-16 MY 말풍선 — 전문 유형을 가졌는데 지금 일반이면 MY 칸 위에. 누르면 유형 시트, X 는 이번 실행 동안 숨김.
        //   유형이 바뀌면(일반이 아니면) 조건이 깨져 바로 사라진다. MY 칸은 1계층 탭바에만 있다.
        val hintType = com.muyeon.domain.model.member.MyTypeHintRules.bubbleType(
            state.isLoggedIn, state.activeType, state.switchableTypes, state.myHintDismissed,
        )
        if (hintType != null && selectedTab.mode == BarMode.MAIN) {
            MyTypeHintBubble(
                text = com.muyeon.domain.model.member.MyTypeHintRules.text(hintType),
                onOpen = onMyLongPress,
                onClose = viewModel::dismissMyTypeHint,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** MY 칸 위 말풍선 — 꼬리가 MY 칸 가운데(탭바 폭의 90% 자리)를 가리킨다. */
@Composable
private fun MyTypeHintBubble(text: String, onOpen: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val nav = androidx.compose.foundation.layout.WindowInsets.Companion.navigationBars.asPaddingValues().calculateBottomPadding()
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxWidth().padding(bottom = nav + 76.dp).testTag("myTypeHint")) {
        // 탭바 안쪽 여백 6dp · 5칸 중 마지막 칸 가운데 = 오른쪽 끝에서 (폭-12)/10 + 6.
        val fromRight = (maxWidth - 12.dp) / 10 + 6.dp
        com.muyeon.app.ui.chat.ChatSurveyHintBubble(
            onOpen = onOpen, onClose = onClose,
            title = text, subtitle = null,
            tailAtEnd = true,
            tailInset = (fromRight - 12.dp - 6.dp).coerceAtLeast(4.dp),
            closeDescription = stringResource(R.string.my_type_hint_close),
        )
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
    BarItem.MY -> painterResource(R.drawable.ic_tab_my)
}
