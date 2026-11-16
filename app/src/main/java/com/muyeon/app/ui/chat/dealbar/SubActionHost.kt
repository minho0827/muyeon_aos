package com.muyeon.app.ui.chat.dealbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.muyeon.app.ui.chat.ChatRoomViewModel
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.domain.common.ApiResult
import kotlinx.coroutines.launch

/**
 * ★ 2026-11-16 대타 카드 버튼 동작 — 기존 대타 API 를 그대로 쓴다(새 API 없음).
 *  - 대타 확정: ResumeApi.decide(ACCEPTED, SUB) = PATCH /subs/applications/:id/status (동시 확정은 서버가 막는다)
 *  - 지원 취소: MyApplicationsUseCase.withdraw("SUB") = POST /me/applications/SUB/:id/withdraw
 *  - 확정 취소: SubCancelDialog(벌점 미리보기 GET …/cancel-preview → POST …/cancel), 공고 화면과 같은 창
 *  - 지원서 보기: 지원자 이력서 화면 · 공고 보기: [onOpenPosting]
 *  끝나면 방(카드·잠금·시스템 글)을 다시 읽는다. 돌려주는 함수를 카드의 onAction 에 건다.
 */
@Composable
fun rememberSubActionHost(vm: ChatRoomViewModel, onOpenPosting: (Int) -> Unit): (SubAction, SubCardItem) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val withdrawUseCase = org.koin.compose.koinInject<com.muyeon.domain.usecase.application.MyApplicationsUseCase>()
    var confirmTarget by remember { mutableStateOf<SubCardItem?>(null) }
    var withdrawTarget by remember { mutableStateOf<SubCardItem?>(null) }
    var cancelTarget by remember { mutableStateOf<SubCardItem?>(null) }
    var busy by remember { mutableStateOf(false) }

    confirmTarget?.let { t ->
        MuyeonDialog(
            title = "이 강사님으로 대타를 확정할까요?",
            message = listOfNotNull(t.title, t.detail).joinToString("\n") + "\n확정하면 서로 연락처가 공개돼요.",
            actions = listOf(
                MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
                MuyeonDialogAction("대타 확정", MuyeonDialogKind.PRIMARY) {
                    if (busy) return@MuyeonDialogAction
                    busy = true
                    scope.launch {
                        com.muyeon.app.ui.resume.ResumeApi(vm.tokenForCards)
                            .decide(t.context.applicationId, "ACCEPTED", com.muyeon.app.ui.resume.ApplicantPostingKind.SUB)
                            .onSuccess { vm.toast = "대타가 확정됐어요." }
                            .onFailure { vm.toast = it.message ?: "대타를 확정하지 못했어요." }
                        busy = false
                        vm.reloadMessagesAndContext()
                    }
                },
            ),
            onDismiss = { confirmTarget = null },
        )
    }

    withdrawTarget?.let { t ->
        MuyeonDialog(
            title = "지원을 취소할까요?",
            message = "취소하면 되돌릴 수 없어요.\n원장님께는 지원 취소 기록만 남아요.",
            actions = listOf(
                MuyeonDialogAction("유지하기", MuyeonDialogKind.CANCEL),
                MuyeonDialogAction("지원 취소", MuyeonDialogKind.DESTRUCTIVE) {
                    if (busy) return@MuyeonDialogAction
                    busy = true
                    scope.launch {
                        when (val r = withdrawUseCase.withdraw("SUB", t.context.applicationId.toLong())) {
                            is ApiResult.Success -> vm.toast = "지원을 취소했어요."
                            is ApiResult.Error -> vm.toast = r.message
                        }
                        busy = false
                        vm.reloadMessagesAndContext()
                    }
                },
            ),
            onDismiss = { withdrawTarget = null },
        )
    }

    cancelTarget?.let { t ->
        val api = remember(vm.tokenForCards) { com.muyeon.app.ui.jobposting.JobPostingApi(vm.tokenForCards) }
        com.muyeon.app.ui.jobposting.SubCancelDialog(api, t.context.applicationId, onDismiss = { cancelTarget = null }) { msg ->
            cancelTarget = null
            vm.toast = msg
            vm.reloadMessagesAndContext()
        }
    }

    return { action, item ->
        val c = item.context
        when (action) {
            SubAction.VIEW_APPLICATION -> if (c.postingId > 0) {
                com.muyeon.app.ui.resume.ResumeActivity.startApplicant(context, c.postingId, c.applicationId, "SUB")
            }
            SubAction.VIEW_POSTING -> if (c.postingId > 0) onOpenPosting(c.postingId)
            SubAction.CONFIRM -> confirmTarget = item
            SubAction.WITHDRAW -> withdrawTarget = item
            SubAction.CANCEL_CONFIRMED -> cancelTarget = item
        }
    }
}
