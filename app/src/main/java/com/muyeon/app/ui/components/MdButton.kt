package com.muyeon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 웹 `.btn-md` 한 줄 버튼(높이 48 · 모서리 12) — [filled] = `.filled.primary`(주황 · 흰 16 bold),
 *   아니면 `.outlined`(흰 바탕 · 회색 테두리 · 검정 16 semibold). 화면 중간에 쓰는 버튼(로그인·회원가입 등).
 */
@Composable
fun MdButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    loading: Boolean = false,
    tag: String? = null,
) {
    val shape = RoundedCornerShape(Dimensions.buttonRadius)
    val canTap = enabled && !loading
    Box(
        modifier.fillMaxWidth().height(Dimensions.buttonMdHeight).clip(shape)
            .then(
                if (filled) Modifier.background(if (enabled) AppColors.Primary else AppColors.Placeholder)
                else Modifier.background(AppColors.Surface).border(1.dp, if (enabled) AppColors.ButtonOutline else AppColors.Divider, shape),
            )
            .clickable(enabled = canTap, role = Role.Button, onClick = onClick)
            .let { m -> if (tag != null) m.testTag(tag) else m },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = if (filled) AppColors.TextOnPrimary else AppColors.Primary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(
                text,
                style = AppTypography.CardTitle16.copy(fontWeight = if (filled) FontWeight.Bold else FontWeight.SemiBold),
                color = when {
                    !enabled -> AppColors.TextDisabled
                    filled -> AppColors.TextOnPrimary
                    else -> AppColors.TextHead
                },
            )
        }
    }
}
