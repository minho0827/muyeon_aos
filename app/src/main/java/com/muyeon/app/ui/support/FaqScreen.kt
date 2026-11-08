package com.muyeon.app.ui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.UnderlineTabs
import com.muyeon.app.ui.support.components.FaqItem
import com.muyeon.app.ui.theme.AppColors
import org.koin.androidx.compose.koinViewModel

/**
 * FAQ — 웹 /faq(분류 탭 + 질문 접기/펼치기).
 *   빈 화면에는 다음 행동 — 원하는 답이 없으면 1:1 문의([onInquiry]).
 */
@Composable
fun FaqScreen(onBack: () -> Unit, onInquiry: () -> Unit, viewModel: FaqViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val all = stringResource(R.string.common_all)
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.support_faq_title), primary = false, onBack = onBack)
        if (s.categories.isNotEmpty()) {
            UnderlineTabs(listOf(ChipOption(FaqViewModel.ALL, all)) + s.categories.map { ChipOption(it, it) }, s.tab, viewModel::selectTab)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            when {
                s.isLoading -> Unit
                s.errorMessage != null -> item {
                    EmptyView(stringResource(R.string.common_list_failed), content = s.errorMessage,
                        buttonText = stringResource(R.string.common_retry), onButton = viewModel::load)
                }
                s.shown.isEmpty() -> item {
                    EmptyView(stringResource(R.string.support_faq_empty), buttonText = stringResource(R.string.support_inquiry_write), onButton = onInquiry)
                }
                else -> items(s.shown, key = { it.id }) { f -> FaqItem(f, s.openId == f.id) { viewModel.toggle(f.id) } }
            }
        }
    }
}
