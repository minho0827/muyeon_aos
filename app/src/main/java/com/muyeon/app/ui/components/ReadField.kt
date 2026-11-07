package com.muyeon.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 상세 읽기 칸 — 웹 components/muyeon/ReadField.js `Field`(라벨 + 테두리 박스 한 줄).
 *   값이 비면 그리지 않는다(웹 hideEmpty). [link] 가 있으면 값 대신 주황 글자 링크.
 */
@Composable
fun ReadField(label: String, value: String?, modifier: Modifier = Modifier, linkText: String? = null, onLink: (() -> Unit)? = null) {
    if (onLink == null && value.isNullOrBlank()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
        Text(label, style = AppTypography.Body14SemiBold, color = AppColors.TextBody)
        if (onLink != null) {
            Text(linkText.orEmpty(), style = AppTypography.Body14SemiBold, color = AppColors.Primary, modifier = Modifier.clickable(onClick = onLink))
        } else {
            Text(
                value.orEmpty(),
                style = AppTypography.Body15,
                color = AppColors.TextHead,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .border(1.dp, AppColors.Divider, RoundedCornerShape(Dimensions.spacingS))
                    .padding(horizontal = Dimensions.spacingL, vertical = 13.dp),
            )
        }
    }
}
