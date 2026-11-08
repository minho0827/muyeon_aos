package com.muyeon.app.ui.about.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 끝 안내 — 웹 .about-cta(주황 그라데이션 · 24 모서리 · 가운데 · 흰 버튼). 아래 80. */
@Composable
fun AboutCta(onBrowse: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(start = Dimensions.gutter, end = Dimensions.gutter, top = 20.dp, bottom = 80.dp)
            .clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(AppColors.Primary, AppColors.PrimaryDeep)))
            .padding(horizontal = Dimensions.gutter, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AboutEyebrow(stringResource(R.string.about_cta_eyebrow), Modifier.padding(bottom = 16.dp), color = AppColors.TextOnPrimary)
        Text(stringResource(R.string.about_cta_title), style = AppTypography.Title22.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold),
            color = AppColors.TextOnPrimary, textAlign = TextAlign.Center)
        Text(stringResource(R.string.about_cta_desc), style = AppTypography.Body15, color = AppColors.TextOnPrimary.copy(alpha = 0.9f),
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
        Box(
            Modifier.padding(top = 28.dp).height(Dimensions.buttonMdHeight).clip(RoundedCornerShape(Dimensions.buttonRadius))
                .background(AppColors.Surface).clickable(onClick = onBrowse).padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.about_cta_button), style = AppTypography.Body15.copy(fontWeight = FontWeight.Bold), color = AppColors.PrimaryDeep)
        }
    }
}
