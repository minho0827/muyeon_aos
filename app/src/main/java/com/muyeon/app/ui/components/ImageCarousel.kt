package com.muyeon.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.Dimensions

/**
 * 상세 대표 사진 — 웹 components/muyeon/ImageCarousel.js. 1장이면 한 장, 2장 이상이면 넘김 + 점. 누르면 크게 보기.
 *   사진 비율은 웹 썸네일 박스와 같은 1:1. 비어 있으면 [fallbackRes] 한 장.
 */
@Composable
fun ImageCarousel(
    images: List<String>,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes fallbackRes: Int? = null,
) {
    if (images.isEmpty() && fallbackRes == null) return
    val pager = rememberPagerState { images.size.coerceAtLeast(1) }
    Box(modifier.fillMaxWidth().aspectRatio(1f)) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            RemoteImage(
                url = images.getOrNull(page),
                fallbackRes = fallbackRes,
                modifier = Modifier.fillMaxSize().clickable(enabled = images.isNotEmpty()) { onOpen(page) },
            )
        }
        if (images.size > 1) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = Dimensions.spacingM),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(images.size) { i ->
                    Box(Modifier.size(6.dp).background(if (i == pager.currentPage) AppColors.Surface else AppColors.Surface.copy(alpha = 0.5f), CircleShape))
                }
            }
        }
    }
}
