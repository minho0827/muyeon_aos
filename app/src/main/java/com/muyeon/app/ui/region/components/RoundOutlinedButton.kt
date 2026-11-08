package com.muyeon.app.ui.region.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 작은 둥근 외곽선 버튼 — 웹 .round-btn-sm.outlined(켜짐 .on-edit = 주황 테두리·글자). */
@Composable
fun RoundOutlinedButton(text: String, on: Boolean = false, tag: String? = null, onClick: () -> Unit) {
    val color = if (on) AppColors.Primary else AppColors.ChipBorder
    Text(
        text, style = AppTypography.Sub13SemiBold, color = if (on) AppColors.Primary else AppColors.TextBody,
        modifier = Modifier.clip(CircleShape).background(AppColors.Surface).border(1.dp, color, CircleShape)
            .clickable(onClick = onClick).let { if (tag != null) it.testTag(tag) else it }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}
