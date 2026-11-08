package com.muyeon.app.ui.quotefeed.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.Pill
import com.muyeon.app.ui.components.SlotText
import com.muyeon.app.ui.quotefeed.QuoteFeedLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.quotefeed.QuoteFeedItem

/**
 * '레슨을 찾는 회원들' 카드 — 홈 레슨 상품 카드([com.muyeon.app.ui.home.components.LessonProductCard])와 같은 틀·크기.
 *   사진 자리(3:4) = 요청 장르 그림(요청에는 공개 사진이 없다) · 왼쪽 위 장르 알약 · 오른쪽 위 '내 요청'/'제안 보냄' ·
 *   아래 지역(회색) · "발레를 가르쳐주실 강사님을 찾고 있어요"(두 줄) · 희망 요일·시간대(굵게) · 레슨 형태·등록 시각(회색).
 *   요청자 이름·연락처는 서버가 주지 않는다(웹 공개 정책 그대로).
 */
@Composable
fun QuoteFeedCard(item: QuoteFeedItem, sent: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(10.dp)).background(AppColors.SurfaceMuted)) {
            Image(
                painterResource(QuoteFeedLabels.genreImage(item.categoryId) ?: R.drawable.img_noimage), null,
                contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize(),
            )
            val genre = QuoteFeedLabels.genre(item.categoryId) ?: stringResource(R.string.quote_feed_lesson)
            Pill(genre, CategoryColors.genre(genre), Modifier.align(Alignment.TopStart).padding(8.dp))
            val state = when {
                item.isMine -> stringResource(R.string.quote_feed_mine)
                sent -> stringResource(R.string.quote_feed_sent)
                else -> null
            }
            state?.let { Pill(it, CategoryColors.Default, Modifier.align(Alignment.TopEnd).padding(8.dp)) }
        }
        // 지역 1줄 · 제목 2줄 · 일정 1줄 · 형태·등록 1줄 — 값이 짧아도 자리를 남긴다(카드 높이 맞춤).
        SlotText(QuoteFeedLabels.region(item), AppTypography.Caption12, AppColors.TextHint, Modifier.padding(top = 10.dp))
        SlotText(QuoteFeedLabels.seeking(item.categoryId), AppTypography.Body15, AppColors.TextHead, Modifier.padding(top = 2.dp), lines = 2)
        Text(
            QuoteFeedLabels.schedule(item), style = AppTypography.Section16, color = AppColors.TextHead, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp),
        )
        SlotText(QuoteFeedLabels.meta(item), AppTypography.Caption12, AppColors.TextHint, Modifier.padding(top = 2.dp))
    }
}
