package com.muyeon.app.ui.subwizard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 칩 묶음 — 고른 칩은 검정 바탕 흰 글자(웹 _tab.scss 칩과 같다). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubChipGroup(options: List<String>, selected: List<String>, label: @Composable (String) -> String, onClick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = o in selected
            Text(
                label(o), style = if (on) AppTypography.Chip14Selected else AppTypography.Chip14,
                color = if (on) AppColors.TextOnPrimary else AppColors.ChipText,
                modifier = Modifier.background(if (on) AppColors.ChipSelectedBg else AppColors.Surface, RoundedCornerShape(50))
                    .border(1.dp, if (on) AppColors.ChipSelectedBg else AppColors.ChipBorder, RoundedCornerShape(50))
                    .clickable { onClick(o) }.padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}
