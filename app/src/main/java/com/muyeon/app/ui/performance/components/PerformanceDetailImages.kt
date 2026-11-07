package com.muyeon.app.ui.performance.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.muyeon.app.ui.format.ImageUrls

/** 상세를 그림으로 올린 공연(detailType=IMAGE) — 그림을 폭에 맞춰 위아래로 잇는다(웹과 같다). */
@Composable
fun PerformanceDetailImages(images: List<String>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        images.forEach { src ->
            AsyncImage(model = ImageUrls.resolve(src), contentDescription = null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
        }
    }
}
