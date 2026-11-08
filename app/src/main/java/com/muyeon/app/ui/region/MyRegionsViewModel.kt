package com.muyeon.app.ui.region

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.muyeon.app.R
import com.muyeon.domain.model.region.RegionEditResult
import com.muyeon.domain.model.region.SavedRegions
import com.muyeon.domain.usecase.region.MyRegionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MyRegionsUiState(
    val saved: SavedRegions = SavedRegions(),
    val editMode: Boolean = false,
    /** 안내 팝업(최대 5개 · 대표 삭제 불가) — 웹 modal-msg. */
    @StringRes val message: Int? = null,
) {
    val canAdd: Boolean get() = saved.regions.size < SavedRegions.MAX
}

/** 관심 지역 — 웹 pages/muyeon/region/MyRegions.js(목록 · [지역 추가] · [지역 삭제] · 별 = 대표). 값은 기기(웹 muyeonRegions 와 같은 JSON). */
class MyRegionsViewModel(private val regions: MyRegionsUseCase) : ViewModel() {
    private val _state = MutableStateFlow(MyRegionsUiState(saved = regions.saved()))
    val state: StateFlow<MyRegionsUiState> = _state.asStateFlow()

    /** 지역 추가 화면에서 돌아올 때마다 다시 읽는다. */
    fun refresh() = _state.update { it.copy(saved = regions.saved()) }

    /** [지역 추가] — 5개면 안내만(웹 onAdd). true 면 추가 화면으로. */
    fun canOpenAdd(): Boolean {
        if (_state.value.canAdd) return true
        _state.update { it.copy(message = R.string.regions_max_notice) }
        return false
    }

    fun toggleEdit() = _state.update { it.copy(editMode = !it.editMode) }

    fun remove(code: String) {
        if (regions.remove(code) == RegionEditResult.REPRESENTATIVE) _state.update { it.copy(message = R.string.regions_rep_cannot_delete) }
        refresh()
    }

    fun setRepresentative(code: String) { regions.setRepresentative(code); refresh() }

    fun dismissMessage() = _state.update { it.copy(message = null) }
}
