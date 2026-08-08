package com.muyeon.app.ui.quote

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

/**
 * 공용 덱 스와이프 페이저 — iOS `DeckPagerView.swift` 1:1.
 *  - 모든 카드 중앙 정렬, 이웃 카드는 음수 겹침(overlap)으로 현재 카드 '뒤에 깔림'
 *  - 카드 중심의 화면중앙 거리로 scale(1.0→0.90)·opacity(1.0→0.70) 연속 보간
 *
 *  iOS 는 DragGesture + 55pt 스냅을 직접 구현했지만, Compose 는 HorizontalPager 가
 *  같은 스냅 동작을 제공하므로 그쪽을 쓴다(수치 = edgeMargin 10 / overlap 40 / scale·alpha 동일).
 */
@Composable
fun <T> DeckPager(
    items: List<T>,
    state: PagerState,
    modifier: Modifier = Modifier,
    edgeMargin: Dp = 10.dp,
    overlap: Dp = 40.dp,
    card: @Composable (T, Boolean) -> Unit,
) {
    HorizontalPager(
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = edgeMargin),
        pageSpacing = -overlap,
        beyondViewportPageCount = 1,
    ) { page ->
        val pct = ((state.currentPage - page) + state.currentPageOffsetFraction)
            .absoluteValue.coerceIn(0f, 1f)
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = 1f - 0.10f * pct
                scaleY = 1f - 0.10f * pct
                alpha = 1f - 0.30f * pct
                // 현재 카드가 위로(iOS zIndex = -abs(i - index))
                translationX = 0f
            },
        ) {
            card(items[page], pct < 0.5f)
        }
    }
}
