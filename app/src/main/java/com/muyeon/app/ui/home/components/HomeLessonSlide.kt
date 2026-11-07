package com.muyeon.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.components.Pill
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.home.HomeLesson

/**
 * 홈 레슨 카드 — 웹 Home lessonSlide: 사진 3:2 · 왼쪽 위 배지(유형/장르 색) · 오른쪽 위 하트(흰 원) ·
 *   (추천) · 제목 · "지역 · 일정"(한 줄) · 가격(굵게, 0 = 무료 · 없음 = 가격 문의).
 */
@Composable
fun HomeLessonSlide(lesson: HomeLesson, tag: String?, liked: Boolean, onClick: () -> Unit, onToggleLike: () -> Unit, modifier: Modifier = Modifier) {
    val label = tag ?: lesson.genre
    HomeCard(
        imageUrl = lesson.image, fallbackRes = R.drawable.img_noimage, onClick = onClick, modifier = modifier,
        image = HomeImageSize.Ratio(3f / 2f),
        overlay = {
            label?.let {
                val c = CategoryColors.lessonTag(it, lesson.genre)
                Text(it, style = AppTypography.Pill11, color = c.fg, modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(c.bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
            }
            HeartButton(liked, onToggleLike, size = 20.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).shadow(4.dp, CircleShape).background(AppColors.OverlayWhite, CircleShape))
        },
    ) {
        if (HomeLabels.isBoosted(lesson.promotedUntil)) Pill(stringResource(R.string.home_boosted), CategoryColors.Boosted)
        HomeCardTitle(lesson.title)
        listOfNotNull(lesson.place, lesson.scheduleText).joinToString(" · ").takeIf { it.isNotEmpty() }?.let { HomeCardSub(it, singleLine = true) }
        HomeCardSub(
            when (val price = lesson.price) {
                null -> stringResource(R.string.home_price_ask)
                0 -> stringResource(R.string.home_price_free)
                else -> stringResource(R.string.home_price_won, HomeLabels.won(price))
            },
            strong = true,
        )
    }
}
