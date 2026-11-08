package com.muyeon.app.ui.academyedit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 섹션 머리 — 웹 h2.t-18-single.fw-b + p.t-14-multi.fc-g600(설명이 없으면 제목만). */
@Composable
fun AcademyEditSectionHead(title: String, desc: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS)) {
        Text(title, style = AppTypography.Title18, color = AppColors.TextHead)
        if (desc != null) Text(desc, style = AppTypography.Body14, color = AppColors.TextSub)
    }
}
