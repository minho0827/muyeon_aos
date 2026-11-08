package com.muyeon.app.ui.likes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.components.SlotText
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/**
 * 찜 카드(그리드) — 웹 Likes today-menu menu-card: 사진(왼쪽 위 '마감' · 오른쪽 위 채운 하트) · 제목 · 지역 · 귀퉁이 한 줄.
 *   홈 LessonProductCard 와 같은 비율·글자(3:4 · 장소 12 · 제목 15)로 맞췄다.
 */
@Composable
fun LikedCard(
    title: String,
    sub: String?,
    corner: String?,
    image: String?,
    fallbackRes: Int,
    closedLabel: String?,
    onClick: () -> Unit,
    onUnlike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(10.dp))) {
            RemoteImage(image, Modifier.matchParentSize(), fallbackRes = fallbackRes, thumbnail = true)
            closedLabel?.let {
                Text(it, style = AppTypography.Caption12, color = AppColors.TextOnPrimary,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(AppColors.Scrim, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
            }
            HeartButton(true, onUnlike, size = 22.dp, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
        SlotText(sub, AppTypography.Caption12, AppColors.TextHint, Modifier.padding(top = 10.dp))
        SlotText(title, AppTypography.Body15, AppColors.TextHead, Modifier.padding(top = 2.dp), lines = 2)
        SlotText(corner, AppTypography.Sub13, AppColors.TextSub, Modifier.padding(top = 2.dp))
    }
}
