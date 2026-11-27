package com.muyeon.app.ui.chat.proposal

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import kotlinx.coroutines.launch

/**
 * ★ 2026-11-27 다른 시간 제안 시트 — 날짜·시간·메모만 고친다. 수업 길이·장소는 받은 제안 그대로(읽기 전용 줄).
 *  성공하면 [onSent](부르는 쪽이 닫고 카드를 다시 읽는다). 실패면 서버 문장을 토스트 + 시트 안 한 줄로 두고 열어 둔다.
 *  스와이프·바깥 누름으로 닫힌다(ProposalSheetScaffold — ModalBottomSheet).
 */
@Composable
fun ProposalCounterSheet(
    initialStartAt: Long,
    durationMin: Int?,
    place: String?,
    send: suspend (startAt: Long, memo: String) -> Result<Unit>,
    onSent: () -> Unit,
    onDismiss: () -> Unit,
) {
    val fallback = stringResource(R.string.proposal_counter_failed)
    val state = remember { ProposalCounterState(initialStartAt, fallback, send) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(state.errorText) { state.errorText?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } }

    ProposalSheetScaffold(
        sending = state.sending,
        canSend = state.canSend,
        onCancel = onDismiss,
        onSend = { scope.launch { if (state.submit()) onSent() } },
        title = stringResource(R.string.proposal_counter_title),
    ) {
        ProposalIntro(stringResource(R.string.proposal_counter_intro))
        ProposalDateTimeField(state.startAt) { state.startAt = it }
        durationMin?.takeIf { it > 0 }?.let {
            ProposalFixedRow(
                stringResource(R.string.proposal_fixed_duration_label),
                stringResource(R.string.proposal_fixed_duration_value, it), null, "proposal.counter.duration",
            )
        }
        place?.takeIf { it.isNotBlank() }?.let {
            ProposalFixedRow(stringResource(R.string.proposal_fixed_place_label), it, null, "proposal.counter.place")
        }
        ProposalMemoField(state.memo) { state.memo = it }
        ProposalError(state.errorText)
    }
}
