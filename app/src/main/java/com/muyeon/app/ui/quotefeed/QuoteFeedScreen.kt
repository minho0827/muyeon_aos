package com.muyeon.app.ui.quotefeed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.ChipRow
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.CursorLoadMoreEffect
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.GuideBanner
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.ViewModeToggle
import com.muyeon.app.ui.quotefeed.components.QuoteFeedCard
import com.muyeon.app.ui.quotefeed.components.QuoteFeedRow
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.Dimensions

/**
 * 홈 '레슨을 찾는 회원들' 전체보기 — 레슨 전체보기(LessonListScreen)와 같은 틀(주황 위 바 · 장르 칩 · 그리드/목록 전환).
 *   위 안내(웹 AvailableQuotes GuideBanner) · 2열 카드(홈 카드 그대로) 또는 한 줄 목록. 누르면 견적 응답 화면(/availableQuotes/:id).
 *
 * @param onOpenPath 웹 주소로 열기(앱 화면이 있으면 앱 — NativeRoutes)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteFeedScreen(
    viewModel: QuoteFeedViewModel,
    onBack: () -> Unit,
    onOpenPath: (String) -> Unit,
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val grid = rememberLazyGridState()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    CursorLoadMoreEffect(grid, canLoad = s.hasMore && s.moreError == null && !s.isLoadingMore, onLoadMore = viewModel::loadMore)
    val all = stringResource(R.string.common_all)
    val chips = viewModel.categoryOptions.map { id -> ChipOption(id, if (id == QuoteFeedViewModel.ALL) all else QuoteFeedLabels.genre(id) ?: id) }
    val open: (com.muyeon.domain.model.quotefeed.QuoteFeedItem) -> Unit = { onOpenPath(QuoteFeedLabels.path(it, viewModel.sent(it))) }

    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.quote_feed_title), onBack = onBack)
        PullToRefreshBox(s.isRefreshing, onRefresh = { viewModel.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                GridCells.Fixed(if (s.grid) 2 else 1), Modifier.fillMaxSize(), grid,
                contentPadding = PaddingValues(bottom = Dimensions.spacing3XL),
            ) {
                item(key = "guide", span = { GridItemSpan(maxLineSpan) }) {
                    GuideBanner(
                        stringResource(if (s.canBrowseAll) R.string.quote_feed_list_guide_pro else R.string.quote_feed_list_guide_member),
                        Modifier.padding(start = Dimensions.gutter, end = Dimensions.gutter, top = Dimensions.spacingM),
                    )
                }
                item(key = "chips", span = { GridItemSpan(maxLineSpan) }) {
                    ChipRow(chips, s.categoryId, viewModel::selectCategory, Modifier.padding(top = Dimensions.spacingM))
                }
                item(key = "toggle", span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingM), horizontalArrangement = Arrangement.End) {
                        ViewModeToggle(s.grid, viewModel::setGrid)
                    }
                }
                when {
                    s.items.isEmpty() && s.isInitialLoading -> Unit
                    s.items.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        EmptyView(
                            when {
                                s.errorMessage != null -> stringResource(R.string.common_list_failed)
                                s.categoryId == QuoteFeedViewModel.ALL -> stringResource(R.string.quote_feed_list_empty)
                                else -> stringResource(R.string.quote_feed_list_genre_empty, QuoteFeedLabels.genre(s.categoryId) ?: s.categoryId)
                            },
                            buttonText = s.errorMessage?.let { stringResource(R.string.common_retry) },
                            onButton = { viewModel.refresh() },
                        )
                    }
                    s.grid -> itemsIndexed(s.items, key = { _, q -> q.id }) { i, q ->
                        // 2열 — 바깥 20 · 가운데 10(레슨 전체보기와 같은 간격).
                        val left = i % 2 == 0
                        QuoteFeedCard(
                            q, viewModel.sent(q), onClick = { open(q) },
                            modifier = Modifier.padding(start = if (left) 20.dp else 5.dp, end = if (left) 5.dp else 20.dp, bottom = 22.dp),
                        )
                    }
                    else -> itemsIndexed(s.items, key = { _, q -> q.id }) { _, q ->
                        QuoteFeedRow(q, viewModel.sent(q), onClick = { open(q) })
                    }
                }
                item(key = "more", span = { GridItemSpan(maxLineSpan) }) {
                    CursorListFooter(s.isLoadingMore, s.moreError, s.moreError != null, viewModel::loadMore)
                }
                item(key = "end", span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.padding(bottom = Dimensions.spacingL)) }
            }
        }
    }
}
