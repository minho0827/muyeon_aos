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
import com.muyeon.app.ui.common.requiredField
import com.muyeon.app.ui.common.labelColor
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 양식 한 칸 — 이름(필수 * · 선택 표시) + 내용. 칸 사이 28. */
@Composable
fun SubFormSection(
    label: String,
    required: Boolean,
    modifier: Modifier = Modifier,
    /** 공통 필수값 확인(RequiredForm) — 필수 칸이면 넘긴다. */
    check: Pair<com.muyeon.app.ui.common.RequiredForm, String>? = null,
    filled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val m = if (check != null && required) modifier.then(Modifier.requiredField(check.first, check.second, label, filled)) else modifier
    Column(m.fillMaxWidth().padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = AppTypography.Section16, color = check?.let { it.first.labelColor(it.second) } ?: AppColors.TextHead)
            if (required) Text(" *", style = AppTypography.Section16, color = AppColors.Primary)
            else Text("  " + stringResource(R.string.subwiz_optional), style = AppTypography.Caption12, color = AppColors.TextHint)
        }
        content()
        check?.let { com.muyeon.app.ui.common.RequiredHint(it.first, it.second) }
    }
}
