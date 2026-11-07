package com.muyeon.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.muyeon.app.R

/** 동그란 얼굴 사진 — 웹 components/muyeon/Avatar.js. 사진이 없으면 기본 그림. [onClick] 이 있으면 누를 수 있다. */
@Composable
fun Avatar(url: String?, size: Dp, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    RemoteImage(
        url = url,
        fallbackRes = R.drawable.img_noimage_teacher,
        thumbnail = true,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    )
}
