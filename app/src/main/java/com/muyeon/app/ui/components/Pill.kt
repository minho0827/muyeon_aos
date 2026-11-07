package com.muyeon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.PillColors

/** 색 알약 — 웹 Home pillStyle(패딩 3/8 · 모서리 6 · 11 semibold). 장르·추천·채용/대타 등. */
@Composable
fun Pill(text: String, colors: PillColors, modifier: Modifier = Modifier) {
    Text(
        text, style = AppTypography.Pill11, color = colors.fg, maxLines = 1,
        modifier = modifier.background(colors.bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
