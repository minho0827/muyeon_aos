package com.muyeon.app.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.community.components.CommunityPostRow
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.ChipRow
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.CursorLoadMoreEffect
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.TopBarIconButton
import com.muyeon.app.ui.main.MainTab
import com.muyeon.app.ui.main.TabReselectEffect
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.Dimensions
import org.koin.androidx.compose.koinViewModel

/**
 * 커뮤니티 탭 — 주황 위 바 "커뮤니티" + 오른쪽 글쓰기(웹 meta.action edit) · 카테고리 칩 · 목록.
 *
 * @param onWrite 글쓰기(웹 창 — 비회원이면 부르는 쪽이 로그인으로)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityListScreen(
    onOpenPost: (Long) -> Unit,
    onWrite: () -> Unit,
    /** 커뮤니티 탭 안 머리(위 바 + 커뮤니티·공연·캐스팅 탭). 주면 이 화면 위 바 대신 그린다. */
    header: (@Composable () -> Unit)? = null,
    viewModel: CommunityListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    TabReselectEffect(MainTab.COMMUNITY) { viewModel.onTabReselect() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e -> if (e is CommunityListEvent.ScrollToTop) listState.animateScrollToItem(0) }
    }
    CursorLoadMoreEffect(listState, canLoad = state.cursor.canAutoLoad && !state.isLoadingMore, onLoadMore = viewModel::loadMore)

    val all = stringResource(R.string.common_all)
    val chipOptions = viewModel.categories.map { c -> ChipOption(c, if (c == CommunityListViewModel.ALL) all else stringResource(CommunityLabels.category(c))) }

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        if (header != null) header() else MainTopBar(title = stringResource(R.string.tab_community)) {
            TopBarIconButton(R.drawable.ic_web_edit, stringResource(R.string.community_write), onClick = onWrite)
        }
        PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = { viewModel.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = com.muyeon.app.ui.main.tabBarBottomPadding())) {
                item(key = "chips") {
                    ChipRow(chipOptions, state.category, viewModel::select, Modifier.fillMaxWidth().padding(vertical = Dimensions.spacingL))
                }
                when {
                    state.items.isEmpty() && state.isInitialLoading -> Unit
                    state.items.isEmpty() -> item(key = "empty") {
                        EmptyView(
                            title = stringResource(if (state.errorMessage != null) R.string.common_list_failed else R.string.community_list_empty),
                            buttonText = state.errorMessage?.let { stringResource(R.string.common_retry) },
                            onButton = { viewModel.refresh() },
                        )
                    }
                    else -> itemsIndexed(state.items, key = { _, p -> p.id }) { i, p ->
                        CommunityPostRow(p, onClick = { onOpenPost(p.id) }, showDivider = i != state.items.lastIndex)
                    }
                }
                item(key = "footer") {
                    CursorListFooter(
                        loading = state.isLoadingMore,
                        error = state.moreError,
                        hasMore = state.cursor.needsTap || state.moreError != null,
                        onLoadMore = viewModel::loadMore,
                        modifier = Modifier.padding(bottom = Dimensions.spacingXXL),
                    )
                }
            }
        }
    }
}
