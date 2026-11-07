package com.muyeon.app.ui.community.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import com.muyeon.app.R
import com.muyeon.app.ui.community.CommunityLabels
import com.muyeon.app.ui.components.Avatar
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.format.timeAgo
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.community.CommunityPost

/**
 * 글 머리 — 웹 CommunityDetail .notice-head: [아바타 44] [노란 카테고리 배지 · 제목] [내 글이면 ⋯],
 *   아래 "작성자 · N분 전 · 조회 N", 그 아래 본문.
 */
@Composable
fun CommunityPostHead(post: CommunityPost, onAuthor: () -> Unit, onMore: (() -> Unit)?, modifier: Modifier = Modifier) {
    val s = post.summary
    Column(modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingXL), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
            Avatar(s.author.image, 44.dp, onClick = if (s.author.isTeacher) onAuthor else null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS)) {
                Row { LevelBadge(stringResource(CommunityLabels.category(s.category)), CategoryColors.Level.Middle) }
                Text(s.title, style = AppTypography.Title18, color = AppColors.TextHead)
            }
            if (onMore != null) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = stringResource(R.string.common_more_menu), tint = AppColors.TextSub, modifier = Modifier.size(Dimensions.iconL).clickable(onClick = onMore))
            }
        }
        Text(
            stringResource(R.string.community_post_info, s.author.name ?: stringResource(R.string.community_anonymous), timeAgo(s.createdAt), post.viewCount),
            style = AppTypography.Sub13, color = AppColors.TextHint,
        )
        Text(post.body, style = AppTypography.Body15, color = AppColors.TextHead, modifier = Modifier.padding(top = Dimensions.spacingS))
    }
}
