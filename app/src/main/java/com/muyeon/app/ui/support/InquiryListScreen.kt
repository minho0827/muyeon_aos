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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.common.MuyeonBottomButton
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.support.components.InquiryRow
import com.muyeon.app.ui.theme.AppColors

/**
 * 1:1 문의내역 — 웹 /support/inquiries. 문의가 있으면 목록 + 아래 [문의하기], 없으면 빈 화면에 [문의하기].
 *   쓰고 돌아오거나 상세에서 지우고 돌아오면 다시 읽는다(화면이 다시 보일 때).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InquiryListScreen(viewModel: InquiryListViewModel, onBack: () -> Unit, onOpen: (Long) -> Unit, onWrite: () -> Unit) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    // 처음 한 번은 ViewModel 이 읽었다 — 그 뒤 다시 보일 때(쓰기·상세에서 돌아옴)만 다시 읽는다.
    val seen = androidx.compose.runtime.remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    LifecycleResumeEffect(Unit) {
        if (seen.getAndSet(true)) viewModel.refresh()
        onPauseOrDispose { }
    }
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.support_inquiry_title), primary = false, onBack = onBack)
        PullToRefreshBox(s.isRefreshing, onRefresh = { viewModel.refresh(pull = true) }, modifier = Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize()) {
                when {
                    s.isLoading -> Unit
                    s.errorMessage != null && s.items.isEmpty() -> item {
                        EmptyView(stringResource(R.string.common_list_failed), content = s.errorMessage,
                            buttonText = stringResource(R.string.common_retry), onButton = { viewModel.refresh() })
                    }
                    s.items.isEmpty() -> item {
                        EmptyView(stringResource(R.string.support_inquiry_empty), content = stringResource(R.string.support_inquiry_empty_desc),
                            buttonText = stringResource(R.string.support_inquiry_write), onButton = onWrite)
                    }
                    else -> itemsIndexed(s.items, key = { _, i -> i.id }) { idx, i ->
                        InquiryRow(i, onClick = { onOpen(i.id) }, showDivider = idx < s.items.lastIndex)
                    }
                }
            }
        }
        if (s.items.isNotEmpty()) MuyeonBottomButton(stringResource(R.string.support_inquiry_write), "inquiry.write", onClick = onWrite)
    }
}
