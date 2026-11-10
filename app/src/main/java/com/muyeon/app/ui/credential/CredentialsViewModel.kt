package com.muyeon.app.ui.credential

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.navigation.AppResults
import com.muyeon.app.result.ResultKeys
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.credential.CredentialDocument
import com.muyeon.domain.model.credential.CredentialItem
import com.muyeon.domain.model.credential.MyCredentials
import com.muyeon.domain.usecase.credential.CredentialsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 고른 서류 한 장(미리보기용) — [uri] 문자열 · PDF 면 미리보기 대신 'PDF'. */
data class PickedDoc(val uri: String, val isPdf: Boolean)

data class CredentialsUiState(
    val data: MyCredentials? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** 서류를 고르는 항목. */
    val editing: String? = null,
    val files: List<PickedDoc> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
)

/**
 * 경력·자격 확인 — 웹 pages/muyeon/my/CredentialsManage.js. 본인인증 상태 · 항목별 상태(미등록·확인 중·확인 완료·반려) ·
 *   [서류 등록]/[다시 등록] → 사진·PDF 최대 5장 → [등록하기](하나씩 올려 key 를 모아 제출).
 */
class CredentialsViewModel(private val credentials: CredentialsUseCase) : ViewModel() {
    private val _state = MutableStateFlow(CredentialsUiState())
    val state: StateFlow<CredentialsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = credentials.load()) {
                is ApiResult.Success -> _state.update { it.copy(data = r.data, isLoading = false) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, errorMessage = r.message) }
            }
        }
    }

    fun startEdit(kind: String) = _state.update { it.copy(editing = kind, files = emptyList()) }
    fun cancelEdit() { if (!_state.value.busy) _state.update { it.copy(editing = null, files = emptyList()) } }

    fun addFiles(picked: List<PickedDoc>) =
        _state.update { it.copy(files = (it.files + picked).distinctBy { f -> f.uri }.take(CredentialsUseCase.MAX_FILES)) }

    fun removeFile(i: Int) = _state.update { it.copy(files = it.files.filterIndexed { idx, _ -> idx != i }) }

    /** 화면이 고른 파일을 읽어(이미지 줄이기·PDF 원본) 넘긴다. */
    fun submit(docs: List<CredentialDocument>) {
        val kind = _state.value.editing ?: return
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val r = credentials.submit(kind, docs)) {
                is ApiResult.Success -> {
                    AppResults.publish(setOf(ResultKeys.PROFILE))
                    _state.update { it.copy(data = r.data, editing = null, files = emptyList(), busy = false, message = SUBMITTED) }
                }
                is ApiResult.Error -> _state.update { it.copy(busy = false, message = r.message) }
            }
        }
    }

    /** ★ 11-10 신청 철회 — 서류는 서버가 지우고 배지도 빠진다. 성공이면 받은 현황으로 바꾼다. */
    fun withdraw(item: CredentialItem) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val r = credentials.withdraw(item.kind, item.id)) {
                is ApiResult.Success -> {
                    AppResults.publish(setOf(ResultKeys.PROFILE))
                    _state.update { it.copy(data = r.data, busy = false, message = WITHDRAWN) }
                }
                is ApiResult.Error -> _state.update { it.copy(busy = false, message = r.message) }
            }
        }
    }

    fun readFailed() = _state.update { it.copy(busy = false, message = READ_FAIL) }
    fun setBusy(b: Boolean) = _state.update { it.copy(busy = b) }
    fun dismissMessage() = _state.update { it.copy(message = null) }

    companion object {
        const val SUBMITTED = "서류가 등록됐어요. 확인이 끝나면 알림으로 알려 드릴게요(보통 1영업일)."
        const val WITHDRAWN = "철회했어요. 필요하면 다시 등록할 수 있어요."
        const val READ_FAIL = "파일을 읽지 못했어요. 10MB 이하 사진이나 PDF 로 다시 골라 주세요."
    }
}
