package com.muyeon.app.ui.chat.proposal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.common.FormDateRow
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.WheelPickerSheet

/**
 * ★ 2026-11-14 날짜·시간 칸 — 무용연 날짜 줄 두 개, 누르면 무용연 휠 시트(예전 안드로이드 기본 보라 달력·시계 대신).
 *  날짜를 바꿔도 시각은, 시각을 바꿔도 날짜는 그대로다.
 */
@Composable
internal fun ProposalDateTimeField(startAt: Long, onChange: (Long) -> Unit) {
    var openDate by remember { mutableStateOf(false) }
    var openTime by remember { mutableStateOf(false) }
    FormField("날짜 · 시간") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormDateRow(ProposalTime.dateLabel(startAt), "날짜", Modifier.weight(1f)) { openDate = true }
            FormDateRow(ProposalTime.timeLabel(startAt), "시간", Modifier.weight(1f)) { openTime = true }
        }
    }
    if (openDate) {
        WheelPickerSheet(
            title = "날짜", options = ProposalTime.dayOptions(), selected = ProposalTime.dayStart(startAt),
            label = ProposalTime::dateLabel, onDismiss = { openDate = false },
        ) { day -> openDate = false; onChange(ProposalTime.withDay(startAt, day)) }
    }
    if (openTime) {
        WheelPickerSheet(
            title = "시간", options = ProposalTime.minuteOptions(), selected = ProposalTime.minuteOfDay(startAt),
            label = ProposalTime::minuteLabel, onDismiss = { openTime = false },
        ) { m -> openTime = false; onChange(ProposalTime.withMinute(startAt, m)) }
    }
}
