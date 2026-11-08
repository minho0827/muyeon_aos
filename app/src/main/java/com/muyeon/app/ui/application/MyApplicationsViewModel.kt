package com.muyeon.app.ui.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.navigation.collectAppResults
import com.muyeon.app.result.ResultKeys
import com.muyeon.app.ui.common.ListFilter
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorState
import com.muyeon.domain.common.fetchCursorPages
import com.muyeon.domain.common.mergeCursorPage
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.model.application.MyApplication
import com.muyeon.domain.model.application.MyApplicationTab
import com.muyeon.domain.usecase.application.MyApplicationsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyApplicationsUiState(
    val tab: MyApplicationTab = MyApplicationTab.ALL,
    val filter: ListFilter = ListFilter(),
    val counts: Map<MyApplicationTab, Int> = emptyMap(),
    val items: List<MyApplication> = emptyList(),
    val cursor: CursorState = CursorState.Initial,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val moreError: String? = null,
    /** … 메뉴를 연 줄. */
    val menuFor: MyApplication? = null,
    val withdrawTarget: MyApplication? = null,
    val deleteTarget: MyApplication? = null,
    val working: Boolean = false,
)

sealed interface MyApplicationsEvent {
    data class Message(val text: String) : MyApplicationsEvent
    data class MessageRes(val res: Int) : MyApplicationsEvent
}

/**
 * 내가 지원한 공고 — 웹 pages/muyeon/my/MyApplications.js. 상태 칩(전체·진행 중·합격·미선정 + 개수) · 기간 필터 · 커서 목록.
 *   … 메뉴: 지원 취소(확정 전) · 내 목록에서 삭제(상대 기록은 남는다). 둘 다 확인 팝업 뒤.
 */
class MyApplicationsViewModel(private val apps: MyApplicationsUseCase) : ViewModel() {
    private val _state = MutableStateFlow(MyApplicationsUiState())
    val state: StateFlow<MyApplicationsUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<MyApplicationsEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<MyApplicationsEvent> = _events.asSharedFlow()
    private var job: Job? = null

    init {
        refresh()
        viewModelScope.collectAppResults(ResultKeys.JOB_POSTINGS, ResultKeys.RESUME) { refresh() }
    }

    fun selectTab(t: MyApplicationTab) {
        if (t == _state.value.tab) return
        _state.update { it.copy(tab = t, items = emptyList(), cursor = CursorState.Initial) }
        refresh()
    }

    fun setFilter(f: ListFilter) {
        if (f == _state.value.filter) return
        _state.update { it.copy(filter = f, items = emptyList(), cursor = CursorState.Initial) }
        refresh()
    }

    private suspend fun fetch(s: MyApplicationsUiState, cursor: String?): ApiResult<CursorPage<MyApplication>> =
        apps.page(s.tab, s.filter.period.param, cursor).map { p ->
            if (p.counts.isNotEmpty()) _state.update { it.copy(counts = p.counts) }
            CursorPage(p.items, p.nextCursor)
        }

    fun refresh(pull: Boolean = false) {
        job?.cancel()
        _state.update { it.copy(isInitialLoading = !pull && it.items.isEmpty(), isRefreshing = pull, errorMessage = null, moreError = null) }
        val s = _state.value
        job = viewModelScope.launch {
            val f = fetchCursorPages(null, emptySet<String>(), { it.key }) { c -> fetch(s, c) }
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
            val f = fetchCursorPages(s.cursor.nextCursor, s.items.mapTo(HashSet()) { it.key }, { it.key }) { c -> fetch(s, c) }
            _state.update {
                it.copy(items = mergeCursorPage(it.items, f.items, replace = false) { x -> x.key },
                    cursor = it.cursor.applied(f), isLoadingMore = false, moreError = f.error)
            }
        }
    }

    fun openMenu(a: MyApplication?) = _state.update { it.copy(menuFor = a) }
    fun askWithdraw(a: MyApplication?) = _state.update { it.copy(withdrawTarget = a, menuFor = null) }
    fun askDelete(a: MyApplication?) = _state.update { it.copy(deleteTarget = a, menuFor = null) }

    fun withdraw() {
        val t = _state.value.withdrawTarget ?: return
        if (_state.value.working) return
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            when (val r = apps.withdraw(t.kind, t.id)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(withdrawTarget = null) }
                    _events.tryEmit(MyApplicationsEvent.MessageRes(com.muyeon.app.R.string.application_withdrawn))
                    refresh()
                }
                is ApiResult.Error -> _events.tryEmit(MyApplicationsEvent.Message(r.message))
            }
            _state.update { it.copy(working = false) }
        }
    }

    fun delete() {
        val t = _state.value.deleteTarget ?: return
        _state.update { s -> s.copy(deleteTarget = null, items = s.items.filterNot { it.key == t.key }) }
        viewModelScope.launch {
            when (val r = apps.archive(t.kind, t.id)) {
                is ApiResult.Success -> { _events.tryEmit(MyApplicationsEvent.MessageRes(com.muyeon.app.R.string.application_deleted)); refresh() }
                is ApiResult.Error -> { _events.tryEmit(MyApplicationsEvent.Message(r.message)); refresh() }
            }
        }
    }
}
