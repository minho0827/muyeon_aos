package com.muyeon.app.ui.performance

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.CursorLoadMoreEffect
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.main.MainTab
import com.muyeon.app.ui.main.TabReselectEffect
import com.muyeon.app.ui.performance.components.PerformanceFilterChips
import com.muyeon.app.ui.performance.components.PerformanceRow
import com.muyeon.app.ui.theme.Dimensions
import org.koin.androidx.compose.koinViewModel

/**
 * 공연 탭 — 주황 위 바 "공연" + 칩 두 줄 + 목록(당겨서 새로고침 · 무한 스크롤).
 *
 * @param onOpenDetail 공연 상세(앱 화면)
 * @param onLogin 비회원 찜 — 웹 로그인 창
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceListScreen(
    onOpenDetail: (Long) -> Unit,
    onLogin: () -> Unit,
    /** 커뮤니티 탭 안 머리. 주면 이 화면 위 바 대신 그린다. */
    header: (@Composable () -> Unit)? = null,
    viewModel: PerformanceListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    TabReselectEffect(MainTab.PERFORMANCE) { viewModel.onTabReselect() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                is PerformanceListEvent.ScrapToast -> Toast.makeText(context, if (e.added) R.string.common_scrap_added else R.string.common_scrap_removed, Toast.LENGTH_SHORT).show()
                is PerformanceListEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
                PerformanceListEvent.LoginRequired -> onLogin()
                PerformanceListEvent.ScrollToTop -> listState.animateScrollToItem(0)
            }
        }
    }
    CursorLoadMoreEffect(listState, canLoad = state.cursor.canAutoLoad && !state.isLoadingMore, onLoadMore = viewModel::loadMore)

    Column(Modifier.fillMaxSize().background(com.muyeon.app.ui.theme.AppColors.Surface)) {
        if (header != null) header() else MainTopBar(title = stringResource(R.string.tab_performance))
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh(pull = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = com.muyeon.app.ui.main.tabBarBottomPadding())) {
                item(key = "chips") {
                    PerformanceFilterChips(
                        genres = viewModel.genreOptions,
                        genre = state.genre,
                        details = state.detailOptions,
                        detail = state.detail,
                        onGenre = viewModel::selectGenre,
                        onDetail = viewModel::selectDetail,
                    )
                }
                when {
                    state.items.isEmpty() && state.isInitialLoading -> Unit
                    state.items.isEmpty() -> item(key = "empty") {
                        EmptyView(
                            title = stringResource(if (state.errorMessage != null) R.string.common_list_failed else R.string.performance_list_empty),
                            buttonText = state.errorMessage?.let { stringResource(R.string.common_retry) },
                            onButton = { viewModel.refresh() },
                        )
                    }
                    else -> itemsIndexed(state.items, key = { _, p -> p.id }) { i, p ->
                        PerformanceRow(
                            item = p,
                            liked = p.id in state.likedIds,
                            onClick = { onOpenDetail(p.id) },
                            onToggleLike = { viewModel.toggleLike(p.id) },
                            // 마지막 줄 아래 선 없음(웹 :last-child).
                            showDivider = i != state.items.lastIndex,
                        )
                    }
                }
                item(key = "footer") {
                    CursorListFooter(
                        loading = state.isLoadingMore,
                        error = state.moreError,
                        hasMore = state.cursor.needsTap || (state.moreError != null),
                        onLoadMore = viewModel::loadMore,
                        modifier = Modifier.padding(bottom = Dimensions.spacingXXL),
                    )
                }
            }
        }
    }
}
