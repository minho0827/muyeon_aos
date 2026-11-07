package com.muyeon.app.ui.components

import androidx.compose.foundation.border
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

/** 상세 읽기 여러 줄 — 웹 ReadField.js `Area`. 앱은 글 길이에 맞춰 늘어난다(웹 auto). 비면 그리지 않는다. */
@Composable
fun ReadArea(value: String?, modifier: Modifier = Modifier, label: String? = null) {
    if (value.isNullOrBlank()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
        if (label != null) Text(label, style = AppTypography.Body14SemiBold, color = AppColors.TextBody)
        Text(
            value,
            style = AppTypography.Body15,
            color = AppColors.TextHead,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppColors.Divider, RoundedCornerShape(Dimensions.spacingS))
                .padding(Dimensions.spacingL),
        )
    }
}
