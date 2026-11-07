package com.muyeon.app.ui.recruit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.navigation.collectAppResults
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.recruit.SubFilter
import com.muyeon.domain.model.recruit.SubRow
import com.muyeon.domain.usecase.recruit.SubListUseCase
import com.muyeon.domain.usecase.region.ObserveActiveRegionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubsUiState(
    val items: List<SubRow> = emptyList(),
    val filter: SubFilter = SubFilter(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
)

/** 대타공고 목록 — 웹 SubList. 장르 칩 + 필터 시트(기간·타임 수·분야·대상·급여 — 한 개씩). 서버에 커서가 없어 한 번에 받는다. */
class SubsViewModel(private val subs: SubListUseCase, observeRegion: ObserveActiveRegionUseCase) : ViewModel() {
    private val region = observeRegion()
    private val _state = MutableStateFlow(SubsUiState())
    val state: StateFlow<SubsUiState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        refresh()
        viewModelScope.launch { region.drop(1).collect { refresh() } }
        // 설문 등록으로 새 공고를 올리면 다시 받는다.
        viewModelScope.collectAppResults(com.muyeon.app.result.ResultKeys.JOB_POSTINGS) { refresh() }
    }

    fun selectGenre(g: String?) { _state.update { it.copy(filter = it.filter.copy(genre = g)) }; refresh() }
    fun applyFilter(f: SubFilter) { _state.update { it.copy(filter = f) }; refresh() }

    fun refresh(pull: Boolean = false) {
        job?.cancel()
        _state.update { it.copy(isRefreshing = pull, loading = it.items.isEmpty() && !pull) }
        val f = _state.value.filter
        job = viewModelScope.launch {
            when (val r = subs(region.value, f)) {
                is ApiResult.Success -> _state.update { it.copy(items = r.data, loading = false, isRefreshing = false, errorMessage = null) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, isRefreshing = false, errorMessage = r.message) }
            }
        }
    }
}
