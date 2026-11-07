package com.muyeon.app.ui.community.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.community.CommunityLabels
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.components.ListRow
import com.muyeon.app.ui.format.timeAgo
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.community.CommunityPostSummary

/**
 * 커뮤니티 목록 한 줄 — 웹 CommunityHome renderRow: (사진) · 노란 카테고리 배지 · 제목 ·
 *   "작성자 · ♥ N · 댓글 N · N분 전" · 화살표.
 */
@Composable
fun CommunityPostRow(item: CommunityPostSummary, onClick: () -> Unit, modifier: Modifier = Modifier, showDivider: Boolean = true) {
    val author = item.author.name ?: stringResource(R.string.community_anonymous)
    ListRow(
        title = item.title,
        sub = stringResource(R.string.community_row_sub, author, item.likeCount, item.commentCount, timeAgo(item.createdAt)),
        onClick = onClick,
        thumbUrl = item.image,
        tags = { LevelBadge(stringResource(CommunityLabels.category(item.category)), CategoryColors.Level.Middle) },
        showDivider = showDivider,
        modifier = modifier.testTag("community_row_${item.id}"),
    )
}
