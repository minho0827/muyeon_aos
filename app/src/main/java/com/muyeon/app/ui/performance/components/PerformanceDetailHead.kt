package com.muyeon.app.ui.performance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.performance.PerformanceDetail

/**
 * 상세 머리 — 웹 PerformanceDetail .notice-head: 배지 줄 · 제목 · 부제(14 회색) · 단체. 오른쪽 위 하트(흰 원 · 그림자).
 */
@Composable
fun PerformanceDetailHead(detail: PerformanceDetail, liked: Boolean, onToggleLike: () -> Unit, modifier: Modifier = Modifier) {
    val p = detail.summary
    Box(modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingXL)) {
        Column(Modifier.fillMaxWidth().padding(end = Dimensions.spacing3XL + Dimensions.spacingS), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXS)) { PerformanceBadges(p) }
            Text(p.title, style = AppTypography.Title20, color = AppColors.TextHead)
            detail.subtitle?.let { Text(it, style = AppTypography.Body14, color = AppColors.TextHint) }
            p.company?.let { Text(it, style = AppTypography.Sub13, color = AppColors.TextHint) }
        }
        HeartButton(
            liked = liked,
            onToggle = onToggleLike,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .shadow(Dimensions.spacingXS, CircleShape)
                .background(AppColors.OverlayWhite, CircleShape),
        )
    }
}
