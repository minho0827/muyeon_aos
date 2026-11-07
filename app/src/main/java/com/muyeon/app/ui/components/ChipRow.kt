package com.muyeon.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 칩 한 개 값. */
data class ChipOption(val value: String, val text: String)

/**
 * 칩 필터 한 줄 — 웹 ListPage `ChipRow`(.tabs.type-chip). 가로로 밀린다.
 *   칩: 높이 36 · 알약 · 1px #D0D0D0 · 14 medium. 선택 = 검정 바탕 흰 글자 bold. 칩 사이 6.
 *   고른 칩이 화면 밖이면 보이게 민다(탭 다시 누르기로 다음 칩이 될 때).
 */
@Composable
fun ChipRow(
    options: List<ChipOption>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = Dimensions.gutter),
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selected, options) {
        val i = options.indexOfFirst { it.value == selected }
        if (i >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == i && it.offset >= 0 }) listState.animateScrollToItem(i)
    }
    LazyRow(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(CHIP_GAP),
    ) {
        items(options, key = { it.value }) { o ->
            Chip(text = o.text, selected = o.value == selected, onClick = { onSelect(o.value) }, modifier = Modifier.testTag("chip_${o.value}"))
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(Dimensions.chipHeight)
            .clip(CircleShape)
            .then(if (selected) Modifier.background(AppColors.ChipSelectedBg) else Modifier.border(BorderStroke(ONE_PX, AppColors.ChipBorder), CircleShape))
            .clickable(onClick = onClick)
            .padding(horizontal = Dimensions.chipPaddingH),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = if (selected) AppTypography.Chip14Selected else AppTypography.Chip14,
            color = if (selected) AppColors.TextOnPrimary else AppColors.ChipText,
            maxLines = 1,
        )
    }
}

private val CHIP_GAP = androidx.compose.ui.unit.Dp(6f)
private val ONE_PX = androidx.compose.ui.unit.Dp(1f)
