package com.muyeon.app.ui.recruit

import androidx.compose.foundation.lazy.grid.LazyGridState

/**
 * ★ 2026-11-16 목록이 맨 위인가 — 탭 다시 누르기(내려가 있으면 맨 위로, 맨 위면 옆 탭으로)에 쓴다.
 *  첫 칸이 보이고 아주 조금(threshold px)만 내려간 것까지 맨 위로 본다.
 */
fun LazyGridState.isAtTop(threshold: Int = 48): Boolean =
    // ★ 11-16 더 위로 못 올리면 맨 위 — 맨 앞 칸이 비어 높이 0 이면(홈 레슨 '레슨을 찾는 회원들' 없음) 첫 칸 번호가 1 이라
    //  예전 판정은 늘 '아님'이 되어 다시 눌러도 토글이 안 됐다(실기기 확인).
    !canScrollBackward || (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset <= threshold)
