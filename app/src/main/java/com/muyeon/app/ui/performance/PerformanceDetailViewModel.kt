package com.muyeon.app.ui.performance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.navigation.Screen
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.performance.PerformanceDetail
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.usecase.performance.GetPerformanceDetailUseCase
import com.muyeon.domain.usecase.scrap.GetLikedIdsUseCase
import com.muyeon.domain.usecase.scrap.ToggleScrapResult
import com.muyeon.domain.usecase.scrap.ToggleScrapUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PerformanceDetailUiState(
    val detail: PerformanceDetail? = null,
    val isLoading: Boolean = true,
    /** 불러오지 못함 — 웹은 "공연을 찾을 수 없어요." 한 화면. */
    val notFound: Boolean = false,
    val liked: Boolean = false,
)

/** 공연 상세 — 웹 pages/muyeon/performance/PerformanceDetail.js. 경로 인자 [Screen.PerformanceDetail.ARG_ID]. */
class PerformanceDetailViewModel(
    savedState: SavedStateHandle,
    private val getDetail: GetPerformanceDetailUseCase,
    private val getLikedIds: GetLikedIdsUseCase,
    private val toggleScrap: ToggleScrapUseCase,
) : ViewModel() {
    private val id: Long = savedState.get<String>(Screen.PerformanceDetail.ARG_ID)?.toLongOrNull()
        ?: savedState.get<Long>(Screen.PerformanceDetail.ARG_ID) ?: 0L

    private val _state = MutableStateFlow(PerformanceDetailUiState())
    val state: StateFlow<PerformanceDetailUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PerformanceListEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<PerformanceListEvent> = _events.asSharedFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, notFound = false) }
        viewModelScope.launch {
            when (val r = getDetail(id)) {
                is ApiResult.Success -> _state.update { it.copy(detail = r.data, isLoading = false) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, notFound = true) }
            }
            val liked = id in getLikedIds(ScrapTarget.PERFORMANCE)
            _state.update { it.copy(liked = liked) }
        }
    }

    fun toggleLike() {
        val was = _state.value.liked
        viewModelScope.launch {
            when (val r = toggleScrap(ScrapTarget.PERFORMANCE, id, was)) {
                is ToggleScrapResult.Done -> {
                    _state.update { it.copy(liked = r.liked) }
                    _events.tryEmit(PerformanceListEvent.ScrapToast(r.liked))
                }
                is ToggleScrapResult.Failed -> _events.tryEmit(PerformanceListEvent.Message(r.message))
                ToggleScrapResult.LoginRequired -> _events.tryEmit(PerformanceListEvent.LoginRequired)
            }
        }
    }
}
