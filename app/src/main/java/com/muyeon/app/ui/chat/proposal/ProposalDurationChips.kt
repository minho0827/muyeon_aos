package com.muyeon.app.ui.chat.proposal

import androidx.compose.runtime.Composable
import com.muyeon.app.ui.common.FormChipWrap
import com.muyeon.app.ui.common.FormField

/** ★ 2026-11-14 수업 길이 — 무용연 알약 칩(30·60·90·120분). */
@Composable
internal fun ProposalDurationChips(durationMin: Int, onChange: (Int) -> Unit) {
    FormField("수업 길이") {
        FormChipWrap(
            options = ProposalFormState.DURATIONS.map { it.toString() to "${it}분" },
            isOn = { it == durationMin.toString() },
            onTap = { v -> v.toIntOrNull()?.let(onChange) },
        )
    }
}
