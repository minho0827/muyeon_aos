package com.muyeon.app.ui.community

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.navigation.AppResults
import com.muyeon.app.navigation.Screen
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorState
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.common.fetchCursorPages
import com.muyeon.domain.common.mergeCursorPage
import com.muyeon.domain.model.community.CommunityComment
import com.muyeon.domain.model.community.CommunityPost
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.usecase.community.DeleteCommunityPostUseCase
import com.muyeon.domain.usecase.community.EditCommunityCommentUseCase
import com.muyeon.domain.usecase.community.GetCommunityCommentsUseCase
import com.muyeon.domain.usecase.community.GetCommunityPostUseCase
import com.muyeon.domain.usecase.community.LikeCommunityUseCase
import com.muyeon.domain.usecase.safety.BlockUserUseCase
import com.muyeon.domain.usecase.safety.ReportUseCase
import com.muyeon.domain.usecase.scrap.ToggleScrapResult
import com.muyeon.domain.usecase.scrap.ToggleScrapUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 답글 묶음(댓글 하나 아래) — 웹 PagedReplies. 펼칠 때 처음 받는다. */
data class RepliesState(
    val open: Boolean = false,
    val items: List<CommunityComment> = emptyList(),
    val cursor: CursorState = CursorState.Initial,
    val loading: Boolean = false,
)

/** 신고 대상 — 글(POST) 또는 댓글(COMMENT). */
data class ReportTarget(val type: String, val id: Long)

/** 차단 대상. 글 작성자를 막으면 목록으로 돌아간다([leaveAfter]). */
data class BlockTarget(val userId: Long, val name: String?, val leaveAfter: Boolean)

data class CommunityPostUiState(
    val post: CommunityPost? = null,
    val postLoading: Boolean = true,
    val postError: Boolean = false,
    val comments: List<CommunityComment> = emptyList(),
    val commentCursor: CursorState = CursorState.Initial,
    val commentsLoading: Boolean = false,
    val commentsError: String? = null,
    val replies: Map<Long, RepliesState> = emptyMap(),
    /** 새 댓글 입력. */
    val text: String = "",
    /** 답글을 다는 댓글(답글의 답글이면 그 답글) id. */
    val replyTo: Long? = null,
    val replyText: String = "",
    val editId: Long? = null,
    val editText: String = "",
    val busy: Boolean = false,
    // 시트·확인창
    val postSheet: Boolean = false,
    val myCommentSheet: CommunityComment? = null,
    val otherCommentSheet: CommunityComment? = null,
    val confirmDeleteComment: Long? = null,
    val confirmDeletePost: Boolean = false,
    val report: ReportTarget? = null,
    val block: BlockTarget? = null,
)

sealed interface CommunityPostEvent {
    data class Toast(val text: String) : CommunityPostEvent
    data class ToastRes(val res: Int) : CommunityPostEvent
    data object LoginRequired : CommunityPostEvent
    /** 글이 없어짐·지움·작성자 차단 — 목록으로. */
    data object Leave : CommunityPostEvent
    /** 글 수정(웹 창). */
    data class Edit(val id: Long) : CommunityPostEvent
    /** 강사 작성자 → 강사 프로필. */
    data class OpenTeacher(val userId: Long) : CommunityPostEvent
    data class ReportDone(val ok: Boolean, val message: String?) : CommunityPostEvent
}

/**
 * 커뮤니티 글 상세 — 웹 pages/muyeon/community/CommunityDetail.js.
 *   열람은 공개. 댓글·답글·수정·삭제·좋아요·스크랩·신고·차단은 로그인(비회원이면 [CommunityPostEvent.LoginRequired]).
 *   댓글을 쓰고·고치고·지우면 웹처럼 글과 댓글을 다시 받는다(좋아요 순 정렬·답글 수가 서버 기준으로 맞게).
 */
class CommunityPostViewModel(
    savedState: SavedStateHandle,
    private val getPost: GetCommunityPostUseCase,
    private val getComments: GetCommunityCommentsUseCase,
    private val editComment: EditCommunityCommentUseCase,
    private val like: LikeCommunityUseCase,
    private val deletePost: DeleteCommunityPostUseCase,
    private val toggleScrap: ToggleScrapUseCase,
    private val report: ReportUseCase,
    private val blockUser: BlockUserUseCase,
    private val tokens: TokenStore,
) : ViewModel() {
    val postId: Long = savedState.get<String>(Screen.CommunityPost.ARG_ID)?.toLongOrNull() ?: 0L

    private val _state = MutableStateFlow(CommunityPostUiState())
    val state: StateFlow<CommunityPostUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CommunityPostEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<CommunityPostEvent> = _events.asSharedFlow()

    private var wasLoggedIn = tokens.isLoggedIn

    init { reload() }

    private var resumedOnce = false

    /** 다른 창(웹 로그인·글 수정)에서 돌아왔다 — 내 좋아요·내 댓글 표시·고친 글을 다시 받는다. */
    fun onResume() {
        if (!resumedOnce) { resumedOnce = true; return }
        wasLoggedIn = tokens.isLoggedIn
        reload()
    }

    fun reload() {
        _state.update { it.copy(postLoading = it.post == null, postError = false) }
        viewModelScope.launch {
            when (val r = getPost(postId)) {
                is ApiResult.Success -> _state.update { it.copy(post = r.data, postLoading = false) }
                is ApiResult.Error -> {
                    if (r.statusCode == 404) { _events.tryEmit(CommunityPostEvent.Leave); return@launch }
                    _state.update { it.copy(postLoading = false, postError = it.post == null) }
                    if (_state.value.post == null) return@launch
                }
            }
            loadComments(reset = true)
            // 펼쳐 둔 답글도 다시(웹은 버전을 올려 전부 다시 받는다).
            _state.value.replies.filterValues { it.open }.keys.forEach { loadReplies(it, reset = true) }
        }
    }

    // ── 댓글 목록 ──
    fun loadComments(reset: Boolean = false) {
        val s = _state.value
        if (s.commentsLoading || (!reset && !s.commentCursor.hasMore)) return
        _state.update { it.copy(commentsLoading = true, commentsError = null, commentCursor = if (reset) CursorState.Initial else it.commentCursor.retrying()) }
        viewModelScope.launch {
            val start = if (reset) null else s.commentCursor.nextCursor
            val existing = if (reset) emptySet() else s.comments.mapTo(HashSet()) { it.id }
            val fetch = fetchCursorPages(start, existing, { it.id }) { c -> getComments(postId, null, c) }
            _state.update {
                it.copy(
                    comments = if (reset && fetch.pages > 0) fetch.items else mergeCursorPage(it.comments, fetch.items, replace = false) { c -> c.id },
                    commentCursor = (if (reset) CursorState.Initial else it.commentCursor).applied(fetch),
                    commentsLoading = false,
                    commentsError = fetch.error,
                )
            }
        }
    }

    fun toggleReplies(parentId: Long) {
        val cur = _state.value.replies[parentId] ?: RepliesState()
        val open = !cur.open
        _state.update { it.copy(replies = it.replies + (parentId to cur.copy(open = open))) }
        if (open && !cur.cursor.loaded) loadReplies(parentId, reset = true)
    }

    fun loadReplies(parentId: Long, reset: Boolean = false) {
        val cur = _state.value.replies[parentId] ?: RepliesState(open = true)
        if (cur.loading || (!reset && !cur.cursor.hasMore)) return
        _state.update { it.copy(replies = it.replies + (parentId to cur.copy(loading = true))) }
        viewModelScope.launch {
            val start = if (reset) null else cur.cursor.nextCursor
            val existing = if (reset) emptySet() else cur.items.mapTo(HashSet()) { it.id }
            val fetch = fetchCursorPages(start, existing, { it.id }) { c -> getComments(postId, parentId, c) }
            _state.update {
                val r = it.replies[parentId] ?: cur
                it.copy(
                    replies = it.replies + (parentId to r.copy(
                        items = if (reset && fetch.pages > 0) fetch.items else mergeCursorPage(r.items, fetch.items, replace = false) { c -> c.id },
                        cursor = (if (reset) CursorState.Initial else r.cursor).applied(fetch),
                        loading = false,
                    )),
                )
            }
        }
    }

    // ── 입력 ──
    fun onText(v: String) = _state.update { it.copy(text = v) }
    fun onReplyText(v: String) = _state.update { it.copy(replyText = v) }
    fun onEditText(v: String) = _state.update { it.copy(editText = v) }

    /** 답글 열기/닫기 — 답글의 답글이면 "@이름 " 을 미리 넣는다(웹 openReply). */
    fun openReply(c: CommunityComment, isReply: Boolean) {
        if (_state.value.replyTo == c.id) { _state.update { it.copy(replyTo = null, replyText = "") }; return }
        _state.update { it.copy(replyTo = c.id, replyText = if (isReply && c.author.name != null) "@${c.author.name} " else "") }
    }

    fun startEdit(c: CommunityComment) = _state.update { it.copy(editId = c.id, editText = c.body, myCommentSheet = null) }
    fun cancelEdit() = _state.update { it.copy(editId = null, editText = "") }

    private inline fun requireLogin(block: () -> Unit) {
        if (!tokens.isLoggedIn) { _events.tryEmit(CommunityPostEvent.LoginRequired); return }
        block()
    }

    fun addComment() = requireLogin {
        val body = _state.value.text
        mutate { editComment.add(postId, body, null)?.onSuccess { _state.update { it.copy(text = "") } } }
    }

    /** 답글 등록 — 답글의 답글도 같은 줄기(최상위 댓글)에 단다(웹: replyInputBox(targetId) → parentId=그 댓글). */
    fun addReply(targetId: Long) = requireLogin {
        val body = _state.value.replyText
        mutate { editComment.add(postId, body, targetId)?.onSuccess { _state.update { it.copy(replyText = "", replyTo = null) } } }
    }

    fun saveEdit(c: CommunityComment) = requireLogin {
        val body = _state.value.editText
        mutate { editComment.update(postId, c.id, body)?.onSuccess { _state.update { it.copy(editId = null, editText = "") } } }
    }

    fun confirmDeleteComment() = requireLogin {
        val id = _state.value.confirmDeleteComment ?: return@requireLogin
        _state.update { it.copy(confirmDeleteComment = null) }
        mutate { editComment.delete(postId, id) }
    }

    /** 바꾸고 다시 받기. 실패면 서버 문구(욕설 필터 등) 그대로 — 입력은 지우지 않는다. */
    private fun mutate(call: suspend () -> ApiResult<Unit>?) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val r = call()
            _state.update { it.copy(busy = false) }
            when (r) {
                null -> Unit
                is ApiResult.Error -> _events.tryEmit(CommunityPostEvent.Toast(r.message))
                is ApiResult.Success -> reload()
            }
        }
    }

    // ── 좋아요·스크랩 ──
    fun likePost() = requireLogin {
        if (_state.value.busy) return@requireLogin
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val r = like.post(postId)
            _state.update { s ->
                val p = s.post
                if (r is ApiResult.Success && p != null) s.copy(busy = false, post = p.copy(likedByMe = r.data.liked, summary = p.summary.copy(likeCount = r.data.likeCount)))
                else s.copy(busy = false)
            }
        }
    }

    fun likeComment(c: CommunityComment) = requireLogin {
        if (_state.value.busy) return@requireLogin
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val r = like.comment(postId, c.id)
            _state.update { s ->
                if (r !is ApiResult.Success) return@update s.copy(busy = false)
                fun upd(x: CommunityComment) = if (x.id == c.id) x.copy(likedByMe = r.data.liked, likeCount = r.data.likeCount) else x
                s.copy(busy = false, comments = s.comments.map(::upd), replies = s.replies.mapValues { (_, v) -> v.copy(items = v.items.map(::upd)) })
            }
        }
    }

    fun toggleScrap() = requireLogin {
        val p = _state.value.post ?: return@requireLogin
        val was = p.scrappedByMe
        _state.update { it.copy(post = p.copy(scrappedByMe = !was)) }
        viewModelScope.launch {
            when (val r = toggleScrap(ScrapTarget.POST, postId, was)) {
                is ToggleScrapResult.Done -> _events.tryEmit(CommunityPostEvent.ToastRes(if (r.liked) com.muyeon.app.R.string.community_scrapped else com.muyeon.app.R.string.community_unscrapped))
                is ToggleScrapResult.Failed -> _state.update { s -> s.copy(post = s.post?.copy(scrappedByMe = was)) }
                ToggleScrapResult.LoginRequired -> _events.tryEmit(CommunityPostEvent.LoginRequired)
            }
        }
    }

    // ── 시트·확인창 ──
    fun openPostSheet(open: Boolean) = _state.update { it.copy(postSheet = open) }
    fun openCommentSheet(c: CommunityComment?) = _state.update {
        if (c == null) it.copy(myCommentSheet = null, otherCommentSheet = null)
        else if (c.isMine) it.copy(myCommentSheet = c) else it.copy(otherCommentSheet = c)
    }
    fun askDeleteComment(c: CommunityComment) = _state.update { it.copy(myCommentSheet = null, confirmDeleteComment = c.id) }
    fun dismissDeleteComment() = _state.update { it.copy(confirmDeleteComment = null) }
    fun askDeletePost(ask: Boolean) = _state.update { it.copy(postSheet = false, confirmDeletePost = ask) }
    fun editPost() {
        _state.update { it.copy(postSheet = false) }
        _events.tryEmit(CommunityPostEvent.Edit(postId))
    }

    fun deletePost() = requireLogin {
        _state.update { it.copy(confirmDeletePost = false) }
        viewModelScope.launch {
            when (val r = deletePost(postId)) {
                is ApiResult.Success -> {
                    AppResults.publish(setOf(CommunityListViewModel.RESULT_COMMUNITY))
                    _events.tryEmit(CommunityPostEvent.Leave)
                }
                is ApiResult.Error -> _events.tryEmit(CommunityPostEvent.Toast(r.message))
            }
        }
    }

    fun openReport(target: ReportTarget?) {
        if (target == null) { _state.update { it.copy(report = null) }; return }
        requireLogin { _state.update { it.copy(report = target, otherCommentSheet = null) } }
    }

    fun submitReport(reason: String, detail: String) {
        val t = _state.value.report ?: return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val r = report(t.type, t.id, reason, detail)
            _state.update { it.copy(busy = false, report = null) }
            _events.tryEmit(CommunityPostEvent.ReportDone(r is ApiResult.Success, (r as? ApiResult.Error)?.message))
        }
    }

    fun openBlock(target: BlockTarget?) {
        if (target == null) { _state.update { it.copy(block = null) }; return }
        requireLogin { _state.update { it.copy(block = target, otherCommentSheet = null) } }
    }

    fun confirmBlock() {
        val t = _state.value.block ?: return
        _state.update { it.copy(block = null) }
        viewModelScope.launch {
            when (val r = blockUser(t.userId)) {
                is ApiResult.Success -> {
                    _events.tryEmit(CommunityPostEvent.ToastRes(com.muyeon.app.R.string.block_done))
                    AppResults.publish(setOf(CommunityListViewModel.RESULT_COMMUNITY))
                    if (t.leaveAfter) _events.tryEmit(CommunityPostEvent.Leave) else reload()
                }
                is ApiResult.Error -> _events.tryEmit(CommunityPostEvent.Toast(r.message))
            }
        }
    }

    /** 작성자 아바타 — 강사면 강사 프로필(웹 authorProfileClick). */
    fun openAuthor(isTeacher: Boolean, userId: Long?) {
        if (isTeacher && userId != null) _events.tryEmit(CommunityPostEvent.OpenTeacher(userId))
    }
}
