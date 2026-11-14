package com.muyeon.app.ui.review.quick

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

/** 빠른 후기 단계들이 함께 쓰는 하단 큰 버튼(당근 '다음'·'후기 보내기'). 꺼져 있으면 회색. */
@Composable
fun QuickReviewBottomButton(label: String, enabled: Boolean, onClick: () -> Unit, tag: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) MuyeonColors.primary else MuyeonColors.border)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(tag),
        Alignment.Center,
    ) {
        Text(
            label, color = if (enabled) Color.White else MuyeonColors.hint,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp,
        )
    }
}
