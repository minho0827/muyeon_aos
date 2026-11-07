package com.muyeon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/**
 * 연한 주황 안내 카드 — 웹 components/muyeon/GuideBanner.js(.write-guide: 주황 6% 바탕 · 모서리 12 · 흰 원 아이콘 32).
 *   [onClose] 가 있으면 오른쪽 × (터치 44).
 */
@Composable
fun GuideBanner(text: String, modifier: Modifier = Modifier, icon: String = "💡", onClose: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().background(AppColors.PrimaryTint, RoundedCornerShape(12.dp)).padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = if (onClose != null) 4.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(32.dp).background(AppColors.Surface, CircleShape), contentAlignment = Alignment.Center) {
            Text(icon, style = AppTypography.CardTitle16)
        }
        Text(text, style = AppTypography.Body14, color = AppColors.TextBody, modifier = Modifier.weight(1f))
        if (onClose != null) {
            val desc = stringResource(R.string.guide_close)
            Box(Modifier.size(44.dp).clickable(onClick = onClose).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
                Text("×", style = AppTypography.Title20, color = AppColors.TextHint)
            }
        }
    }
}
