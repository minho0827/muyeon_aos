package com.muyeon.app.ui.about.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 서비스 4가지 — 웹 .about-section(머리 + .about-feature-grid 한 줄씩). 카드: 회색 바탕 · 20 모서리 · 24 안쪽 · 번호 → 28 → 제목 20 → 설명 14. */
@Composable
fun AboutFeatures() {
    val features = listOf(
        R.string.about_f1_title to R.string.about_f1_body, R.string.about_f2_title to R.string.about_f2_body,
        R.string.about_f3_title to R.string.about_f3_body, R.string.about_f4_title to R.string.about_f4_body,
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = 72.dp)) {
        AboutSectionHead(stringResource(R.string.about_service_eyebrow), stringResource(R.string.about_service_title), stringResource(R.string.about_service_desc))
        Column(Modifier.padding(top = 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            features.forEachIndexed { i, (t, b) -> AboutFeatureCard("%02d".format(i + 1), stringResource(t), stringResource(b)) }
        }
    }
}

@Composable
fun AboutFeatureCard(number: String, title: String, body: String) {
    Column(Modifier.fillMaxWidth().background(AppColors.SurfaceMuted, RoundedCornerShape(20.dp)).padding(24.dp)) {
        Text(number, style = AppTypography.Caption12.copy(fontWeight = FontWeight.ExtraBold), color = AppColors.Primary)
        Text(title, style = AppTypography.Title20, color = AppColors.TextHead, modifier = Modifier.padding(top = 28.dp))
        Text(body, style = AppTypography.Body14.copy(lineHeight = 23.sp), color = AppColors.TextSub, modifier = Modifier.padding(top = 10.dp))
    }
}

/** 영문 머리글 + 제목(28 굵게) + 설명(16) — 웹 .about-section__head. */
@Composable
fun AboutSectionHead(eyebrow: String, title: String, desc: String? = null) {
    Column {
        AboutEyebrow(eyebrow, Modifier.padding(bottom = 16.dp))
        Text(title, style = AppTypography.Title22.copy(fontSize = SECTION_TITLE, lineHeight = SECTION_TITLE * 1.25f, fontWeight = FontWeight.ExtraBold), color = AppColors.TextHead)
        if (desc != null) Text(desc, style = AppTypography.Body15.copy(lineHeight = 25.sp), color = AppColors.TextSub, modifier = Modifier.padding(top = 14.dp))
    }
}

private val SECTION_TITLE = 26.sp
