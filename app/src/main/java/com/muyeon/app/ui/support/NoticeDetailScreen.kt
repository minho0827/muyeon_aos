package com.muyeon.app.ui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.app.ui.theme.Dimensions

/** 공지 상세 — 웹 NoticeDetail.js(.notice-head: 중요 배지 · 제목 · 날짜 / .notice-body: 본문 줄바꿈 그대로). */
@Composable
fun NoticeDetailScreen(viewModel: NoticeDetailViewModel, onBack: () -> Unit) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.support_notice_detail_title), primary = false, onBack = onBack)
        val n = s.notice
        when {
            s.isLoading -> Unit
            n == null -> EmptyView(stringResource(R.string.support_notice_not_found), content = s.errorMessage,
                buttonText = stringResource(R.string.common_retry), onButton = viewModel::load)
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(Modifier.fillMaxWidth().padding(Dimensions.gutter), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
                    if (n.pinned) LevelBadge(stringResource(R.string.support_notice_important), CategoryColors.Level.High)
                    Text(n.title, style = AppTypography.Title18, color = AppColors.TextHead)
                    Text(KstDates.dot(n.createdAt), style = AppTypography.Sub13, color = AppColors.TextHint)
                }
                HorizontalDivider(thickness = 1.dp, color = AppColors.Divider)
                Text(n.content, style = AppTypography.Body15, color = AppColors.TextBody, modifier = Modifier.padding(Dimensions.gutter))
            }
        }
    }
}
