package com.muyeon.app.ui.quotefeed.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.home.components.HomeSectionHeader
import com.muyeon.domain.model.quotefeed.QuoteFeedItem

/**
 * 홈 [레슨] '레슨을 찾는 회원들' — 레슨 섹션([com.muyeon.app.ui.home.components.LessonSection])과 같은 머리·간격·카드 폭(150) 가로 줄.
 *   비면 그리지 않는다(비회원은 서버 공개 목록이 없어 늘 빈다).
 *
 * @param sentOf 이미 제안을 보낸 요청인지(강사·학원 활동 중일 때만 의미)
 */
@Composable
fun QuoteFeedSection(
    items: List<QuoteFeedItem>,
    sentOf: (QuoteFeedItem) -> Boolean,
    onOpen: (QuoteFeedItem) -> Unit,
    onAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier.fillMaxWidth().padding(top = 28.dp)) {
        HomeSectionHeader(stringResource(R.string.quote_feed_title), stringResource(R.string.quote_feed_desc), onAll)
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { q ->
                QuoteFeedCard(q, sentOf(q), onClick = { onOpen(q) }, modifier = Modifier.width(150.dp))
            }
        }
    }
}
