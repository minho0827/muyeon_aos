package com.muyeon.app.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 사진 크기 — 웹 카드마다 다르다: 고정 높이 18.6rem 또는 비율(레슨 3:2). */
sealed interface HomeImageSize {
    data object Fixed : HomeImageSize
    data class Ratio(val ratio: Float) : HomeImageSize
}

/**
 * 홈 카드 틀 — 웹 .meal-menu-card: 사진(모서리 14) · 16 간격 · 본문(6 간격). 사진 위 겹침은 [overlay].
 *   제목은 [title](16 semibold), 그 밖 줄은 [body] 에 넣는다([HomeCardSub]).
 */
@Composable
fun HomeCard(
    imageUrl: String?,
    fallbackRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    image: HomeImageSize = HomeImageSize.Fixed,
    imageAlignment: Alignment = Alignment.Center,
    overlay: @Composable BoxScope.() -> Unit = {},
    body: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))) {
            RemoteImage(
                url = imageUrl, fallbackRes = fallbackRes, thumbnail = true, alignment = imageAlignment,
                modifier = when (image) {
                    HomeImageSize.Fixed -> Modifier.fillMaxWidth().height(IMAGE_H)
                    is HomeImageSize.Ratio -> Modifier.fillMaxWidth().aspectRatio(image.ratio)
                },
            )
            overlay()
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp), content = body)
    }
}

@Composable
fun HomeCardTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, style = AppTypography.CardTitle16, color = AppColors.TextHead, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = modifier)

/** 보조 줄 — 13 회색, 두 줄까지(웹 .sub). [strong] 이면 가격처럼 굵고 진하게 한 줄. */
@Composable
fun HomeCardSub(text: String, modifier: Modifier = Modifier, strong: Boolean = false, singleLine: Boolean = false) =
    Text(
        text,
        style = if (strong) AppTypography.Sub13SemiBold else AppTypography.Sub13,
        color = if (strong) AppColors.TextHead else AppColors.TextSub,
        maxLines = if (strong || singleLine) 1 else 2, overflow = TextOverflow.Ellipsis, modifier = modifier,
    )

private val IMAGE_H = 186.dp
