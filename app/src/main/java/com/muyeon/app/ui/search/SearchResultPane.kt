package com.muyeon.app.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muyeon.app.R
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.CursorLoadMoreEffect
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 지금 열린 시트 — 섹션 몇 개(null = 전체 필터) 또는 정렬. */
private sealed interface PaneSheet {
    data class Filter(val keys: List<String>?) : PaneSheet
    data object Sort : PaneSheet
}

/**
 * 한 도메인 검색 결과 — 칩 줄 + 결과 목록 + 시트. 상태는 도메인마다 ViewModel 하나(탭을 오가도 지킨다).
 *   도메인이 무엇이든 같은 코드 — 칩·정렬·한 줄 모양은 [domain] 이 정한다.
 */
@Composable
fun <T : Any> SearchResultPane(domain: SearchDomain<T>, query: String, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val vm: SearchViewModel<T> = viewModel(key = "search.${domain.key}") { SearchViewModel(domain) }
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var sheet by remember { mutableStateOf<PaneSheet?>(null) }

    LaunchedEffect(query) { vm.setQuery(query) }
    // 조건이 바뀌면 맨 위부터.
    LaunchedEffect(state.query, state.filter, state.sort) { listState.scrollToItem(0) }
    CursorLoadMoreEffect(listState, canLoad = state.cursor.canAutoLoad && !state.isLoadingMore && !state.isLoading, onLoadMore = vm::loadMore)

    Column(modifier.fillMaxSize()) {
        FilterChipBar(
            domain = domain,
            filter = state.filter,
            sort = state.sort,
            onSection = { sheet = PaneSheet.Filter(listOf(it)) },
            onAll = { sheet = PaneSheet.Filter(null) },
            onSort = { sheet = PaneSheet.Sort },
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Dimensions.spacingXXL),
        ) {
            when {
                state.items.isEmpty() && state.isLoading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), Alignment.Center) { CircularProgressIndicator(color = AppColors.Primary) }
                }
                state.items.isEmpty() -> item(key = "empty") {
                    if (state.error != null) {
                        EmptyView(stringResource(R.string.search_failed), content = state.error, buttonText = stringResource(R.string.common_retry), onButton = vm::refresh)
                    } else {
                        EmptyView(stringResource(R.string.search_empty), content = stringResource(R.string.search_empty_sub))
                    }
                }
                else -> {
                    state.total?.let { n ->
                        item(key = "total") {
                            Text(
                                stringResource(R.string.search_total, n), style = AppTypography.Sub13SemiBold, color = AppColors.TextSub,
                                modifier = Modifier.padding(horizontal = Dimensions.gutter, vertical = 4.dp),
                            )
                        }
                    }
                    items(state.items, key = { "${domain.key}.${domain.idOf(it)}" }) { item ->
                        domain.Row(item) { onOpen(domain.open(item)) }
                    }
                }
            }
            item(key = "footer") {
                CursorListFooter(
                    loading = state.isLoadingMore,
                    error = state.moreError,
                    hasMore = state.cursor.needsTap || state.moreError != null,
                    onLoadMore = vm::loadMore,
                )
            }
        }
    }

    when (val s = sheet) {
        null -> Unit
        is PaneSheet.Filter -> FilterSheet(
            titleRes = s.keys?.singleOrNull()?.let { domain.section(it)?.titleRes } ?: R.string.search_filter,
            sections = s.keys?.mapNotNull { domain.section(it) } ?: domain.filterSections,
            initial = state.filter,
            count = state.draftCount,
            showCountRes = domain.showCountRes,
            onDraft = vm::requestCount,
            onApply = { f -> vm.applyFilter(f); sheet = null },
            onDismiss = { sheet = null },
        )
        PaneSheet.Sort -> SortSheet(
            options = domain.sortOptions,
            selected = state.sort,
            onSelect = { k -> vm.selectSort(k); sheet = null },
            onDismiss = { sheet = null },
        )
    }
}
