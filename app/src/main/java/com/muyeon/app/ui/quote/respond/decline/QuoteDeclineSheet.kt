package com.muyeon.app.ui.quote.respond.decline

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.SheetAction
import com.muyeon.app.ui.components.SheetButton
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * ★ 2026-11-14 "죄송해요, 다음에 뵐게요" → 사유 고르기 시트(iOS QuoteDeclineSheet 1:1).
 *  라디오 7개(기타면 글 칸) + [취소][정중히 거절하기](스크롤 밖 아래 고정 — 키보드가 올라와도 보인다). 보내기는 부르는 쪽.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDeclineSheet(
    academy: Boolean,
    busy: Boolean,
    onSubmit: (reason: QuoteDeclineReason, detail: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf<QuoteDeclineReason?>(null) }
    var detail by remember { mutableStateOf("") }
    val canSubmit = reason != null && !busy && (reason != QuoteDeclineReason.OTHER || detail.isNotBlank())
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = AppColors.Surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingS),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS),
            ) {
                Text(stringResource(R.string.quote_decline_title), style = AppTypography.Title18, color = AppColors.TextHead)
                Text(stringResource(R.string.quote_decline_subtitle), style = AppTypography.Sub13, color = AppColors.TextSub)
                QuoteDeclineReason.entries.forEach { r ->
                    QuoteDeclineReasonRow(r, academy, selected = reason == r) { reason = r }
                }
                if (reason == QuoteDeclineReason.OTHER) {
                    OutlinedTextField(
                        value = detail,
                        onValueChange = { detail = it.take(QuoteDeclineReason.DETAIL_MAX) },
                        placeholder = { Text(stringResource(R.string.quote_decline_other_hint), style = AppTypography.Body14, color = AppColors.TextHint) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp).testTag("quoteDecline.detail"),
                        textStyle = AppTypography.Body15,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.ChipBorder),
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().background(AppColors.Surface).padding(horizontal = Dimensions.gutter, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS),
            ) {
                SheetButton(SheetAction(stringResource(R.string.common_cancel), false, onDismiss), Modifier.weight(1f))
                SheetButton(
                    SheetAction(stringResource(R.string.quote_decline_submit), true) { reason?.let { onSubmit(it, detail) } },
                    Modifier.weight(1f).testTag("quoteDecline.submit"),
                    enabled = canSubmit,
                )
            }
        }
    }
}

/** 라디오 한 줄(+ 예시 글). */
@Composable
private fun QuoteDeclineReasonRow(reason: QuoteDeclineReason, academy: Boolean, selected: Boolean, onTap: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onTap).testTag("quoteDecline.reason.${reason.code}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onTap, colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary, unselectedColor = AppColors.ChipBorder))
        Column {
            Text(stringResource(reason.label(academy)), style = AppTypography.Body15, color = AppColors.TextHead)
            reason.hint?.let { Text(stringResource(it), style = AppTypography.Sub13, color = AppColors.TextSub) }
        }
    }
}
