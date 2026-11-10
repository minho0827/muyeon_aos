package com.muyeon.app.ui.quote.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.app.ui.theme.Dimensions

/** 제안 표시 — 철회됨(회색 배지 · 시각) / 수정됨(옅은 글씨 · 시각) / 없음. */
@Composable
fun QuoteResponseStatusBadge(mark: QuoteResponseMark, modifier: Modifier = Modifier) {
    when (mark) {
        is QuoteResponseMark.Withdrawn -> WithdrawnBadge(mark, modifier)
        is QuoteResponseMark.Edited -> EditedLabel(mark, modifier)
        QuoteResponseMark.None -> Unit
    }
}

@Composable
private fun WithdrawnBadge(mark: QuoteResponseMark.Withdrawn, modifier: Modifier) {
    val time = QuoteResponseMark.timeText(mark.at)
    val text = if (time.isEmpty()) stringResource(R.string.quote_owner_withdrawn_plain) else stringResource(R.string.quote_owner_withdrawn, time)
    LevelBadge(text, CategoryColors.Level.End, modifier)
}

@Composable
private fun EditedLabel(mark: QuoteResponseMark.Edited, modifier: Modifier) {
    val time = QuoteResponseMark.timeText(mark.at)
    if (time.isNotEmpty()) Text(stringResource(R.string.quote_owner_edited, time), style = AppTypography.Caption12, color = AppColors.TextSub, modifier = modifier)
}

/** 고객 쪽 — 철회된 제안 자리에 [채택] 대신 보이는 안내(+ 사유). */
@Composable
fun WithdrawnResponseNotice(mark: QuoteResponseMark.Withdrawn, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXXS)) {
        Text(stringResource(R.string.quote_owner_withdrawn_notice), style = AppTypography.Sub13SemiBold, color = AppColors.TextSub)
        mark.reason?.let { Text(stringResource(R.string.quote_owner_withdraw_reason, it), style = AppTypography.Sub13, color = AppColors.TextSub) }
    }
}
