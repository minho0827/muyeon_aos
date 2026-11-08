package com.muyeon.app.ui.quote.respond.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.theme.Dimensions

/** 첨부 사진 — 웹 .quote-photo-list(정사각 96 · 모서리 12 · 간격 8 · 줄바꿈). 누르면 크게 보기. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuotePhotoList(photos: List<String>, modifier: Modifier = Modifier, onOpen: (Int) -> Unit = {}) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS),
        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS),
    ) {
        photos.forEachIndexed { i, src ->
            RemoteImage(
                QuoteUi.imageUrl(src),
                Modifier.size(PHOTO).clip(RoundedCornerShape(Dimensions.spacingM)).clickable { onOpen(i) },
            )
        }
    }
}

private val PHOTO = 96.dp
