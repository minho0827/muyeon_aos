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
import com.muyeon.app.ui.components.BrandSearchBar
import com.muyeon.app.ui.main.tabBarBottomPadding
import com.muyeon.app.ui.performance.components.PerfCastingHeader
import com.muyeon.app.ui.performance.components.PerfCastingItem
import com.muyeon.app.ui.performance.components.PerfCastingMode
import com.muyeon.app.ui.performance.components.PerformanceDetailChips
import com.muyeon.app.ui.performance.components.PerformanceListEffects
import com.muyeon.app.ui.performance.components.performanceRows
import com.muyeon.app.ui.theme.AppColors
import org.koin.androidx.compose.koinViewModel

/**
 * ★ 2026-11-09 공연·캐스팅 통합 탭 — 공연 칸.
 *   검색창 위 바 · [공연][캐스팅] | 장르 줄 · 세부 칩 · 목록. ★ 11-10 [전체]도 다른 장르처럼 목록만('지금 공연 중' 줄 뺌, 사용자 결정).
 *
 * @param onSearch 위 검색창 — 검색 화면(공연 탭으로)
 * @param onMode [캐스팅] 알약
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceHomeScreen(
    onSearch: () -> Unit,
    onMode: (PerfCastingMode) -> Unit,
    onOpenDetail: (Long) -> Unit,
    onLogin: () -> Unit,
    viewModel: PerformanceListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val allLabel = stringResource(R.string.common_all)
    PerformanceListEffects(viewModel, listState, canLoadMore = state.cursor.canAutoLoad && !state.isLoadingMore, onLogin = onLogin)
    // 장르·세부 칩을 바꾸면 새 목록 맨 위부터(11-10). 다른 화면에서 돌아올 때는 보던 자리 그대로.
    com.muyeon.app.ui.common.OnValueChanged(state.genre to state.detail.key) { listState.scrollToItem(0) }

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        BrandSearchBar(onClick = onSearch)
        PerfCastingHeader(
            mode = PerfCastingMode.PERFORMANCE,
            onMode = onMode,
            items = viewModel.genreOptions.map { PerfCastingItem(it, if (it == PerformanceListViewModel.ALL) allLabel else it) },
            selected = state.genre,
            onSelect = viewModel::selectGenre,
        )
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh(pull = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = tabBarBottomPadding())) {
                // 장르는 위 줄 — 세부 분류·태그(전막 발레·축제 등)만 칩 한 줄로.
                if (state.detailOptions.size > 1) {
                    item(key = "chips.detail") { PerformanceDetailChips(state.detailOptions, state.detail, viewModel::selectDetail) }
                }
                performanceRows(state, viewModel, onOpenDetail)
            }
        }
    }
}
