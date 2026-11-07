package com.muyeon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.app.ui.theme.Dimensions

/**
 * 흰 글자 배지 — 웹 _chips.scss `.tag-group .level`(12 bold · 패딩 2/8 · 모서리 6).
 *   여러 개는 Row(spacedBy 4)로 나란히 둔다(웹 .tag-group gap 0.4rem).
 */
@Composable
fun LevelBadge(text: String, level: CategoryColors.Level, modifier: Modifier = Modifier) {
    Text(
        text,
        style = AppTypography.Badge12,
        color = AppColors.TextOnPrimary,
        maxLines = 1,
        modifier = modifier
            .background(level.bg, RoundedCornerShape(Dimensions.badgeRadius))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
