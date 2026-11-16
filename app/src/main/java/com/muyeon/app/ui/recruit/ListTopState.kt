package com.muyeon.app.ui.recruit

import androidx.compose.foundation.lazy.grid.LazyGridState

/**
 * ★ 2026-11-16 목록이 맨 위인가 — 탭 다시 누르기(내려가 있으면 맨 위로, 맨 위면 옆 탭으로)에 쓴다.
 *  첫 칸이 보이고 아주 조금(threshold px)만 내려간 것까지 맨 위로 본다.
 */
fun LazyGridState.isAtTop(threshold: Int = 48): Boolean =
    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset <= threshold
