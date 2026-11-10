package com.muyeon.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 상세 대표 사진 — 웹 components/muyeon/ImageCarousel.js. 칸은 웹과 같은 1:1.
 *   사진은 자르지 않고 전체(HeroImage). 비어 있으면 [fallbackRes] 한 장, 그것도 없으면 그리지 않는다.
 */
@Composable
fun ImageCarousel(
    images: List<String>,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes fallbackRes: Int? = null,
) {
    if (images.isEmpty() && fallbackRes == null) return
    HeroImage(images, modifier, ratio = HeroRatio.SQUARE, onOpen = onOpen) {
        RemoteImage(null, Modifier.fillMaxSize(), fallbackRes = fallbackRes)
    }
}
