package com.muyeon.app.ui.performance.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.muyeon.app.R
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.components.ListRow
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.performance.PerformanceLabels
import com.muyeon.domain.model.performance.PerformanceSummary

/**
 * 공연 목록 한 줄 — 웹 PerformanceList renderRow: 포스터 · 배지 · 제목(20자) · "단체 · 공연장 · 기간" · 하트.
 */
@Composable
fun PerformanceRow(
    item: PerformanceSummary,
    liked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    ListRow(
        title = PerformanceLabels.listTitle(item.title),
        sub = listOfNotNull(item.company, item.venue, KstDates.dashPeriod(item.startAt, item.endAt).ifEmpty { null }).joinToString(" · "),
        onClick = onClick,
        thumbUrl = item.posterUrl,
        thumbFallbackRes = R.drawable.img_noimage_performance,
        tags = { PerformanceBadges(item) },
        trailing = { HeartButton(liked = liked, onToggle = onToggleLike) },
        showDivider = showDivider,
        modifier = modifier.testTag("performance_row_${item.id}"),
    )
}
