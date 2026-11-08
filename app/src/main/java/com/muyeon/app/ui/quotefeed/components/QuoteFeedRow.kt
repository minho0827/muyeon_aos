package com.muyeon.app.ui.quotefeed.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.ListRow
import com.muyeon.app.ui.components.Pill
import com.muyeon.app.ui.quotefeed.QuoteFeedLabels
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.quotefeed.QuoteFeedItem

/**
 * '레슨을 찾는 회원들' 목록 보기 한 줄 — 웹 AvailableQuotes ListRow(장르 배지 · 비회원 요청 배지 · 제목 · 지역)에
 *   요일·시간대를 더한다. 썸네일은 장르 그림.
 */
@Composable
fun QuoteFeedRow(item: QuoteFeedItem, sent: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val genre = QuoteFeedLabels.genre(item.categoryId) ?: stringResource(R.string.quote_feed_lesson)
    ListRow(
        title = QuoteFeedLabels.seeking(item.categoryId),
        onClick = onClick,
        modifier = modifier,
        sub = listOf(QuoteFeedLabels.region(item), QuoteFeedLabels.schedule(item), QuoteFeedLabels.meta(item)).filter { it.isNotBlank() }.joinToString(" · "),
        thumbFallbackRes = QuoteFeedLabels.genreImage(item.categoryId) ?: R.drawable.img_noimage,
        tags = {
            Pill(genre, CategoryColors.genre(genre))
            if (item.guestRequest) Pill(stringResource(R.string.quote_feed_guest), CategoryColors.Default, Modifier.padding(start = 4.dp))
            when {
                item.isMine -> Pill(stringResource(R.string.quote_feed_mine), CategoryColors.Default, Modifier.padding(start = 4.dp))
                sent -> Pill(stringResource(R.string.quote_feed_sent), CategoryColors.Default, Modifier.padding(start = 4.dp))
            }
        },
    )
}
