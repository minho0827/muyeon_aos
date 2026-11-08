package com.muyeon.app.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.alert.AlertOptions
import com.muyeon.domain.model.alert.AlertPrefs
import com.muyeon.domain.model.region.RegionEntry
import com.muyeon.domain.usecase.alert.AlertPrefsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlertSettingsUiState(
    val prefs: AlertPrefs = AlertPrefs(),
    val held: List<String> = emptyList(),
    /** 지역 코드 → 이름을 찾는 표(GET /regions). */
    val regionList: List<RegionEntry> = emptyList(),
    /** 고른 지역 이름 — 시트에서 고른 이름, 아니면 코드로 찾은 이름. */
    val regionName: String = "",
    val isLoading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val saveFailed: Boolean = false,
) {
    val types: List<String> get() = AlertOptions.typesFor(held)
    val showFields: Boolean get() = AlertOptions.showFields(held)
    val fieldsAsTeacher: Boolean get() = AlertOptions.fieldsAsTeacher(held)
    val showTargets: Boolean get() = AlertOptions.showTargets(held)
    val regionCode: String get() = prefs.regions.firstOrNull().orEmpty()
}

/**
 * 관심 조건 알림 — 웹 pages/muyeon/my/AlertSettings.js. 읽기 실패는 빈 조건으로 연다(웹 catch 무시).
 *   저장: 빈 배열 = 조건 해제. 성공해야만 '저장됐어요'(닫으면 MY), 실패면 '저장하지 못했어요'.
 */
class AlertSettingsViewModel(private val useCase: AlertPrefsUseCase) : ViewModel() {
    private val _state = MutableStateFlow(AlertSettingsUiState())
    val state: StateFlow<AlertSettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val form = useCase.load()
            _state.update { it.copy(prefs = form.prefs, held = form.heldTypes, isLoading = false) }
            val list = useCase.regionList()
            _state.update { it.copy(regionList = list, regionName = it.regionName.ifEmpty { nameOf(it.regionCode, list) }) }
        }
    }

    private fun toggled(l: List<String>, v: String) = if (v in l) l - v else l + v
    fun toggleType(v: String) = _state.update { it.copy(prefs = it.prefs.copy(types = toggled(it.prefs.types, v))) }
    fun toggleGenre(v: String) = _state.update { it.copy(prefs = it.prefs.copy(genres = toggled(it.prefs.genres, v))) }
    fun toggleField(v: String) = _state.update { it.copy(prefs = it.prefs.copy(fields = toggled(it.prefs.fields, v))) }
    fun toggleTarget(v: String) = _state.update { it.copy(prefs = it.prefs.copy(targets = toggled(it.prefs.targets, v))) }
    fun togglePush() = _state.update { it.copy(prefs = it.prefs.copy(push = !it.prefs.push)) }

    /** 지역 하나(시·도 전체면 2자리 코드) — 웹 RegionSelect onChange. 빈 코드 = 해제. */
    fun pickRegion(name: String, code: String?) =
        _state.update { it.copy(prefs = it.prefs.copy(regions = listOfNotNull(code?.takeIf { c -> c.isNotBlank() })), regionName = if (code.isNullOrBlank()) "" else name) }

    fun save() {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val ok = useCase.save(_state.value.prefs) is ApiResult.Success
            _state.update { it.copy(saving = false, saved = ok, saveFailed = !ok) }
        }
    }

    fun dismissFailed() = _state.update { it.copy(saveFailed = false) }

    private fun nameOf(code: String, list: List<RegionEntry>): String {
        if (code.isBlank()) return ""
        list.firstOrNull { it.code == code }?.let { return it.name }
        return list.firstOrNull { it.code.startsWith(code) }?.let { "${it.sido} 전체" }.orEmpty()
    }
}
