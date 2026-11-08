package com.muyeon.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.usecase.account.ChangePasswordUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChangePasswordUiState(
    val password: String = "",
    val confirm: String = "",
    val invalid: ChangePasswordUseCase.Invalid? = null,
    val confirmAsk: Boolean = false,
    val saving: Boolean = false,
    /** 결과 팝업 문구 id — [done] 이면 확인 뒤 닫는다. */
    val result: Int? = null,
    val done: Boolean = false,
)

/** 비밀번호 변경 — 웹 pages/muyeon/my/ChangePassword.js(정책 확인 → '변경할까요?' → PATCH /auth/me/password). */
class ChangePasswordViewModel(private val change: ChangePasswordUseCase) : ViewModel() {
    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun onPassword(v: String) = _state.update { it.copy(password = v.take(MAX), invalid = if (it.invalid == ChangePasswordUseCase.Invalid.POLICY) null else it.invalid) }
    fun onConfirm(v: String) = _state.update { it.copy(confirm = v.take(MAX), invalid = if (it.invalid == ChangePasswordUseCase.Invalid.MISMATCH) null else it.invalid) }

    fun submit() {
        val s = _state.value
        when (change.check(s.password, s.confirm)) {
            ChangePasswordUseCase.Invalid.POLICY -> _state.update { it.copy(invalid = ChangePasswordUseCase.Invalid.POLICY, result = com.muyeon.app.R.string.password_policy_check) }
            ChangePasswordUseCase.Invalid.MISMATCH -> _state.update { it.copy(invalid = ChangePasswordUseCase.Invalid.MISMATCH, result = com.muyeon.app.R.string.password_mismatch_check) }
            null -> _state.update { it.copy(invalid = null, confirmAsk = true) }
        }
    }

    fun cancelAsk() = _state.update { it.copy(confirmAsk = false) }

    fun confirmChange() {
        if (_state.value.saving) return
        val pw = _state.value.password
        _state.update { it.copy(confirmAsk = false, saving = true) }
        viewModelScope.launch {
            when (change(pw)) {
                is ApiResult.Success -> _state.update { ChangePasswordUiState(result = com.muyeon.app.R.string.password_changed, done = true) }
                is ApiResult.Error -> _state.update { it.copy(saving = false, result = com.muyeon.app.R.string.password_change_failed) }
            }
        }
    }

    /** 결과 팝업 닫기 — 바뀌었으면 true(화면을 닫는다). */
    fun dismissResult(): Boolean {
        val done = _state.value.done
        _state.update { it.copy(result = null, done = false) }
        return done
    }

    private companion object { const val MAX = 20 }
}
