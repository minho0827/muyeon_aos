package com.muyeon.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 신고 사유 — 웹 constants/statusOptions.js REPORT_REASONS 와 같은 코드·순서. */
enum class ReportReason(val code: String) { FAKE_POSTING("FAKE_POSTING"), INAPPROPRIATE("INAPPROPRIATE"), FAKE_RESUME("FAKE_RESUME"), SPAM("SPAM"), ETC("ETC") }

/**
 * 신고 시트 — 웹 components/muyeon/ReportButton.js 모달. 사유 하나(라디오) + 상세(선택, 500자) + 긴급 문의 메일 + [취소][신고하기].
 *   보내기·결과 안내는 부르는 쪽이 한다([onSubmit]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSheet(busy: Boolean, onSubmit: (reason: String, detail: String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf<ReportReason?>(null) }
    var detail by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = AppColors.Surface) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingS),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM),
        ) {
            Text(stringResource(R.string.report_title), style = AppTypography.Title18, color = AppColors.TextHead)
            ReportReason.entries.forEach { r ->
                Row(Modifier.fillMaxWidth().clickable { reason = r }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = reason == r, onClick = { reason = r }, colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary))
                    Text(stringResource(labelOf(r)), style = AppTypography.Body15, color = AppColors.TextHead)
                }
            }
            OutlinedTextField(
                value = detail,
                onValueChange = { if (it.length <= DETAIL_MAX) detail = it },
                placeholder = { Text(stringResource(R.string.report_detail_hint), style = AppTypography.Body14, color = AppColors.TextHint) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                textStyle = AppTypography.Body15,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.ChipBorder),
            )
            // 긴급 문의 — 앱 안 신고와 별도로 운영자에게 바로 닿는 길(Apple 1.2, 웹과 같은 말).
            Text(stringResource(R.string.report_urgent), style = AppTypography.Sub13, color = AppColors.TextSub)
            Row(Modifier.fillMaxWidth().padding(bottom = Dimensions.spacingS), horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
                SheetButton(SheetAction(stringResource(R.string.common_cancel), false, onDismiss), Modifier.weight(1f))
                SheetButton(
                    SheetAction(stringResource(R.string.report_submit), true) { reason?.let { onSubmit(it.code, detail) } },
                    Modifier.weight(1f),
                    enabled = reason != null && !busy,
                )
            }
        }
    }
}

private fun labelOf(r: ReportReason): Int = when (r) {
    ReportReason.FAKE_POSTING -> R.string.report_reason_fake_posting
    ReportReason.INAPPROPRIATE -> R.string.report_reason_inappropriate
    ReportReason.FAKE_RESUME -> R.string.report_reason_fake_resume
    ReportReason.SPAM -> R.string.report_reason_spam
    ReportReason.ETC -> R.string.report_reason_etc
}

private const val DETAIL_MAX = 500
