package com.muyeon.app.ui.reservation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.navigation.collectAppResults
import com.muyeon.app.result.ResultKeys
import com.muyeon.app.ui.common.ListFilter
import com.muyeon.domain.common.CursorState
import com.muyeon.domain.common.fetchCursorPages
import com.muyeon.domain.common.mergeCursorPage
import com.muyeon.domain.model.reservation.MyReservationItem
import com.muyeon.domain.model.reservation.ReservationCategory
import com.muyeon.domain.usecase.reservation.GetMyReservationsPageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyReservationsUiState(
    val category: ReservationCategory = ReservationCategory.PERSONAL,
    /** 기간 필터(웹 ListFilter PERIOD_SECTION — 화면을 떠나면 버린다). */
    val filter: ListFilter = ListFilter(),
    val items: List<MyReservationItem> = emptyList(),
    val cursor: CursorState = CursorState.Initial,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val moreError: String? = null,
)

/**
 * 내가 예약한 내역 — 웹 pages/muyeon/my/MyReservations.js. 분류 칩(개인레슨·학원 수업·레슨 완료·취소 내역) + 기간 필터 · 커서 목록.
 *   예약 상세(취소·회차 변경)·후기 쓰기가 돌려준 결과(RESERVATIONS·REVIEWS)로 다시 읽는다(웹 useResultRefresh).
 */
class MyReservationsViewModel(private val getPage: GetMyReservationsPageUseCase) : ViewModel() {
    private val _state = MutableStateFlow(MyReservationsUiState())
    val state: StateFlow<MyReservationsUiState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        refresh()
        viewModelScope.collectAppResults(ResultKeys.RESERVATIONS, ResultKeys.REVIEWS) { refresh() }
    }

    fun selectCategory(c: ReservationCategory) {
        if (c == _state.value.category) return
        _state.update { it.copy(category = c, items = emptyList(), cursor = CursorState.Initial) }
        refresh()
    }

    fun setFilter(f: ListFilter) {
        if (f == _state.value.filter) return
        _state.update { it.copy(filter = f, items = emptyList(), cursor = CursorState.Initial) }
        refresh()
    }

    fun refresh(pull: Boolean = false) {
        job?.cancel()
        _state.update { it.copy(isInitialLoading = !pull && it.items.isEmpty(), isRefreshing = pull, errorMessage = null, moreError = null) }
        val s = _state.value
        job = viewModelScope.launch {
            val f = fetchCursorPages(null, emptySet<String>(), { it.key }) { c -> getPage(s.category, s.filter.period.param, c) }
            _state.update {
                it.copy(
                    items = mergeCursorPage(it.items, f.items, replace = f.pages > 0) { x -> x.key },
                    cursor = it.cursor.applied(f), isInitialLoading = false, isRefreshing = false,
                    errorMessage = if (f.pages == 0) f.error else null,
                )
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoadingMore || s.isInitialLoading || !s.cursor.hasMore) return
        _state.update { it.copy(isLoadingMore = true, moreError = null, cursor = it.cursor.retrying()) }
        job = viewModelScope.launch {
            val f = fetchCursorPages(s.cursor.nextCursor, s.items.mapTo(HashSet()) { it.key }, { it.key }) { c -> getPage(s.category, s.filter.period.param, c) }
            _state.update {
                it.copy(
                    items = mergeCursorPage(it.items, f.items, replace = false) { x -> x.key },
                    cursor = it.cursor.applied(f), isLoadingMore = false, moreError = f.error,
                )
            }
        }
    }
}
