package com.muyeon.app.ui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.support.components.NoticeRow
import com.muyeon.app.ui.theme.AppColors
import org.koin.androidx.compose.koinViewModel

/** 공지사항 — 웹 /notice(흰 위 바 · 목록 · 누르면 상세). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeListScreen(onBack: () -> Unit, onOpen: (Long) -> Unit, viewModel: NoticeListViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.support_notice_title), primary = false, onBack = onBack)
        PullToRefreshBox(s.isRefreshing, onRefresh = { viewModel.load(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize()) {
                when {
                    s.isLoading -> Unit
                    s.errorMessage != null && s.items.isEmpty() -> item {
                        EmptyView(stringResource(R.string.common_list_failed), content = s.errorMessage,
                            buttonText = stringResource(R.string.common_retry), onButton = { viewModel.load() })
                    }
                    s.items.isEmpty() -> item { EmptyView(stringResource(R.string.support_notice_empty)) }
                    else -> itemsIndexed(s.items, key = { _, n -> n.id }) { i, n ->
                        NoticeRow(n, onClick = { onOpen(n.id) }, showDivider = i < s.items.lastIndex)
                    }
                }
            }
        }
    }
}
