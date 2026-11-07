package com.muyeon.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 빈 상태 — 웹 components/ui/EmptyView.js(.list-none: 문서 그림 48 · 제목 18 medium · 설명 14 · 외곽선 둥근 버튼).
 *   불러오는 중에는 그리지 않는다 — 부르는 쪽이 로딩이 끝난 뒤에만 넣는다(웹은 busy 게이트).
 */
@Composable
fun EmptyView(
    title: String,
    modifier: Modifier = Modifier,
    content: String? = null,
    buttonText: String? = null,
    onButton: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = Dimensions.spacing3XL * 2, horizontal = Dimensions.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS),
    ) {
        Icon(painterResource(R.drawable.ic_web_nodata), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(48.dp))
        Text(title, style = AppTypography.Title18.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = AppColors.TextHead, textAlign = TextAlign.Center)
        if (!content.isNullOrBlank()) Text(content, style = AppTypography.Body14, color = AppColors.TextSub, textAlign = TextAlign.Center)
        if (buttonText != null && onButton != null) {
            OutlinedButton(onClick = onButton, shape = CircleShape, border = BorderStroke(1.dp, AppColors.ChipBorder), modifier = Modifier.padding(top = Dimensions.spacingS)) {
                Text(buttonText, style = AppTypography.Body14SemiBold, color = AppColors.TextBody)
            }
        }
    }
}
