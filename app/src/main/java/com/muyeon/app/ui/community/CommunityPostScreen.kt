package com.muyeon.app.ui.community

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.common.ImageViewerActivity
import com.muyeon.app.ui.community.components.CommentCallbacks
import com.muyeon.app.ui.community.components.CommunityCommentThread
import com.muyeon.app.ui.community.components.CommunityPostActions
import com.muyeon.app.ui.community.components.CommunityPostHead
import com.muyeon.app.ui.components.ActionSheet
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.CursorLoadMoreEffect
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.ImageCarousel
import com.muyeon.app.ui.components.InputWithButton
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.ReportSheet
import com.muyeon.app.ui.components.SheetAction
import com.muyeon.app.ui.format.ImageUrls
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import org.json.JSONArray
import org.koin.androidx.compose.koinViewModel

/**
 * 커뮤니티 글 상세 — 흰 위 바 "게시글"(뒤로) · 사진 · 머리·본문 · [좋아요][스크랩] · 댓글 N · 댓글 줄기들 · 댓글 입력 · [신고][차단하기].
 *
 * @param onLogin 로그인 필요(웹 로그인 창 → 돌아오면 이 글)
 * @param onEditPost 글 수정(웹 창)
 * @param onOpenTeacher 강사 작성자 프로필(기존 앱 화면)
 */
@Composable
fun CommunityPostScreen(
    onBack: () -> Unit,
    onLogin: (Long) -> Unit,
    onEditPost: (Long) -> Unit,
    onOpenTeacher: (Long) -> Unit,
    viewModel: CommunityPostViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var reportResult by remember { mutableStateOf<String?>(null) }
    val reportDone = stringResource(R.string.report_done)
    val reportFailed = stringResource(R.string.report_failed)

    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                is CommunityPostEvent.Toast -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
                is CommunityPostEvent.ToastRes -> Toast.makeText(context, e.res, Toast.LENGTH_SHORT).show()
                CommunityPostEvent.LoginRequired -> onLogin(viewModel.postId)
                CommunityPostEvent.Leave -> onBack()
                is CommunityPostEvent.Edit -> onEditPost(e.id)
                is CommunityPostEvent.OpenTeacher -> onOpenTeacher(e.userId)
                is CommunityPostEvent.ReportDone -> reportResult = if (e.ok) reportDone else (e.message ?: reportFailed)
            }
        }
    }
    CursorLoadMoreEffect(listState, canLoad = state.commentCursor.canAutoLoad && !state.commentsLoading, onLoadMore = { viewModel.loadComments() })

    val cb = CommentCallbacks(
        onEditText = viewModel::onEditText,
        onSaveEdit = viewModel::saveEdit,
        onCancelEdit = viewModel::cancelEdit,
        onMore = { viewModel.openCommentSheet(it) },
        onLike = viewModel::likeComment,
        onReply = viewModel::openReply,
        onAuthor = { viewModel.openAuthor(it.author.isTeacher, it.author.id) },
        onReplyText = viewModel::onReplyText,
        onSendReply = viewModel::addReply,
        onToggleReplies = viewModel::toggleReplies,
        onMoreReplies = { viewModel.loadReplies(it) },
    )

    Column(Modifier.fillMaxSize().background(AppColors.Surface).imePadding()) {
        MainTopBar(title = stringResource(R.string.community_post_title), primary = false, onBack = onBack)
        val post = state.post
        when {
            post == null && state.postLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.Primary) }
            post == null -> EmptyView(
                title = stringResource(R.string.community_post_failed),
                buttonText = stringResource(R.string.common_retry),
                onButton = viewModel::reload,
            )
            else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                if (post.images.isNotEmpty()) item(key = "images") {
                    ImageCarousel(
                        images = post.images,
                        onOpen = { i -> ImageViewerActivity.start(context, JSONArray(post.images.map { ImageUrls.resolve(it) }).toString(), i, allowSave = false) },
                    )
                }
                item(key = "head") {
                    CommunityPostHead(
                        post = post,
                        onAuthor = { viewModel.openAuthor(post.summary.author.isTeacher, post.summary.author.id) },
                        onMore = if (post.isMine) ({ viewModel.openPostSheet(true) }) else null,
                    )
                }
                item(key = "actions") {
                    CommunityPostActions(
                        liked = post.likedByMe, likeCount = post.summary.likeCount, scrapped = post.scrappedByMe, busy = state.busy,
                        onLike = viewModel::likePost, onScrap = viewModel::toggleScrap,
                        modifier = Modifier.padding(bottom = Dimensions.spacingXXL),
                    )
                    HorizontalDivider(thickness = 8.dp, color = AppColors.SurfaceMuted)
                }
                item(key = "commentHead") {
                    Text(
                        stringResource(R.string.community_comments_title, post.summary.commentCount),
                        style = AppTypography.Section16, color = AppColors.TextHead,
                        modifier = Modifier.padding(start = Dimensions.gutter, end = Dimensions.gutter, top = Dimensions.spacingXL),
                    )
                }
                if (state.comments.isEmpty()) item(key = "commentEmpty") {
                    Text(
                        stringResource(
                            when {
                                state.commentsLoading -> R.string.community_comments_loading
                                state.commentsError != null -> R.string.community_comments_failed
                                else -> R.string.community_comments_empty
                            },
                        ),
                        style = AppTypography.Body14, color = AppColors.TextHint,
                        modifier = Modifier.padding(Dimensions.gutter),
                    )
                }
                items(state.comments, key = { "c${it.id}" }) { c ->
                    CommunityCommentThread(
                        comment = c, replies = state.replies[c.id], replyTo = state.replyTo, replyText = state.replyText,
                        editId = state.editId, editText = state.editText, busy = state.busy, cb = cb,
                        modifier = Modifier.padding(horizontal = Dimensions.gutter),
                    )
                }
                item(key = "commentFooter") {
                    CursorListFooter(
                        loading = state.commentsLoading && state.comments.isNotEmpty(),
                        error = state.commentsError.takeIf { state.comments.isNotEmpty() },
                        hasMore = state.commentCursor.needsTap || (state.commentsError != null && state.comments.isNotEmpty()),
                        onLoadMore = { viewModel.loadComments() },
                    )
                    InputWithButton(
                        value = state.text, onValueChange = viewModel::onText,
                        placeholder = stringResource(R.string.community_comment_hint), buttonText = stringResource(R.string.community_comment_submit),
                        onButton = viewModel::addComment, enabled = !state.busy,
                        modifier = Modifier.padding(Dimensions.gutter),
                    )
                }
                item(key = "safety") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingL),
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXL, Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(R.string.report_button), style = AppTypography.Sub13SemiBold, color = AppColors.TextSub,
                            modifier = Modifier.clickable { viewModel.openReport(ReportTarget("POST", post.summary.id)) })
                        val authorId = post.summary.author.id
                        if (!post.isMine && authorId != null && !post.isAnonymous) {
                            Text(stringResource(R.string.block_button), style = AppTypography.Sub13SemiBold, color = AppColors.TextSub,
                                modifier = Modifier.clickable { viewModel.openBlock(BlockTarget(authorId, post.summary.author.name, leaveAfter = true)) })
                        }
                    }
                    Spacer(Modifier.navigationBarsPadding().height(Dimensions.spacing3XL))
                }
            }
        }
    }

    // ── 시트·확인창(웹 modal-bottom·ConfirmModal 과 같은 구성) ──
    if (state.postSheet) ActionSheet(
        title = stringResource(R.string.community_my_post),
        actions = listOf(
            SheetAction(stringResource(R.string.common_edit), true, viewModel::editPost),
            SheetAction(stringResource(R.string.common_delete), false) { viewModel.askDeletePost(true) },
        ),
        onDismiss = { viewModel.openPostSheet(false) },
    )
    state.myCommentSheet?.let { c ->
        ActionSheet(
            title = stringResource(R.string.community_my_comment),
            actions = listOf(
                SheetAction(stringResource(R.string.common_edit), true) { viewModel.startEdit(c) },
                SheetAction(stringResource(R.string.common_delete), false) { viewModel.askDeleteComment(c) },
            ),
            onDismiss = { viewModel.openCommentSheet(null) },
        )
    }
    state.otherCommentSheet?.let { c ->
        val authorId = c.author.id
        ActionSheet(
            title = stringResource(R.string.community_comment),
            actions = listOfNotNull(
                SheetAction(stringResource(R.string.report_submit), false) { viewModel.openReport(ReportTarget("COMMENT", c.id)) },
                if (authorId != null && !c.isAnonymous) SheetAction(stringResource(R.string.block_button), true) {
                    viewModel.openBlock(BlockTarget(authorId, c.author.name, leaveAfter = false))
                } else null,
            ),
            onDismiss = { viewModel.openCommentSheet(null) },
        )
    }
    if (state.report != null) ReportSheet(busy = state.busy, onSubmit = viewModel::submitReport, onDismiss = { viewModel.openReport(null) })

    ConfirmDialog(
        show = state.confirmDeleteComment != null, title = stringResource(R.string.community_delete_comment_q),
        confirm = stringResource(R.string.common_delete), onConfirm = viewModel::confirmDeleteComment, onDismiss = viewModel::dismissDeleteComment,
    )
    ConfirmDialog(
        show = state.confirmDeletePost, title = stringResource(R.string.community_delete_post_q),
        confirm = stringResource(R.string.common_delete), onConfirm = viewModel::deletePost, onDismiss = { viewModel.askDeletePost(false) },
    )
    val block = state.block
    ConfirmDialog(
        show = block != null,
        title = stringResource(R.string.block_confirm_title, block?.name ?: stringResource(R.string.block_confirm_someone)),
        body = stringResource(R.string.block_confirm_body),
        confirm = stringResource(R.string.block_button), onConfirm = viewModel::confirmBlock, onDismiss = { viewModel.openBlock(null) },
    )
    CustomDialog(
        title = reportResult.orEmpty(), content = null, leftButtonText = null, rightButtonText = stringResource(R.string.common_confirm),
        buttonCount = 1, alignment = ContentAlignment.Middle, onDismiss = { reportResult = null },
        onLeftButtonClick = {}, onRightButtonClick = { reportResult = null }, showPopup = reportResult != null,
    )
}

/** 무용연 확인 팝업(CustomDialog 두 버튼). */
@Composable
private fun ConfirmDialog(show: Boolean, title: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, body: String? = null) {
    CustomDialog(
        title = title, content = body, leftButtonText = stringResource(R.string.common_cancel), rightButtonText = confirm,
        buttonCount = 2, alignment = ContentAlignment.Middle, onDismiss = onDismiss,
        onLeftButtonClick = onDismiss, onRightButtonClick = onConfirm, showPopup = show,
    )
}
