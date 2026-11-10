package com.muyeon.app.ui.performance.components


import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.muyeon.app.R
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.CursorLoadMoreEffect
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.main.MainTab
import com.muyeon.app.ui.main.TabReselectEffect
import com.muyeon.app.ui.performance.PerformanceListEvent
import com.muyeon.app.ui.performance.PerformanceListUiState
import com.muyeon.app.ui.performance.PerformanceListViewModel
import com.muyeon.app.ui.theme.Dimensions

/**
 * 공연 목록 공용 부품 — 예전 공연 화면(PerformanceListScreen)과 통합 탭(PerformanceHomeScreen)이 같이 쓴다.
 *   화면 복귀·탭 다시 누르기·찜 알림·무한 스크롤.
 */
@Composable
fun PerformanceListEffects(viewModel: PerformanceListViewModel, listState: LazyListState, canLoadMore: Boolean, onLogin: () -> Unit) {
    val context = LocalContext.current
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
    CursorLoadMoreEffect(listState, canLoad = canLoadMore, onLoadMore = viewModel::loadMore)
}

/** 목록 본문 — 빈 목록 안내 · 한 줄씩 · 끝(더 보기). */
fun LazyListScope.performanceRows(
    state: PerformanceListUiState,
    viewModel: PerformanceListViewModel,
    onOpenDetail: (Long) -> Unit,
) {
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
