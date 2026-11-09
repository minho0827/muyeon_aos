package com.muyeon.app.ui.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.main.tabBarBottomPadding
import com.muyeon.app.ui.performance.components.PerformanceFilterChips
import com.muyeon.app.ui.performance.components.PerformanceListEffects
import com.muyeon.app.ui.performance.components.performanceRows
import com.muyeon.app.ui.theme.AppColors
import org.koin.androidx.compose.koinViewModel

/**
 * 공연 탭(예전 모양) — 주황 위 바 "공연" + 칩 두 줄 + 목록(당겨서 새로고침 · 무한 스크롤).
 *   공연·캐스팅 통합 탭은 [PerformanceHomeScreen].
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
    val listState = rememberLazyListState()
    PerformanceListEffects(viewModel, listState, canLoadMore = state.cursor.canAutoLoad && !state.isLoadingMore, onLogin = onLogin)

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        if (header != null) header() else MainTopBar(title = stringResource(R.string.tab_performance), chat = true)
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh(pull = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = tabBarBottomPadding())) {
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
                performanceRows(state, viewModel, onOpenDetail)
            }
        }
    }
}
