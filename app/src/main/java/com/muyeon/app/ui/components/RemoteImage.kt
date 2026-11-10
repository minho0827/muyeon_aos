package com.muyeon.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.muyeon.app.ui.format.ImageUrls

/**
 * 서버 사진 — 상대 주소 처리·(목록) 썸네일·없을 때 기본 그림을 한 곳에서. Coil 3.
 *   [fallbackRes] 는 주소가 없거나 받지 못했을 때(웹 NO_IMAGE_*).
 *   ★ 11-10 불러오는 중엔 회색 상자만(imageLoadingPainter) — 기본 그림은 없음·실패 때만. 그림이 없으면 실패도 회색.
 */
@Composable
fun RemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    @DrawableRes fallbackRes: Int? = null,
    thumbnail: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    contentDescription: String? = null,
) {
    val resolved = if (thumbnail) ImageUrls.thumbnail(url) else ImageUrls.resolve(url)
    val loading = imageLoadingPainter()
    val fallback = fallbackRes?.let { painterResource(it) } ?: loading
    AsyncImage(
        model = resolved,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
        placeholder = loading,
        error = fallback,
        fallback = fallback,
    )
}
