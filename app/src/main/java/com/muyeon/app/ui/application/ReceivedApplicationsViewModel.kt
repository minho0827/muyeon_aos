package com.muyeon.app.ui.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.navigation.collectAppResults
import com.muyeon.app.result.ResultKeys
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.common.CursorState
import com.muyeon.domain.common.fetchCursorPages
import com.muyeon.domain.common.mergeCursorPage
import com.muyeon.domain.model.application.ReceivedApplication
import com.muyeon.domain.usecase.application.ReceivedApplicationsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceivedApplicationsUiState(
    /** 휴지통 보기. */
    val trash: Boolean = false,
    val archivedCount: Int = 0,
    val items: List<ReceivedApplication> = emptyList(),
    val cursor: CursorState = CursorState.Initial,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val moreError: String? = null,
    val purgeTarget: ReceivedApplication? = null,
)

sealed interface ReceivedApplicationsEvent {
    data class Message(val text: String) : ReceivedApplicationsEvent
    data class MessageRes(val res: Int) : ReceivedApplicationsEvent
}

/**
 * 받은 지원 — 웹 pages/muyeon/my/ReceivedApplications.js(?kind=&postingId= 로 공고 하나만).
 *   밀어서 휴지통으로 · 휴지통에서 복원 / 밀어서 영구 삭제(확인 팝업). 이력서를 보고 돌아오면 다시 읽는다(열람 표시).
 */
class ReceivedApplicationsViewModel(
    private val kind: String?,
    private val postingId: Long?,
    private val apps: ReceivedApplicationsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ReceivedApplicationsUiState())
    val state: StateFlow<ReceivedApplicationsUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<ReceivedApplicationsEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ReceivedApplicationsEvent> = _events.asSharedFlow()
    private var job: Job? = null

    init {
        refresh()
        viewModelScope.collectAppResults(ResultKeys.JOB_POSTINGS, ResultKeys.RESUME) { refresh() }
    }

    fun toggleTrash() {
        _state.update { it.copy(trash = !it.trash, items = emptyList(), cursor = CursorState.Initial) }
        refresh()
    }

    private suspend fun fetch(trash: Boolean, cursor: String?): ApiResult<CursorPage<ReceivedApplication>> =
        apps.page(kind, postingId, trash, cursor).map { p ->
            _state.update { it.copy(archivedCount = p.archivedCount) }
            CursorPage(p.items, p.nextCursor)
        }

    fun refresh(pull: Boolean = false) {
        job?.cancel()
        _state.update { it.copy(isInitialLoading = !pull && it.items.isEmpty(), isRefreshing = pull, errorMessage = null, moreError = null) }
        val trash = _state.value.trash
        job = viewModelScope.launch {
            val f = fetchCursorPages(null, emptySet<String>(), { it.key }) { c -> fetch(trash, c) }
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
            val f = fetchCursorPages(s.cursor.nextCursor, s.items.mapTo(HashSet()) { it.key }, { it.key }) { c -> fetch(s.trash, c) }
            _state.update {
                it.copy(items = mergeCursorPage(it.items, f.items, replace = false) { x -> x.key },
                    cursor = it.cursor.applied(f), isLoadingMore = false, moreError = f.error)
            }
        }
    }

    private fun drop(a: ReceivedApplication, delta: Int) =
        _state.update { s -> s.copy(items = s.items.filterNot { it.key == a.key }, archivedCount = (s.archivedCount + delta).coerceAtLeast(0)) }

    private fun act(a: ReceivedApplication, delta: Int, ok: Int, call: suspend () -> ApiResult<Unit>) {
        drop(a, delta)
        viewModelScope.launch {
            when (val r = call()) {
                is ApiResult.Success -> _events.tryEmit(ReceivedApplicationsEvent.MessageRes(ok))
                is ApiResult.Error -> { _events.tryEmit(ReceivedApplicationsEvent.Message(r.message)); refresh() }
            }
        }
    }

    fun archive(a: ReceivedApplication) = act(a, +1, com.muyeon.app.R.string.received_archived) { apps.archive(a.kind, a.id) }
    fun restore(a: ReceivedApplication) = act(a, -1, com.muyeon.app.R.string.received_restored) { apps.restore(a.kind, a.id) }
    fun askPurge(a: ReceivedApplication?) = _state.update { it.copy(purgeTarget = a) }
    fun purge() {
        val t = _state.value.purgeTarget ?: return
        _state.update { it.copy(purgeTarget = null) }
        act(t, -1, com.muyeon.app.R.string.received_purged) { apps.purge(t.kind, t.id) }
    }
}
