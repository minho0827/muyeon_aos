package com.muyeon.app.ui.subwizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 양식 한 칸 — 이름(필수 * · 선택 표시) + 내용. 칸 사이 28. */
@Composable
fun SubFormSection(label: String, required: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = AppTypography.Section16, color = AppColors.TextHead)
            if (required) Text(" *", style = AppTypography.Section16, color = AppColors.Primary)
            else Text("  " + stringResource(R.string.subwiz_optional), style = AppTypography.Caption12, color = AppColors.TextHint)
        }
        content()
    }
}
