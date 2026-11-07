package com.muyeon.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.muyeon.app.ui.format.ImageUrls
import com.muyeon.app.ui.theme.AppColors

/**
 * 서버 사진 — 상대 주소 처리·(목록) 썸네일·없을 때 기본 그림을 한 곳에서. Coil 3.
 *   [fallbackRes] 는 주소가 없거나 받지 못했을 때(웹 NO_IMAGE_*).
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
    val fallback = fallbackRes?.let { painterResource(it) }
    AsyncImage(
        model = resolved,
        contentDescription = contentDescription,
        modifier = modifier.background(AppColors.SurfaceMuted),
        contentScale = contentScale,
        alignment = alignment,
        error = fallback,
        fallback = fallback,
    )
}
