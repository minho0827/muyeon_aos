package com.muyeon.app.ui.about.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 영문 머리글 — 웹 .about-eyebrow(주황 12 굵게, 아래 16). */
@Composable
fun AboutEyebrow(text: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = AppColors.Primary) {
    Text(text, style = AppTypography.Caption12.copy(fontWeight = FontWeight.ExtraBold), color = color, modifier = modifier)
}
