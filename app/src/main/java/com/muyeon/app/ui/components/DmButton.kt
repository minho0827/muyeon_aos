package com.muyeon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.app.ui.theme.Dimensions

/** 레슨 문의(종이비행기) 동그라미 — 웹 DmIcon 버튼(36 · #FDEBDD · 주황 선). 예시 프로필은 흐리게 막는다. */
@Composable
fun DmButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier
            .size(Dimensions.dmButton)
            .alpha(if (enabled) 1f else 0.45f)
            .background(CategoryColors.Dm.bg, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_web_dm), contentDescription = stringResource(R.string.lesson_request), tint = CategoryColors.Dm.fg, modifier = Modifier.size(18.dp))
    }
}
