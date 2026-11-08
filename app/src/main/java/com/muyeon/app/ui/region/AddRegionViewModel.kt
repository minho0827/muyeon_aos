package com.muyeon.app.ui.region

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.region.RegionEditResult
import com.muyeon.domain.model.region.RegionEntry
import com.muyeon.domain.usecase.region.AddRegionOptionsUseCase
import com.muyeon.domain.usecase.region.MyRegionsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddRegionUiState(
    val groups: List<Pair<String, List<RegionEntry>>> = emptyList(),
    val sido: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** 5개 넘음 팝업. */
    val maxReached: Boolean = false,
) {
    val items: List<RegionEntry> get() = groups.firstOrNull { it.first == sido }?.second.orEmpty()
    /** 수도권만 시군구까지(웹 isMetro), 그 밖은 'OO 전체' 한 장. */
    val isMetro: Boolean get() = sido in AddRegionOptionsUseCase.METRO
}

sealed interface AddRegionEvent { data object Added : AddRegionEvent }

/** 지역 추가 — 웹 pages/muyeon/region/AddRegion.js(시·도 칩 → 'OO 전체' + 수도권은 시군구 목록, 누르면 등록하고 목록으로). */
class AddRegionViewModel(private val options: AddRegionOptionsUseCase, private val regions: MyRegionsUseCase) : ViewModel() {
    private val _state = MutableStateFlow(AddRegionUiState())
    val state: StateFlow<AddRegionUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<AddRegionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AddRegionEvent> = _events.asSharedFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = options()) {
                is ApiResult.Success -> _state.update { it.copy(groups = r.data, sido = it.sido.ifEmpty { r.data.firstOrNull()?.first.orEmpty() }, isLoading = false) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, errorMessage = r.message) }
            }
        }
    }

    fun selectSido(s: String) = _state.update { it.copy(sido = s) }

    fun add(item: ActiveRegion) {
        if (regions.add(item) == RegionEditResult.MAX) { _state.update { it.copy(maxReached = true) }; return }
        _events.tryEmit(AddRegionEvent.Added)
    }

    fun dismissMax() = _state.update { it.copy(maxReached = false) }
}
