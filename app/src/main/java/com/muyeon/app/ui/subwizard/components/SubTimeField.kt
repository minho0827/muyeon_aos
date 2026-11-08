package com.muyeon.app.ui.subwizard.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.common.WheelColumn
import com.muyeon.app.ui.subwizard.SubWizardQuestions
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/**
 * 수업 시간 — [시작] ~ [끝] 두 칸. 누르면 시·분 휠(분은 10분 단위 — 16:10 처럼, 사용자 요청 2026-11-08).
 *   휠은 기존 공용 WheelColumn(이력서 기간·레슨 개설과 같은 휠)을 쓴다.
 */
@Composable
fun SubTimeField(start: String?, end: String?, onStart: (String) -> Unit, onEnd: (String) -> Unit) {
    var editing by remember { mutableStateOf<Boolean?>(null) } // true=시작, false=끝
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        TimeBox(stringResource(R.string.subwiz_start), start, Modifier.weight(1f)) { editing = true }
        Text("~", style = AppTypography.Body15, color = AppColors.TextSub)
        TimeBox(stringResource(R.string.subwiz_end), end, Modifier.weight(1f)) { editing = false }
    }
    editing?.let { isStart ->
        val initial = (if (isStart) start else end) ?: (if (isStart) SubWizardQuestions.DEFAULT_START else nextHour(start))
        TimeWheelSheet(
            title = stringResource(if (isStart) R.string.subwiz_start else R.string.subwiz_end),
            initial = initial,
            onDismiss = { editing = null },
            onDone = { v -> editing = null; if (isStart) onStart(v) else onEnd(v) },
        )
    }
}

/** 시작 한 시간 뒤(끝 기본값). */
private fun nextHour(start: String?): String {
    val (h, m) = (start ?: SubWizardQuestions.DEFAULT_START).split(":").map { it.toInt() }
    return "%02d:%02d".format((h + 1).coerceAtMost(23), m)
}

@Composable
private fun TimeBox(label: String, value: String?, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.border(1.dp, if (value != null) AppColors.TextHead else AppColors.ChipBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, style = AppTypography.Caption12, color = AppColors.TextHint)
        Text(value ?: stringResource(R.string.subwiz_time_pick), style = AppTypography.Section16,
            color = if (value != null) AppColors.TextHead else AppColors.TextHint)
    }
}

/** 시·분 휠 시트 — [취소] 제목 [완료]. 분은 00·10·…·50. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeWheelSheet(title: String, initial: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val (ih, im) = initial.split(":").map { it.toInt() }
    var hour by remember { mutableIntStateOf(ih) }
    var minute by remember { mutableIntStateOf(SubWizardQuestions.MINUTES.minByOrNull { kotlin.math.abs(it - im) } ?: 0) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.Surface, dragHandle = null) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.common_cancel), style = AppTypography.Body15, color = AppColors.TextSub, modifier = Modifier.clickable(onClick = onDismiss))
                Text(title, style = AppTypography.Section16, color = AppColors.TextHead, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.common_done), style = AppTypography.Body15Medium, color = AppColors.Primary,
                    modifier = Modifier.clickable { onDone("%02d:%02d".format(hour, minute)) })
            }
            val ctx = androidx.compose.ui.platform.LocalContext.current
            Row(Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 40.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                WheelColumn(SubWizardQuestions.HOURS, hour, { ctx.getString(R.string.subwiz_hour, it) }, enabled = true, onSelect = { hour = it },
                    modifier = Modifier.weight(1f).height(160.dp), visibleRows = 5)
                WheelColumn(SubWizardQuestions.MINUTES, minute, { ctx.getString(R.string.subwiz_minute, it) }, enabled = true, onSelect = { minute = it },
                    modifier = Modifier.weight(1f).height(160.dp), visibleRows = 5)
            }
            Box(Modifier.height(16.dp))
        }
    }
}
