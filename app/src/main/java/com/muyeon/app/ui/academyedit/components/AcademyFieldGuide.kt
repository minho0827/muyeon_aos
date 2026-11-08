package com.muyeon.app.ui.academyedit.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 칸 아래 안내 한 줄 — 웹 FieldGuide(.message-box .message). */
@Composable
fun AcademyFieldGuide(text: String) {
    Text(text, style = AppTypography.Sub13, color = AppColors.TextSub)
}
