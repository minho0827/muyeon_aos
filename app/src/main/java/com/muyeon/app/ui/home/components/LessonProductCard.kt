package com.muyeon.app.ui.home.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.components.Pill
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.home.HomeLesson

/**
 * 레슨 상품 카드 — 뷰티컬리 상품 카드처럼: 세로 사진(3:4, 둥근 모서리) · 왼쪽 위 배지(체험·원데이·장르) · 오른쪽 위 찜 ·
 *   아래 장소(회색 작은 글자) · 제목 두 줄 · 가격 굵게.
 */
@Composable
fun LessonProductCard(lesson: HomeLesson, tag: String?, liked: Boolean, onClick: () -> Unit, onToggleLike: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(10.dp))) {
            RemoteImage(lesson.image, Modifier.matchParentSize(), fallbackRes = R.drawable.img_noimage, thumbnail = true)
            val label = tag ?: lesson.genre
            label?.let { Pill(it, CategoryColors.lessonTag(tag, lesson.genre), Modifier.align(Alignment.TopStart).padding(8.dp)) }
            HeartButton(liked, onToggleLike, size = 22.dp, emptyColor = AppColors.Surface, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
        lesson.place?.let {
            Text(it, style = AppTypography.Caption12, color = AppColors.TextHint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
        }
        Text(lesson.title, style = AppTypography.Body15, color = AppColors.TextHead, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = if (lesson.place != null) 2.dp else 10.dp))
        Text(
            when (val p = lesson.price) {
                null -> stringResource(R.string.home_price_ask)
                0 -> stringResource(R.string.home_price_free)
                else -> stringResource(R.string.home_price_won, HomeLabels.won(p))
            },
            style = AppTypography.Section16, color = AppColors.TextHead, modifier = Modifier.padding(top = 4.dp),
        )
    }
}
