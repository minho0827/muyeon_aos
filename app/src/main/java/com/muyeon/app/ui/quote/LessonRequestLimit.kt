package com.muyeon.app.ui.quote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind

/**
 * ★ 2026-11-08 원장·공연팀이 이번 달 레슨 요청(무료 3회)을 다 썼을 때(서버 403 LESSON_REQUEST_LIMIT).
 *  서버: POST /quotes → { code, message, data: { tier, limit, used, paths: { JOB, SUB } } }.
 *  채팅 한도 팝업(RecruitChatLimitDialog)과 같은 앱 공통 팝업 — 본문은 서버 문구 그대로(비면 기본 문구).
 *  iOS `LessonRequestLimitAlert`(QuoteWizard) 와 같은 버튼.
 *  ★ 2026-11-23 멤버십 가입을 첫 버튼으로(사용자 결정): [멤버십 가입하기] [채용 공고 올리기] [대타공고 올리기] [닫기].
 */
const val LESSON_REQUEST_LIMIT_CODE = "LESSON_REQUEST_LIMIT"
/** ★ 2026-11-08 같은 강사와 이미 대화 중(서버 409 QUOTE_CHAT_EXISTS { roomId }) — 위저드를 닫고 그 채팅방을 연다. */
const val QUOTE_CHAT_EXISTS_CODE = "QUOTE_CHAT_EXISTS"
/** ★ 2026-11-23 학원 원장 회원·학원 요청은 강사 1:1로만(서버 403 QUOTE_DIRECT_ONLY { teachersPath }). */
const val QUOTE_DIRECT_ONLY_CODE = "QUOTE_DIRECT_ONLY"

/** ★ 2026-11-23 "강사님을 골라 1:1로 요청해 주세요" — [강사 보러가기] = 홈 강사 목록(iOS presentDirectOnly 와 같다). */
@Composable
internal fun DirectOnlyDialog(
    message: String?,
    onTeachers: () -> Unit,
    onDismiss: () -> Unit,
) {
    MuyeonDialog(
        title = stringResource(R.string.lrq_direct_only_title),
        message = message?.takeIf { it.isNotBlank() } ?: stringResource(R.string.lrq_direct_only_message),
        actions = listOf(
            MuyeonDialogAction(stringResource(R.string.lrq_action_teachers), MuyeonDialogKind.PRIMARY, onTeachers),
            MuyeonDialogAction(stringResource(R.string.lrq_action_close), MuyeonDialogKind.CANCEL, onDismiss),
        ),
        dismissOnAction = false,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun LessonRequestLimitDialog(
    message: String?,
    onJob: () -> Unit,
    onSub: () -> Unit,
    onMembership: () -> Unit,
    onDismiss: () -> Unit,
    // ★ 2026-11-27 일반 회원(서버 audience GENERAL)은 공고 길이 없다 — 채용·대타 공고 버튼을 뺀다.
    showPostings: Boolean = true,
) {
    // ★ 11-27 연타 방지 — 첫 탭만 받는다(멤버십 화면이 두 번 열리던 길). 다이얼로그가 닫히면 상태도 같이 사라진다.
    var handled by remember { mutableStateOf(false) }
    val once: (() -> Unit) -> () -> Unit = { action -> { if (!handled) { handled = true; action() } } }
    val onJob = once(onJob)
    val onSub = once(onSub)
    val onMembership = once(onMembership)
    val onDismiss = once(onDismiss)
    MuyeonDialog(
        title = stringResource(R.string.lrq_limit_title),
        message = message?.takeIf { it.isNotBlank() } ?: stringResource(R.string.lrq_limit_message_default),
        actions = buildList {
            add(MuyeonDialogAction(stringResource(R.string.lrq_action_membership), MuyeonDialogKind.PRIMARY, onMembership))
            if (showPostings) {
                add(MuyeonDialogAction(stringResource(R.string.lrq_action_job), MuyeonDialogKind.CANCEL, onJob))
                add(MuyeonDialogAction(stringResource(R.string.lrq_action_sub), MuyeonDialogKind.CANCEL, onSub))
            }
            add(MuyeonDialogAction(stringResource(R.string.lrq_action_close), MuyeonDialogKind.CANCEL, onDismiss))
        },
        dismissOnAction = false,
        onDismiss = onDismiss,
    )
}
