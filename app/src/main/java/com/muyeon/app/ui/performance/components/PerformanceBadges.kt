package com.muyeon.app.ui.performance.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.performance.PerformanceLabels
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.performance.PerformanceSummary

/** 공연 배지 줄 — 장르(초록) · 세부 분류(초록) · 상태(예정 초록/공연중 주황/종료 노랑). 목록·상세 같이 쓴다. */
@Composable
fun RowScope.PerformanceBadges(p: PerformanceSummary) {
    p.genre?.let { LevelBadge(it, CategoryColors.Level.Row) }
    PerformanceLabels.code(p.subGenre)?.let { LevelBadge(stringResource(it), CategoryColors.Level.Row) }
    if (p.status != null) LevelBadge(stringResource(PerformanceLabels.status(p.status)), PerformanceLabels.statusLevel(p.status))
}
