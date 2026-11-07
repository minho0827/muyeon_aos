package com.muyeon.app.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.CursorLoadMoreEffect
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.home.components.HomeClassicHead
import com.muyeon.app.ui.home.components.HomeFeedCard
import com.muyeon.app.ui.home.components.HomeLegalFooter
import com.muyeon.app.ui.home.components.HomeNoticeCommunityFooter
import com.muyeon.app.ui.home.components.HomeSwipeContent
import com.muyeon.app.ui.home.components.HomeTopBar
import com.muyeon.app.ui.home.components.RecommendBlockSheet
import com.muyeon.app.ui.home.components.TeacherHomeContent
import com.muyeon.app.ui.main.MainTab
import com.muyeon.app.ui.main.TabReselectEffect
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.domain.model.member.ActiveType
import kotlinx.coroutines.flow.SharedFlow
import org.koin.androidx.compose.koinViewModel

/**
 * 홈 탭 — 웹 pages/muyeon/Home.js 의 유형별 분기:
 *   강사 → [TeacherHomeContent] · 무용수·공연팀 → 클래식(통합 피드) · 그 밖(일반·비회원·학원) → [HomeSwipeContent].
 *   위 바는 공통(대표 지역 · 알림 종). 탭 다시 누르기 = 맨 위로(웹: 홈은 칩 넘김 없이 맨 위로).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(activeType: String, isLoggedIn: Boolean, actions: HomeActions, viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 탭 셸에서 유형을 바꾸거나 로그아웃하면 화면이 멈추지 않으니(onResume 없음) 셸 값을 따라간다.
    LaunchedEffect(activeType, isLoggedIn) { viewModel.onSession(activeType, isLoggedIn) }
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) { viewModel.onResume(); onPauseOrDispose { } }
    HomeEventsEffect(viewModel.events, actions)

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        HomeTopBar(state.region?.name, state.unread, onRegion = { actions.openPath("/myRegions") }, onBell = { actions.native("openNotifications", emptyMap()) })
        when (state.activeType) {
            ActiveType.TEACHER -> TeacherHomeBody(actions, state, koinViewModel(key = "home-teacher"))
            ActiveType.DANCER, ActiveType.TEAM -> ClassicHomeBody(actions, koinViewModel(key = "home-classic-${state.activeType}"))
            else -> {
                val listState = rememberLazyListState()
                TabReselectEffect(MainTab.HOME) { listState.animateScrollToItem(0) }
                PullToRefreshBox(state.isRefreshing, onRefresh = { viewModel.load(pull = true) }, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = com.muyeon.app.ui.main.tabBarBottomPadding())) {
                        item(key = "sections") { HomeSwipeContent(state, viewModel, actions, Modifier.padding(top = 24.dp, bottom = 32.dp)) }
                        item(key = "footer") { Footer(state.subNotice, state.notices, actions) }
                    }
                }
            }
        }
    }
    state.blockTarget?.let { t -> RecommendBlockSheet(t.name, state.blockBusy, viewModel::confirmBlock) { viewModel.openBlock(null) } }
}

@Composable
private fun Footer(subs: List<com.muyeon.domain.model.home.HomeRecruit>, notices: List<com.muyeon.domain.model.home.HomeNotice>, actions: HomeActions) =
    HomeNoticeCommunityFooter(subs, notices, actions.openPath, onOpenCommunity = { actions.openTab(MainTab.COMMUNITY) }, onDial = actions.dial, onMail = actions.mail)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeacherHomeBody(actions: HomeActions, header: HomeUiState, vm: TeacherHomeViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    HomeEventsEffect(vm.events, actions)
    val listState = rememberLazyListState()
    TabReselectEffect(MainTab.HOME) { listState.animateScrollToItem(0) }
    PullToRefreshBox(s.isRefreshing, onRefresh = { vm.load(pull = true) }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = com.muyeon.app.ui.main.tabBarBottomPadding())) {
            item(key = "sections") { TeacherHomeContent(s, vm, actions, Modifier.padding(top = 24.dp)) }
            item(key = "footer") { Footer(s.subNotice, s.notices, actions) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassicHomeBody(actions: HomeActions, vm: HomeClassicViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    HomeEventsEffect(vm.events, actions)
    LifecycleResumeEffect(Unit) { vm.onResume(); onPauseOrDispose { } }
    val gridState = rememberLazyGridState()
    TabReselectEffect(MainTab.HOME) { gridState.animateScrollToItem(0) }
    CursorLoadMoreEffect(gridState, canLoad = s.cursor.canAutoLoad && !s.isLoadingMore, onLoadMore = vm::loadMore)
    val full: (androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan) = { GridItemSpan(maxLineSpan) }
    PullToRefreshBox(s.isRefreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(GridCells.Fixed(if (s.grid) 2 else 1), Modifier.fillMaxSize(), gridState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = com.muyeon.app.ui.main.tabBarBottomPadding()),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "head", span = full) { HomeClassicHead(s, vm::selectDomain, vm::selectSub, vm::setGrid) }
            when {
                s.items.isEmpty() && s.isInitialLoading -> Unit
                s.items.isEmpty() -> item(key = "empty", span = full) {
                    EmptyView(stringResource(if (s.errorMessage != null) R.string.home_classic_failed else R.string.home_classic_empty),
                        buttonText = s.errorMessage?.let { stringResource(R.string.common_retry) }, onButton = { vm.refresh() })
                }
                else -> itemsIndexed(s.items, key = { _, it -> it.id }) { i, item ->
                    val eid = item.entityId
                    val status = if (item.domain == "JOB" && eid != null) when (eid) {
                        in s.applied -> stringResource(R.string.home_job_applied)
                        in s.viewed -> stringResource(R.string.home_job_viewed)
                        else -> null
                    } else null
                    val chip = status ?: if (s.domain == "ALL") HomeDomainLabels.cardChip(item.domain)?.let { stringResource(it) } else null
                    val tags = item.categories.map { c -> HomeDomainLabels.tag(c)?.let { stringResource(it) } ?: c }
                    val left = !s.grid || i % 2 == 0
                    val right = !s.grid || i % 2 == 1
                    HomeFeedCard(
                        item, s.grid, chip, tags,
                        liked = eid?.takeIf { item.domain in setOf("JOB", "CASTING", "SPACE") }?.let { it in s.liked[item.domain].orEmpty() },
                        onClick = { actions.openPath(item.path) }, onToggleLike = { vm.toggleLike(item) },
                        modifier = Modifier.padding(start = if (left) 20.dp else 0.dp, end = if (right) 20.dp else 0.dp),
                    )
                }
            }
            item(key = "more", span = full) {
                CursorListFooter(s.isLoadingMore, s.moreError, s.cursor.needsTap || s.moreError != null, vm::loadMore)
            }
            item(key = "legal", span = full) {
                Column { Spacer(Modifier.height(24.dp)); HomeLegalFooter(actions.openPath, actions.dial, actions.mail) }
            }
        }
    }
}

/** 찜 토스트·문구·로그인 — 세 홈이 같이 쓴다. */
@Composable
private fun HomeEventsEffect(events: SharedFlow<HomeEvent>, actions: HomeActions) {
    val context = LocalContext.current
    LaunchedEffect(events) {
        events.collect { e ->
            when (e) {
                is HomeEvent.ScrapToast -> Toast.makeText(context, if (e.added) R.string.common_scrap_added else R.string.common_scrap_removed, Toast.LENGTH_SHORT).show()
                is HomeEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
                is HomeEvent.MessageRes -> Toast.makeText(context, e.res, Toast.LENGTH_LONG).show()
                HomeEvent.LoginRequired -> actions.login()
            }
        }
    }
}

