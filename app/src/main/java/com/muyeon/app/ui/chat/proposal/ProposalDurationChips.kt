package com.muyeon.app.ui.chat.proposal

import androidx.compose.runtime.Composable
import com.muyeon.app.ui.common.FormChipWrap
import com.muyeon.app.ui.common.FormField

/**
 * ★ 2026-11-14 수업 길이 — 무용연 알약 칩(30·60·90·120분).
 *  ★ 2026-11-27 견적 응답('회당 수업 길이', 필수)도 같은 칩을 쓴다. 견적에서 온 값이 목록에 없으면(예: 45분) 칩을 하나 더 둔다.
 */
@Composable
internal fun ProposalDurationChips(
    durationMin: Int?,
    label: String = "수업 길이",
    required: Boolean = false,
    onChange: (Int) -> Unit,
) {
    FormField(label, required = required) {
        FormChipWrap(
            options = durationOptions(durationMin).map { it.toString() to "${it}분" },
            isOn = { it == durationMin?.toString() },
            onTap = { v -> v.toIntOrNull()?.let(onChange) },
        )
    }
}

internal fun durationOptions(current: Int?): List<Int> =
    (ProposalFormState.DURATIONS + listOfNotNull(current?.takeIf { it > 0 })).distinct().sorted()
