package com.muyeon.app.ui.policy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.policy.components.PolicyBlocks
import com.muyeon.app.ui.policy.components.PolicyHistory
import com.muyeon.app.ui.policy.components.PolicyNotice
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.Dimensions

/**
 * ★ 2026-11-08 (b4) 약관·정책 전문 — 웹 /policy?doc=&version=(pages/setting/Policy.js) 그대로.
 *   위에서부터: (다른 버전이면) '…시행 버전입니다. 현재 버전 보기' · (현재면) '…부터 개정된 내용이 적용됩니다. 개정 내용 보기' · 본문 · 이전 버전 목록.
 *   버전을 오가면 같은 화면을 새로 쌓는다(웹 Link 와 같다 — 뒤로 가면 보던 버전).
 *
 * @param onOpenVersion 다른 버전(null = 현재 버전)으로
 */
@Composable
fun PolicyScreen(
    version: String?,
    onBack: () -> Unit,
    onOpenVersion: (String?) -> Unit,
    viewModel: PolicyViewModel,
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.policy_title), primary = false, onBack = onBack)
        val v = s.view
        when {
            s.isLoading -> Unit
            s.failed || v == null -> EmptyView(stringResource(R.string.policy_load_failed), buttonText = stringResource(R.string.common_retry), onButton = viewModel::load)
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingXL),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM),
            ) {
                PolicyNotice(view = v, versionRequested = !version.isNullOrBlank(), onOpenVersion = onOpenVersion)
                SelectionContainer { PolicyBlocks(s.blocks) }
                if (version.isNullOrBlank()) PolicyHistory(v.current?.history.orEmpty(), onOpenVersion)
            }
        }
    }
}
