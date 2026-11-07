package com.muyeon.app.ui.community.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.community.RepliesState
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.InputWithButton
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.community.CommunityComment

/** 댓글 한 줄기에서 화면이 부르는 일 묶음(파라미터가 길어지지 않게). */
class CommentCallbacks(
    val onEditText: (String) -> Unit,
    val onSaveEdit: (CommunityComment) -> Unit,
    val onCancelEdit: () -> Unit,
    val onMore: (CommunityComment) -> Unit,
    val onLike: (CommunityComment) -> Unit,
    val onReply: (CommunityComment, Boolean) -> Unit,
    val onAuthor: (CommunityComment) -> Unit,
    val onReplyText: (String) -> Unit,
    val onSendReply: (Long) -> Unit,
    val onToggleReplies: (Long) -> Unit,
    val onMoreReplies: (Long) -> Unit,
)

/**
 * 댓글 한 줄기 — 웹 li.thread: 댓글 · (답글 입력) · [답글 N개 보기/접기] · 답글들(들여쓰기) · 답글 더 보기.
 */
@Composable
fun CommunityCommentThread(
    comment: CommunityComment,
    replies: RepliesState?,
    replyTo: Long?,
    replyText: String,
    editId: Long?,
    editText: String,
    busy: Boolean,
    cb: CommentCallbacks,
    modifier: Modifier = Modifier,
) {
    @Composable
    fun item(c: CommunityComment, isReply: Boolean) {
        CommunityCommentItem(
            comment = c, isReply = isReply, editing = editId == c.id, editText = editText, busy = busy,
            onEditText = cb.onEditText, onSaveEdit = { cb.onSaveEdit(c) }, onCancelEdit = cb.onCancelEdit,
            onMore = { cb.onMore(c) }, onLike = { cb.onLike(c) }, onReply = { cb.onReply(c, isReply) }, onAuthor = { cb.onAuthor(c) },
        )
        if (replyTo == c.id) {
            InputWithButton(
                value = replyText, onValueChange = cb.onReplyText,
                placeholder = stringResource(R.string.community_reply_hint), buttonText = stringResource(R.string.community_reply_submit),
                onButton = { cb.onSendReply(c.id) }, enabled = !busy, modifier = Modifier.padding(top = Dimensions.spacingS),
            )
        }
    }

    Column(modifier.fillMaxWidth().padding(vertical = Dimensions.spacingL), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
        item(comment, isReply = false)
        if (comment.replyCount > 0) {
            val open = replies?.open == true
            Text(
                if (open) stringResource(R.string.community_replies_hide) else stringResource(R.string.community_replies_show, comment.replyCount),
                style = AppTypography.Sub13SemiBold, color = AppColors.Primary,
                modifier = Modifier.clickable { cb.onToggleReplies(comment.id) },
            )
            if (open && replies != null) {
                Column(Modifier.padding(start = REPLY_INDENT), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
                    replies.items.forEach { r -> item(r, isReply = true) }
                    CursorListFooter(loading = replies.loading, error = null, hasMore = replies.cursor.hasMore, onLoadMore = { cb.onMoreReplies(comment.id) })
                }
            }
        }
    }
    HorizontalDivider(thickness = 1.dp, color = AppColors.Divider)
}

private val REPLY_INDENT = 32.dp
