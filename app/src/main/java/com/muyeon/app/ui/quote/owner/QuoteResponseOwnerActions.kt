package com.muyeon.app.ui.quote.owner

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.common.ApiMessageError
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.app.ui.components.SheetAction
import com.muyeon.app.ui.components.SheetButton
import com.muyeon.app.ui.theme.AppTypography
import kotlinx.coroutines.launch

/**
 * 보낸 강사 본인의 [수정] [철회] — 보낸 제안 상세 · 견적 응답 화면이 같이 쓴다.
 *  [수정] → [QuoteResponseEditSheet](보낸 값으로 채움) · [철회] → 확인(사유 선택) → withdraw.
 *  서버 거절(409 채택·마감 등, 아직 배포 전 404·500)은 문구 그대로 팝업.
 * @param onChanged 서버가 돌려준 고친·철회한 제안 — 부르는 쪽이 화면 값을 바꾸거나 다시 읽는다.
 */
@Composable
fun QuoteResponseOwnerActions(
    quoteId: Int,
    responseId: Int,
    categoryLabel: String,
    sent: QuoteResponseDraft,
    onChanged: (QuoteResponseUpdate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val api = remember { QuoteResponseOwnerApi(context) }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var confirmWithdraw by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val failed = stringResource(R.string.quote_owner_failed)
    val noChange = stringResource(R.string.quote_owner_no_change)
    val editedToast = stringResource(R.string.quote_owner_edited_toast)
    val withdrawnToast = stringResource(R.string.quote_owner_withdrawn_toast)
    fun messageOf(e: Throwable) = (e as? ApiMessageError)?.message?.ifBlank { null } ?: failed

    Row(modifier.fillMaxWidth().testTag("quoteResponse.ownerActions"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SheetButton(SheetAction(stringResource(R.string.quote_owner_edit), false) { if (!busy) editing = true }, Modifier.weight(1f), enabled = !busy)
        SheetButton(SheetAction(stringResource(R.string.quote_owner_withdraw), false) { if (!busy) confirmWithdraw = true }, Modifier.weight(1f), enabled = !busy)
    }

    if (editing) {
        QuoteResponseEditSheet(
            categoryLabel = categoryLabel,
            sent = sent,
            onDismiss = { editing = false },
            onSave = save@{ draft ->
                val patch = draft.patchFrom(sent) ?: run { error = noChange; return@save false }
                var ok = false
                api.edit(quoteId, responseId, patch)
                    .onSuccess { u ->
                        ok = true
                        editing = false
                        Toast.makeText(context, editedToast, Toast.LENGTH_SHORT).show()
                        onChanged(u)
                    }
                    .onFailure { error = messageOf(it) }
                ok
            },
        )
    }

    if (confirmWithdraw) {
        WithdrawConfirm(
            onConfirm = { reason ->
                busy = true
                scope.launch {
                    api.withdraw(quoteId, responseId, reason)
                        .onSuccess { u ->
                            Toast.makeText(context, withdrawnToast, Toast.LENGTH_SHORT).show()
                            onChanged(u)
                        }
                        .onFailure { error = messageOf(it) }
                    busy = false
                }
            },
            onDismiss = { confirmWithdraw = false },
        )
    }

    error?.let { m ->
        MuyeonDialog(
            title = m,
            actions = listOf(MuyeonDialogAction(stringResource(R.string.quote_owner_ok))),
            onDismiss = { error = null },
        )
    }
}

/** 철회 확인 — 사유는 선택(200자). */
@Composable
private fun WithdrawConfirm(onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    MuyeonDialog(
        title = stringResource(R.string.quote_owner_withdraw_title),
        message = stringResource(R.string.quote_owner_withdraw_body),
        content = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it.take(QuoteResponseOwnerApi.REASON_MAX) },
                placeholder = { Text(stringResource(R.string.quote_owner_withdraw_reason_hint), style = AppTypography.Body14) },
                modifier = Modifier.fillMaxWidth().testTag("quoteResponse.withdrawReason"),
            )
        },
        actions = listOf(
            MuyeonDialogAction(stringResource(R.string.quote_owner_withdraw), MuyeonDialogKind.DESTRUCTIVE) { onConfirm(reason.trim().ifEmpty { null }) },
            MuyeonDialogAction(stringResource(R.string.quote_owner_cancel), MuyeonDialogKind.CANCEL),
        ),
        onDismiss = onDismiss,
    )
}
