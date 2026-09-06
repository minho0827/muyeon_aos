package com.muyeon.app.ui.resume

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.muyeon.app.ui.quote.DialogAction
import com.muyeon.app.ui.quote.DialogMessage
import com.muyeon.app.ui.quote.DialogTitle

/**
 * 합격 확정 후 안내 — iOS `ApplicantDecisionPrompt.swift` 1:1.
 *  확정 결과와 "남은 지원자 일괄 미선정" 유도를 단계별로 묻는다.
 */
sealed class ApplicantDecisionPrompt {
    /** 남은 검토 대상이 없어 안내만 하는 경우. */
    data class Confirmed(val title: String) : ApplicantDecisionPrompt()

    /** 대타 확정 — 바로 일괄 안내를 물어본다. */
    data class SubConfirmed(val count: Int) : ApplicantDecisionPrompt()

    /** 채용 확정 — 계속 검토 / 마감 중 선택. */
    data class JobConfirmed(val name: String, val count: Int) : ApplicantDecisionPrompt()

    /** 채용 마감 재확인. */
    data class JobCloseConfirmation(val count: Int) : ApplicantDecisionPrompt()
}

@Composable
fun ApplicantDecisionPromptDialog(
    prompt: ApplicantDecisionPrompt,
    onPrompt: (ApplicantDecisionPrompt?) -> Unit,
    onBulkNotify: () -> Unit,
) {
    when (prompt) {
        is ApplicantDecisionPrompt.Confirmed -> AlertDialog(
            onDismissRequest = { onPrompt(null) },
            title = { DialogTitle(prompt.title) },
            confirmButton = { TextButton(onClick = { onPrompt(null) }) { DialogAction("확인") } },
        )

        is ApplicantDecisionPrompt.SubConfirmed -> AlertDialog(
            onDismissRequest = { onPrompt(null) },
            title = { DialogTitle("대타가 확정되었어요") },
            text = { DialogMessage("나머지 지원자 ${prompt.count}명을 미선정 처리하고 결과 알림을 발송할까요?") },
            confirmButton = {
                TextButton(onClick = { onPrompt(null); onBulkNotify() }) { DialogAction("일괄 안내하기") }
            },
            dismissButton = { TextButton(onClick = { onPrompt(null) }) { DialogAction("계속 검토하기") } },
        )

        is ApplicantDecisionPrompt.JobConfirmed -> AlertDialog(
            onDismissRequest = { onPrompt(null) },
            title = { DialogTitle("${prompt.name} 님을 채용 확정했어요") },
            text = { DialogMessage("다른 지원자 ${prompt.count}명도 계속 검토하시겠어요?") },
            confirmButton = { TextButton(onClick = { onPrompt(null) }) { DialogAction("계속 검토하기") } },
            dismissButton = {
                TextButton(
                    onClick = { onPrompt(ApplicantDecisionPrompt.JobCloseConfirmation(prompt.count)) },
                ) { DialogAction("채용 마감하기") }
            },
        )

        is ApplicantDecisionPrompt.JobCloseConfirmation -> AlertDialog(
            onDismissRequest = { onPrompt(null) },
            title = { DialogTitle("채용을 마감할까요?") },
            text = {
                DialogMessage(
                    "채용을 마감하면 남은 지원자 ${prompt.count}명이 미선정 처리되고 결과 알림이 발송됩니다.",
                )
            },
            confirmButton = {
                TextButton(onClick = { onPrompt(null); onBulkNotify() }) { DialogAction("마감하고 일괄 안내") }
            },
            dismissButton = { TextButton(onClick = { onPrompt(null) }) { DialogAction("취소") } },
        )
    }
}
