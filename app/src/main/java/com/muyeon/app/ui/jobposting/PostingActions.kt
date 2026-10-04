package com.muyeon.app.ui.jobposting

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.muyeon.app.ui.quote.DialogAction
import com.muyeon.app.ui.quote.DialogMessage
import com.muyeon.app.ui.quote.DialogTitle
import com.muyeon.app.ui.quote.QuoteDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 공고 하나를 가리키는 키 — kind 가 달라도 id 가 겹칠 수 있어 둘 다 들고 다닌다. */
data class PostingRef(val kind: String, val id: Int)

val MyPosting.ref: PostingRef get() = PostingRef(kind, id)

/** 무엇이 바뀌었나 — 상세는 DELETED 면 닫고, 나머지는 재조회한다. */
enum class PostingChange { STATUS, CLOSED, DUPLICATED, DELETED }

/**
 * 내 공고 관리 목록·상세 공용 동작(상태 변경·마감·복사·삭제) — iOS MyJobPostingsViewModel 의 동작부와 같은 규칙.
 *  ★ 2026-10-04 목록에만 있던 동작을 네이티브 상세와 나눠 쓰려고 떼어냈다.
 *   · 마감하기: 채용·대타는 대기 지원자 수를 먼저 묻고 `close {rejectPending}` 로 일괄 미선정 안내(iOS PostingClosePrompt).
 *     종전 AOS 는 확인 없이 상태만 CLOSED 로 바꿔 남은 지원자가 결과 안내를 못 받았다.
 *   · 공연(CASTING)은 미선정 안내 API 가 없어 상태만 바꾼다(iOS 와 동일).
 * 화면은 [Dialogs] 를 한 번 그려 두기만 하면 된다.
 */
class PostingActions internal constructor(
    private val api: JobPostingApi,
    private val scope: CoroutineScope,
    private val changed: (PostingRef, PostingChange) -> Unit,
) {
    var message by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
        private set
    private var deleteTarget by mutableStateOf<PostingRef?>(null)
    private var closeTarget by mutableStateOf<Pair<PostingRef, Int>?>(null)

    private fun run(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { block() } finally { busy = false }
        }
    }

    fun setStatus(ref: PostingRef, status: String) = run {
        api.setStatus(ref.kind, ref.id, status)
            .onSuccess {
                message = when (status) {
                    "CLOSED" -> "공고가 마감되었습니다."
                    "HOLD" -> "공고가 보류되었습니다."
                    else -> "공고가 다시 등록되었습니다."
                }
                changed(ref, PostingChange.STATUS)
            }
            .onFailure { message = it.message ?: "상태 변경에 실패했어요." }
    }

    /** 마감하기 — 채용·대타는 대기 지원자 확인창부터. */
    fun requestClose(ref: PostingRef) {
        if (ref.kind != "JOB" && ref.kind != "SUB") { setStatus(ref, "CLOSED"); return }
        run {
            api.pendingApplicantCount(ref.kind, ref.id)
                .onSuccess { closeTarget = ref to it }
                .onFailure { message = it.message ?: "미선정 대상 인원을 불러오지 못했어요." }
        }
    }

    private fun confirmClose(ref: PostingRef) = run {
        api.closeWithApplicantResults(ref.kind, ref.id)
            .onSuccess {
                message = "공고를 마감하고 남은 지원자 ${it}명에게 미선정 결과를 안내했어요."
                changed(ref, PostingChange.CLOSED)
            }
            .onFailure { message = it.message ?: "공고 마감과 결과 안내에 실패했어요." }
    }

    fun duplicate(ref: PostingRef) = run {
        api.duplicate(ref.kind, ref.id)
            .onSuccess {
                message = "임시저장으로 복사했어요."
                changed(ref, PostingChange.DUPLICATED)
            }
            .onFailure { message = it.message ?: "복사에 실패했어요." }
    }

    fun requestDelete(ref: PostingRef) { deleteTarget = ref }

    private fun confirmDelete(ref: PostingRef) = run {
        api.remove(ref.kind, ref.id)
            .onSuccess {
                message = "공고를 삭제했어요."
                changed(ref, PostingChange.DELETED)
            }
            .onFailure { message = it.message ?: "삭제에 실패했어요." }
    }

    /** 확인창·안내창 — 화면 아무 곳에 한 번 그린다. */
    @Composable
    fun Dialogs() {
        // 삭제 확인 — 되돌릴 수 없어 한 번 더 묻는다.
        deleteTarget?.let { target ->
            QuoteDialog(
                title = "이 공고를 삭제할까요?",
                message = "목록에서 사라집니다. 이미 받은 지원 내역은 그대로 남아요.",
                confirmText = "삭제",
                onConfirm = { deleteTarget = null; confirmDelete(target) },
                onDismiss = { deleteTarget = null },
            )
        }
        // 마감 확인 — iOS PostingClosePrompt 와 같은 문구.
        closeTarget?.let { (target, count) ->
            val label = if (target.kind == "SUB") "대타공고" else "채용공고"
            QuoteDialog(
                title = "${label}를 마감할까요?",
                message = "남은 지원자 ${count}명이 미선정 처리되고 결과 알림이 발송됩니다. 이미 확정된 지원자는 제외됩니다.",
                confirmText = "마감하고 일괄 안내",
                onConfirm = { closeTarget = null; confirmClose(target) },
                onDismiss = { closeTarget = null },
            )
        }
        message?.let { msg ->
            PostingInfoDialog("알림", msg) { message = null }
        }
    }
}

@Composable
fun rememberPostingActions(
    api: JobPostingApi,
    onChanged: (PostingRef, PostingChange) -> Unit,
): PostingActions {
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(onChanged)
    return remember(api, scope) { PostingActions(api, scope) { r, c -> current(r, c) } }
}

/** 확인 버튼 하나짜리 안내창 — QuoteDialog 는 '취소'가 늘 붙어 안내용으로는 어색하다. */
@Composable
internal fun PostingInfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitle(title) },
        text = { DialogMessage(message) },
        confirmButton = { TextButton(onClick = onDismiss) { DialogAction("확인") } },
    )
}

/** 확인·취소 문구를 모두 지정하는 확인창(멤버십 안내의 '다음에' 등). */
@Composable
internal fun PostingConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitle(title) },
        text = { DialogMessage(message) },
        confirmButton = { TextButton(onClick = onConfirm) { DialogAction(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { DialogAction(dismissText) } },
    )
}
