package com.muyeon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors

/** 그리드/목록 전환 — 웹 .view-select-btn-group(고른 쪽 회색 바탕 32 · 모서리 8). */
@Composable
fun ViewModeToggle(grid: Boolean, onGrid: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Item(R.drawable.ic_web_square, stringResource(R.string.lesson_view_grid), grid) { onGrid(true) }
        Item(R.drawable.ic_web_menu, stringResource(R.string.lesson_view_list), !grid) { onGrid(false) }
    }
}

@Composable
private fun Item(iconRes: Int, desc: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).background(if (selected) AppColors.Divider else AppColors.Surface, RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(painterResource(iconRes), contentDescription = desc, tint = AppColors.TextHead, modifier = Modifier.size(32.dp)) }
}
