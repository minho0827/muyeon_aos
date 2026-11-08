package com.muyeon.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.account.WithdrawResult
import com.muyeon.domain.model.account.WithdrawalInfo
import com.muyeon.domain.usecase.account.WithdrawUseCase
import com.muyeon.domain.usecase.my.LogoutUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WithdrawStep { INFO, REASON, CONFIRM }

data class WithdrawUiState(
    val info: WithdrawalInfo? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val step: WithdrawStep = WithdrawStep.INFO,
    val checked: Set<String> = emptySet(),
    val reasonCode: String = "",
    val reasonText: String = "",
    val secret: String = "",
    /** 단계별 빨간 안내. */
    val message: String? = null,
    val busy: Boolean = false,
    /** 완료 팝업 — 값은 재가입 가능일(없으면 빈 글자). null 이면 팝업 없음. */
    val doneRejoin: String? = null,
) {
    val agreementKeys: List<String> get() = WithdrawUseCase.AGREEMENTS + if (info?.autoCancel?.isNotEmpty() == true) listOf(WithdrawUseCase.AUTO_CANCEL) else emptyList()
    val allChecked: Boolean get() = agreementKeys.all { it in checked }
    val canProceed: Boolean get() = info != null && info.blockers.isEmpty() && allChecked
}

sealed interface WithdrawEvent { data object LoggedOut : WithdrawEvent }

/**
 * 회원탈퇴 — 웹 pages/muyeon/my/Withdraw.js. 안내·동의 → 사유 → 본인확인(비밀번호 또는 확인 문구) 3단계, 완료 팝업 뒤 로그아웃.
 *   서버가 WITHDRAW_BLOCKED 로 막으면 안내 단계로 되돌리고 다시 읽는다.
 */
class WithdrawViewModel(private val withdraw: WithdrawUseCase, private val logout: LogoutUseCase) : ViewModel() {
    private val _state = MutableStateFlow(WithdrawUiState())
    val state: StateFlow<WithdrawUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<WithdrawEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<WithdrawEvent> = _events.asSharedFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = withdraw.info()) {
                is ApiResult.Success -> _state.update { it.copy(info = r.data, isLoading = false) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, errorMessage = r.message) }
            }
        }
    }

    fun toggleAll(on: Boolean) = _state.update { it.copy(checked = if (on) it.agreementKeys.toSet() else emptySet()) }
    fun toggle(key: String) = _state.update { it.copy(checked = if (key in it.checked) it.checked - key else it.checked + key) }

    fun toReason() { if (_state.value.canProceed) _state.update { it.copy(step = WithdrawStep.REASON, message = null) } }
    fun back() = _state.update {
        when (it.step) {
            WithdrawStep.CONFIRM -> it.copy(step = WithdrawStep.REASON, message = null)
            else -> it.copy(step = WithdrawStep.INFO, message = null)
        }
    }

    fun onReason(code: String) = _state.update { it.copy(reasonCode = code, message = null) }
    fun onReasonText(t: String) = _state.update { it.copy(reasonText = t.take(200), message = null) }
    fun onSecret(t: String) = _state.update { it.copy(secret = t, message = null) }

    fun toConfirm(etcMissing: String) {
        val s = _state.value
        if (s.reasonCode.isEmpty()) return
        if (s.reasonCode == WithdrawUseCase.ETC && s.reasonText.isBlank()) { _state.update { it.copy(message = etcMissing) }; return }
        _state.update { it.copy(step = WithdrawStep.CONFIRM, secret = "", message = null) }
    }

    fun submit() {
        val s = _state.value
        val info = s.info ?: return
        if (s.busy || s.secret.isBlank()) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            when (val r = withdraw(info, s.reasonCode, s.reasonText, s.secret)) {
                is ApiResult.Success -> when (val w = r.data) {
                    is WithdrawResult.Done -> _state.update { it.copy(busy = false, doneRejoin = w.rejoinableFrom.orEmpty()) }
                    WithdrawResult.Blocked -> { _state.update { it.copy(busy = false, step = WithdrawStep.INFO) }; load() }
                }
                is ApiResult.Error -> _state.update { it.copy(busy = false, message = r.message) }
            }
        }
    }

    /** 완료 팝업 [확인] — 로그아웃(MY·설정과 같은 길) 후 홈으로. */
    fun finish() {
        _state.update { it.copy(doneRejoin = null) }
        logout()
        _events.tryEmit(WithdrawEvent.LoggedOut)
    }
}
