package com.muyeon.app.ui.quote.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.chat.proposal.LessonTerms
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** ★ 2026-11-27 보낸 제안의 회당 수업 길이 · 장소 — 값이 있을 때만 한 줄씩. */
@Composable
fun QuoteTermsLines(terms: LessonTerms?, modifier: Modifier = Modifier) {
    val duration = terms?.durationMin?.takeIf { it > 0 }
    val place = terms?.place?.trim()?.takeIf { it.isNotEmpty() }
    if (duration == null && place == null) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        duration?.let { Text(stringResource(R.string.quote_terms_line_duration, it), style = AppTypography.Sub13, color = AppColors.TextSub) }
        place?.let { Text(stringResource(R.string.quote_terms_line_place, it), style = AppTypography.Sub13, color = AppColors.TextSub) }
    }
}
