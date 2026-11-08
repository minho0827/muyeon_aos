package com.muyeon.app.ui.lesson.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors

/** 레슨 상세 아래 찜 칸 — iOS LessonContentDetailView bottomButtons 1:1(52 · 모서리 12 · 회색 테두리 · 하트 20, 찜이면 주황). */
@Composable
fun LessonDetailLikeButton(liked: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(52.dp).clip(shape).border(1.dp, AppColors.Divider, shape).clickable(onClick = onClick).testTag("lessonDetail.like"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(if (liked) R.string.lesson_detail_unlike else R.string.lesson_detail_like),
            tint = if (liked) AppColors.Primary else AppColors.TextSub,
            modifier = Modifier.size(20.dp),
        )
    }
}
