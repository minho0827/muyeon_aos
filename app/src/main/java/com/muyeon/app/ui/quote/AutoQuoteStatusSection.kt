package com.muyeon.app.ui.quote

import androidx.compose.runtime.Composable
import com.muyeon.app.ui.common.MuyeonGroupRow
import com.muyeon.app.ui.common.MuyeonGroupSection
import com.muyeon.app.ui.common.MuyeonSwitch

/** ★ 2026-11-06 스위치 아래 한 줄 — 켜짐인데 안 나가는 경우를 따로 말한다(iOS·웹과 같은 문장).
 *  켜짐/꺼짐은 스위치가 보여 주므로 문장 앞에 붙이지 않는다. */
fun autoQuoteStatusSentence(status: AutoQuoteStatus, switchOn: Boolean): String = when {
    switchOn && status.sending -> "'${status.activeTitle?.ifEmpty { null } ?: "선택한"}' 메시지로 새 요청에 바로 견적을 보내요."
    switchOn -> "켜져 있지만 지금은 보내지 않아요 — 아래 내용을 확인해 주세요"
    else -> "새 요청에 자동 견적을 보내지 않아요."
}

/** ★ 2026-11-06 맨 위 묶음 — 자동 견적 발송 스위치 + 이번 달 자동견적(누르면 발송 내역). */
@Composable
fun AutoQuoteStatusSection(
    status: AutoQuoteStatus,
    switchOn: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenHistory: () -> Unit,
) {
    MuyeonGroupSection {
        MuyeonGroupRow(
            title = "자동 견적 발송",
            subtitle = autoQuoteStatusSentence(status, switchOn),
            showChevron = false,
            accessory = { MuyeonSwitch(switchOn, onToggle, label = "자동 견적 발송") },
        )
        status.quota?.let { q ->
            MuyeonGroupRow(
                title = "이번 달 자동견적",
                value = if (q.limit < 0) "${q.used}회 · 무제한" else "${q.used} / ${q.limit}회",
                onClick = onOpenHistory,
            )
        }
    }
}
