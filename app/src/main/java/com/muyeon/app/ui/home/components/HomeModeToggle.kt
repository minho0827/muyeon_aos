package com.muyeon.app.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.home.HomeMode
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** [강사 | 레슨] 알약 토글 — 컬리 [마켓컬리 | 뷰티컬리]와 같은 모양(회색 알약 안에 고른 쪽만 검정으로 채움). */
@Composable
fun HomeModeToggle(mode: HomeMode, onMode: (HomeMode) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(50)).background(AppColors.SurfaceMuted).padding(3.dp)) {
        Segment(stringResource(R.string.home_mode_teachers), mode == HomeMode.TEACHERS) { onMode(HomeMode.TEACHERS) }
        Segment(stringResource(R.string.home_mode_lessons), mode == HomeMode.LESSONS) { onMode(HomeMode.LESSONS) }
    }
}

@Composable
private fun Segment(text: String, on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) AppColors.TextHead else AppColors.SurfaceMuted, label = "segBg")
    val fg by animateColorAsState(if (on) AppColors.TextOnPrimary else AppColors.TextSub, label = "segFg")
    Box(
        Modifier.width(76.dp).height(34.dp).clip(RoundedCornerShape(50)).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppTypography.Body15Medium.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium), color = fg)
    }
}
