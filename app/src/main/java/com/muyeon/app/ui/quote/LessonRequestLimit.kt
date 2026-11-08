package com.muyeon.app.ui.quote

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind

/**
 * ★ 2026-11-08 원장·공연팀이 이번 달 레슨 요청(무료 3회)을 다 썼을 때(서버 403 LESSON_REQUEST_LIMIT).
 *  서버: POST /quotes → { code, message, data: { tier, limit, used, paths: { JOB, SUB } } }.
 *  채팅 한도 팝업(RecruitChatLimitDialog)과 같은 앱 공통 팝업 — 본문은 서버 문구 그대로(비면 기본 문구).
 *  iOS `LessonRequestLimitAlert`(QuoteWizard) 와 같은 버튼: [채용 공고 올리기] [대타공고 올리기] [멤버십 보기] [닫기].
 */
const val LESSON_REQUEST_LIMIT_CODE = "LESSON_REQUEST_LIMIT"
/** ★ 2026-11-08 같은 강사와 이미 대화 중(서버 409 QUOTE_CHAT_EXISTS { roomId }) — 위저드를 닫고 그 채팅방을 연다. */
const val QUOTE_CHAT_EXISTS_CODE = "QUOTE_CHAT_EXISTS"

@Composable
internal fun LessonRequestLimitDialog(
    message: String?,
    onJob: () -> Unit,
    onSub: () -> Unit,
    onMembership: () -> Unit,
    onDismiss: () -> Unit,
) {
    MuyeonDialog(
        title = stringResource(R.string.lrq_limit_title),
        message = message?.takeIf { it.isNotBlank() } ?: stringResource(R.string.lrq_limit_message_default),
        actions = listOf(
            MuyeonDialogAction(stringResource(R.string.lrq_action_job), MuyeonDialogKind.PRIMARY, onJob),
            MuyeonDialogAction(stringResource(R.string.lrq_action_sub), MuyeonDialogKind.CANCEL, onSub),
            MuyeonDialogAction(stringResource(R.string.lrq_action_membership), MuyeonDialogKind.CANCEL, onMembership),
            MuyeonDialogAction(stringResource(R.string.lrq_action_close), MuyeonDialogKind.CANCEL, onDismiss),
        ),
        dismissOnAction = false,
        onDismiss = onDismiss,
    )
}
