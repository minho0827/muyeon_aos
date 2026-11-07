package com.muyeon.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 상세 섹션 제목 — 웹 ReadField.js `SecTit`(16 bold). */
@Composable
fun ReadSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = AppTypography.Section16, color = AppColors.TextHead, modifier = modifier.padding(top = Dimensions.spacingS))
}
