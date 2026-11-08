package com.muyeon.app.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.support.SupportComposeType
import com.muyeon.domain.model.support.SupportInquiry
import com.muyeon.domain.usecase.support.MyInquiriesUseCase
import com.muyeon.domain.usecase.support.SendSupportUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InquiryListUiState(
    val items: List<SupportInquiry> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

/** 1:1 문의내역 — 웹 SupportInquiries.js(내 문의만, 최신순). 쓰고 돌아오면 [refresh]. */
class InquiryListViewModel(private val inquiries: MyInquiriesUseCase) : ViewModel() {
    private val _state = MutableStateFlow(InquiryListUiState())
    val state: StateFlow<InquiryListUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh(pull: Boolean = false) {
        _state.update { it.copy(isLoading = !pull && it.items.isEmpty(), isRefreshing = pull, errorMessage = null) }
        viewModelScope.launch {
            when (val r = inquiries.list()) {
                is ApiResult.Success -> _state.update { it.copy(items = r.data, isLoading = false, isRefreshing = false) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = r.message) }
            }
        }
    }

    /** 상세에서 지웠다 — 목록에서도 뺀다. */
    fun removed(id: Long) = _state.update { s -> s.copy(items = s.items.filterNot { it.id == id }) }
}

data class InquiryDetailUiState(
    val inquiry: SupportInquiry? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val confirmDelete: Boolean = false,
    val deleting: Boolean = false,
)

sealed interface InquiryDetailEvent {
    data object Deleted : InquiryDetailEvent
    data class Message(val text: String) : InquiryDetailEvent
}

/** 1:1 문의 상세 — 웹 SupportInquiryDetail.js(답변 완료 푸시의 착지점). 삭제는 웹 목록 밀어서 삭제와 같은 API. */
class InquiryDetailViewModel(private val id: Long, private val inquiries: MyInquiriesUseCase) : ViewModel() {
    private val _state = MutableStateFlow(InquiryDetailUiState())
    val state: StateFlow<InquiryDetailUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<InquiryDetailEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<InquiryDetailEvent> = _events.asSharedFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = inquiries.detail(id)) {
                is ApiResult.Success -> _state.update { it.copy(inquiry = r.data, isLoading = false) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, errorMessage = r.message) }
            }
        }
    }

    fun askDelete(ask: Boolean) = _state.update { it.copy(confirmDelete = ask) }

    fun delete() {
        if (_state.value.deleting) return
        _state.update { it.copy(confirmDelete = false, deleting = true) }
        viewModelScope.launch {
            when (val r = inquiries.delete(id)) {
                is ApiResult.Success -> _events.tryEmit(InquiryDetailEvent.Deleted)
                is ApiResult.Error -> _events.tryEmit(InquiryDetailEvent.Message(r.message))
            }
            _state.update { it.copy(deleting = false) }
        }
    }
}

data class SupportComposeUiState(
    val text: String = "",
    val sending: Boolean = false,
    val errorMessage: String? = null,
    val done: Boolean = false,
)

/** 1:1 문의 / 의견 쓰기 — 웹 SupportCompose.js(type inquiry|feedback). */
class SupportComposeViewModel(private val type: SupportComposeType, private val send: SendSupportUseCase) : ViewModel() {
    private val _state = MutableStateFlow(SupportComposeUiState())
    val state: StateFlow<SupportComposeUiState> = _state.asStateFlow()

    fun onText(t: String) = _state.update { it.copy(text = t.take(SendSupportUseCase.MAX), errorMessage = null) }

    /** 화면이 필수값 확인(RequiredForm)을 통과한 뒤에만 부른다. */
    fun submit() {
        val s = _state.value
        if (s.sending || s.text.isBlank()) return
        _state.update { it.copy(sending = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = send(type, s.text)) {
                is ApiResult.Success -> _state.update { it.copy(sending = false, done = true) }
                is ApiResult.Error -> _state.update { it.copy(sending = false, errorMessage = r.message) }
            }
        }
    }
}
