package com.muyeon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.muyeon.app.ui.format.ImageUrls

/**
 * 인스타 방식 사진 칸 — 사진은 자르지 않고 전체(Fit), 남는 양옆·위아래는 같은 사진을 크게 흐려 비슷한 색으로 채운다.
 *   공개 프로필 히어로(PublicProfileScreen ProfileHero)와 같은 모양. 흐림은 Android 12+ 에서만(그 아래는 같은 사진이 살짝 어둡게).
 */
@Composable
fun BlurFitImage(url: String?, modifier: Modifier = Modifier) {
    val resolved = ImageUrls.resolve(url)
    Box(modifier.background(Color.Black).clipToBounds()) {
        AsyncImage(resolved, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(1.2f).blur(24.dp), placeholder = com.muyeon.app.ui.components.imageLoadingPainter())
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.10f)))
        AsyncImage(resolved, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(), placeholder = com.muyeon.app.ui.components.imageLoadingPainter())
    }
}
