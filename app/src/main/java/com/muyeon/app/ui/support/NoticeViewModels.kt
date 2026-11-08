package com.muyeon.app.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.support.Notice
import com.muyeon.domain.usecase.support.GetNoticesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoticeListUiState(
    val items: List<Notice> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** 실패(빈 목록과 다른 화면). */
    val errorMessage: String? = null,
)

/** 공지사항 목록 — 웹 pages/notice/Notice.js(고정 먼저 → 최신, 서버 순서 그대로). */
class NoticeListViewModel(private val getNotices: GetNoticesUseCase) : ViewModel() {
    private val _state = MutableStateFlow(NoticeListUiState())
    val state: StateFlow<NoticeListUiState> = _state.asStateFlow()

    init { load() }

    fun load(pull: Boolean = false) {
        _state.update { it.copy(isLoading = !pull && it.items.isEmpty(), isRefreshing = pull, errorMessage = null) }
        viewModelScope.launch {
            when (val r = getNotices()) {
                is ApiResult.Success -> _state.update { it.copy(items = r.data, isLoading = false, isRefreshing = false) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = r.message) }
            }
        }
    }
}

data class NoticeDetailUiState(
    val notice: Notice? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/** 공지 상세 — 웹 NoticeDetail.js. */
class NoticeDetailViewModel(private val id: Long, private val getNotices: GetNoticesUseCase) : ViewModel() {
    private val _state = MutableStateFlow(NoticeDetailUiState())
    val state: StateFlow<NoticeDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = getNotices.detail(id)) {
                is ApiResult.Success -> _state.update { it.copy(notice = r.data, isLoading = false) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, errorMessage = r.message) }
            }
        }
    }
}
