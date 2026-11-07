package com.muyeon.app.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 끝에서 [threshold] 칸 안쪽이 보이면 다음 페이지 — 웹 IntersectionObserver(rootMargin 300px)와 같은 역할.
 *   [canLoad] 가 false(불러오는 중·실패·끝)면 부르지 않는다. PaceERA CursorLoadMoreEffect 와 같은 자리.
 */
@Composable
fun CursorLoadMoreEffect(state: LazyListState, canLoad: Boolean, threshold: Int = 4, onLoadMore: () -> Unit) {
    val latest by rememberUpdatedState(onLoadMore)
    val can by rememberUpdatedState(canLoad)
    val nearEnd by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 1 - threshold
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { nearEnd }.distinctUntilChanged().filter { it }.collect { if (can) latest() }
    }
    LaunchedEffect(canLoad) { if (canLoad && nearEnd) latest() }
}

@Composable
fun CursorLoadMoreEffect(state: LazyGridState, canLoad: Boolean, threshold: Int = 4, onLoadMore: () -> Unit) {
    val latest by rememberUpdatedState(onLoadMore)
    val can by rememberUpdatedState(canLoad)
    val nearEnd by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 1 - threshold
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { nearEnd }.distinctUntilChanged().filter { it }.collect { if (can) latest() }
    }
    LaunchedEffect(canLoad) { if (canLoad && nearEnd) latest() }
}
