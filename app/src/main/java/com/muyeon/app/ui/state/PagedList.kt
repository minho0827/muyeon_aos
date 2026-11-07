package com.muyeon.app.ui.state

import com.muyeon.domain.common.CursorState

/** 커서 목록 한 덩어리(홈 강사 그리드 · 레슨 장르 목록 · 채용 탭 목록) — 처음 읽기·더 읽기·실패를 따로 둔다. */
data class PagedList<T>(
    val items: List<T> = emptyList(),
    val cursor: CursorState = CursorState.Initial,
    val isInitialLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val moreError: String? = null,
)
