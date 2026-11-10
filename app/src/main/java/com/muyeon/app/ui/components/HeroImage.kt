package com.muyeon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 상세 대표 사진 칸 비율 — 화면마다 숫자를 따로 쓰지 않게. */
object HeroRatio {
    const val WIDE = 4f / 3f    // 레슨·공간·양도·구인 같은 가로 사진
    const val SQUARE = 1f       // 공연 포스터·커뮤니티·무용수(웹 1:1)
}

/**
 * 상세 대표 사진 — 사진은 자르지 않고 전체를 보여 준다(BlurFitImage: 남는 양옆·위아래는 같은 사진을 흐리게).
 *   비율 칸은 [ratio] 로 고정. 2장 이상이면 넘김 + 오른쪽 아래 "1 / N". 누르면 [onOpen](몇 번째).
 *   사진이 없으면 [empty] 를 칸 안에 그린다(기본은 회색 칸).
 */
@Composable
fun HeroImage(
    images: List<String>,
    modifier: Modifier = Modifier,
    ratio: Float = HeroRatio.WIDE,
    onOpen: ((Int) -> Unit)? = null,
    empty: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.fillMaxWidth().aspectRatio(ratio).background(AppColors.SurfaceMuted), Alignment.Center) {
        if (images.isEmpty()) {
            empty()
            return@Box
        }
        val pager = rememberPagerState { images.size }
        HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
            BlurFitImage(
                images[i],
                Modifier.fillMaxSize().clickable(enabled = onOpen != null) { onOpen?.invoke(i) },
            )
        }
        if (images.size > 1) HeroCounter(pager.currentPage + 1, images.size, Modifier.align(Alignment.BottomEnd))
    }
}

/** "1 / N" 작은 알약 — 대표 사진 오른쪽 아래. */
@Composable
private fun HeroCounter(current: Int, total: Int, modifier: Modifier) {
    Text(
        "$current / $total",
        style = AppTypography.Caption12,
        color = Color.White,
        modifier = modifier.padding(12.dp).clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
