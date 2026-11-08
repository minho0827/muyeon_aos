package com.muyeon.app.ui.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 소셜 로그인 구분선 — 웹 로그인 '— 또는 —'(위 1.6rem · 아래 1.2rem · 간격 0.8rem). */
@Composable
fun LoginOrDivider(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = Dimensions.spacingL, bottom = Dimensions.spacingM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(AppColors.Divider))
        Text(text, style = AppTypography.Sub13, color = AppColors.TextSub)
        Box(Modifier.weight(1f).height(1.dp).background(AppColors.Divider))
    }
}
