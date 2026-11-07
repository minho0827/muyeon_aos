package com.muyeon.app.ui.community.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.Avatar
import com.muyeon.app.ui.components.InputWithButton
import com.muyeon.app.ui.format.timeAgo
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.community.CommunityComment

/**
 * 댓글·답글 한 개 — 웹 CommunityDetail renderComment(.comment-box):
 *   [아바타 28(답글 24)] 이름 · 시간 · ⋯ / 본문(삭제면 회색) / [♥ N] [답글].  수정 중이면 본문 자리에 입력칸.
 */
@Composable
fun CommunityCommentItem(
    comment: CommunityComment,
    isReply: Boolean,
    editing: Boolean,
    editText: String,
    busy: Boolean,
    onEditText: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onMore: () -> Unit,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onAuthor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = comment
    Column(modifier.fillMaxWidth().testTag("comment_${c.id}"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!c.deleted) Avatar(c.author.image, if (isReply) 24.dp else 28.dp, onClick = if (c.author.isTeacher) onAuthor else null)
            Text(
                if (c.deleted) stringResource(R.string.community_comment_deleted_name) else c.author.name ?: stringResource(R.string.community_anonymous),
                style = AppTypography.Body14SemiBold, color = AppColors.TextHead,
            )
            if (!c.deleted) Text(timeAgo(c.createdAt), style = AppTypography.Caption12, color = AppColors.TextHint)
            Spacer(Modifier.weight(1f))
            if (!c.deleted && !editing) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = stringResource(R.string.common_more_menu), tint = AppColors.TextHint, modifier = Modifier.size(20.dp).clickable(onClick = onMore))
            }
        }
        if (editing) {
            InputWithButton(value = editText, onValueChange = onEditText, placeholder = "", buttonText = stringResource(R.string.common_save), onButton = onSaveEdit, enabled = !busy)
            Text(stringResource(R.string.common_cancel), style = AppTypography.Sub13SemiBold, color = AppColors.TextSub, modifier = Modifier.clickable(onClick = onCancelEdit))
        } else {
            Text(c.body, style = AppTypography.Body14, color = if (c.deleted) AppColors.TextHint else AppColors.TextBody)
        }
        if (!c.deleted && !editing) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingL)) {
                Text(
                    stringResource(R.string.community_comment_like, c.likeCount),
                    style = AppTypography.Sub13SemiBold,
                    color = if (c.likedByMe) AppColors.Primary else AppColors.TextSub,
                    modifier = Modifier.clickable(enabled = !busy, onClick = onLike),
                )
                Text(stringResource(R.string.community_reply), style = AppTypography.Sub13SemiBold, color = AppColors.TextSub, modifier = Modifier.clickable(onClick = onReply))
            }
        }
    }
}
