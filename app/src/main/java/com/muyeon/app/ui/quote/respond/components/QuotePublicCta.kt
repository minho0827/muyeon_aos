package com.muyeon.app.ui.quote.respond.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 공개 상세의 견적 보내기 유도 박스 — 웹 .quote-public-cta(주황 6% 바탕 · 모서리 16 · 패딩 16 · 간격 6). */
@Composable
fun QuotePublicCta(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().background(AppColors.PrimaryTint, RoundedCornerShape(Dimensions.cardRadius)).padding(Dimensions.spacingL),
        verticalArrangement = Arrangement.spacedBy(CTA_GAP),
    ) {
        Text(title, style = AppTypography.Body15.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = AppColors.TextHead)
        Text(body, style = AppTypography.Sub13, color = AppColors.TextSub)
    }
}

private val CTA_GAP = 6.dp
