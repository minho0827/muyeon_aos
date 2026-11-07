package com.muyeon.app.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/**
 * 홈 섹션 머리 — 웹 Home Section(.section-head): 제목 16 bold(아이콘 20 선택) · 오른쪽 "더보기 >"(14 회색). 여백 0 20 14.
 */
@Composable
fun HomeSectionHead(title: String, modifier: Modifier = Modifier, iconRes: Int? = null, onMore: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (iconRes != null) Icon(painterResource(iconRes), null, tint = Color.Unspecified, modifier = Modifier.padding(end = 4.dp).size(20.dp))
        Text(title, style = AppTypography.Section16, color = AppColors.TextHead, modifier = Modifier.weight(1f))
        if (onMore != null) HomeMoreButton(onMore)
    }
}

/** "더보기 >" — 웹 .section-more-btn(14 회색 · 화살표 16, 60% 진하기). */
@Composable
fun HomeMoreButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.home_more), style = AppTypography.More14, color = AppColors.TextSub)
        Icon(painterResource(R.drawable.ic_web_arrow_right_bold), null, tint = AppColors.TextSub, modifier = Modifier.size(16.dp).alpha(0.6f))
    }
}

/** 카드 폭 — 화면 폭 비율(웹 vw) 또는 상한(min(40vw, 20rem)). */
@Composable
fun slideWidth(fraction: Float, max: Dp? = null): Dp {
    val w = LocalConfiguration.current.screenWidthDp.dp * fraction
    return if (max != null) min(w, max) else w
}

/**
 * 홈 가로 넘김 섹션 — 웹 Section(Swiper slidesPerView auto · 간격 8 · 좌우 20 · 카드 단위로 멈춤).
 *   [fit] 이면 1~2개일 때 넘기지 않고 화면 폭을 나눠 쓴다(웹 fit — 공간 대관·양도양수).
 *   비어 있으면 [empty] 가 있을 때만 그것을, 없으면 섹션 전체를 그리지 않는다.
 */
@Composable
fun <T> HomeCarouselSection(
    title: String,
    items: List<T>,
    key: (T) -> Any,
    cardWidth: Dp,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    onMore: (() -> Unit)? = null,
    fit: Boolean = false,
    empty: (@Composable () -> Unit)? = null,
    card: @Composable (T, Modifier) -> Unit,
) {
    if (items.isEmpty() && empty == null) return
    Column(modifier.fillMaxWidth()) {
        HomeSectionHead(title, iconRes = iconRes, onMore = onMore)
        when {
            items.isEmpty() -> Row(Modifier.padding(horizontal = 20.dp)) { empty?.invoke() }
            fit && items.size <= 2 -> Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { card(it, Modifier.weight(1f)) }
            }
            else -> {
                val state = rememberLazyListState()
                LazyRow(
                    state = state,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    flingBehavior = rememberSnapFlingBehavior(state),
                ) { items(items, key = key) { card(it, Modifier.width(cardWidth)) } }
            }
        }
    }
}
