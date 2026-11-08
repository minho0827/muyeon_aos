package com.muyeon.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.R
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.usecase.auth.FindIdUseCase
import com.muyeon.domain.usecase.auth.ResetPasswordUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountRecoveryUiState(
    val type: String = AccountRecoveryViewModel.TYPE_ID,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val code: String = "",
    val password: String = "",
    val confirm: String = "",
    /** 인증번호를 보냈다 — 인증번호·새 비밀번호 칸이 보인다. */
    val codeSent: Boolean = false,
    val sending: Boolean = false,
    val submitting: Boolean = false,
    /** 칸 아래 안내(빨강=오류 · 회색=안내). 서버 문구는 [infoText], 앱 문구는 [infoRes]. */
    val infoText: String? = null,
    val infoRes: Int? = null,
    val infoError: Boolean = false,
    /** 결과 팝업 — 찾은 이메일([foundEmail]) · 앱 문구([resultRes]) · 서버 문구([resultText]). */
    val foundEmail: String? = null,
    val resultRes: Int? = null,
    val resultText: String? = null,
) {
    val isPassword: Boolean get() = type == AccountRecoveryViewModel.TYPE_PW
    val showResult: Boolean get() = foundEmail != null || resultRes != null || resultText != null
}

/**
 * 아이디 찾기 · 비밀번호 재설정 — 웹 pages/account/AccountRecovery.js 와 같은 단계.
 *   id: 이름 + 휴대폰 → POST /auth/find-id → 팝업(가입 이메일 / 서버 문구).
 *   pw: 이메일 → [인증번호 받기](send-code RESET) → 인증번호·새 비밀번호·확인 → POST /auth/reset-password-email → 팝업.
 */
class AccountRecoveryViewModel(
    type: String,
    private val findId: FindIdUseCase,
    private val reset: ResetPasswordUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AccountRecoveryUiState(type = if (type == TYPE_PW) TYPE_PW else TYPE_ID))
    val state: StateFlow<AccountRecoveryUiState> = _state.asStateFlow()

    fun onName(v: String) = _state.update { it.copy(name = v.take(30)) }
    fun onPhone(v: String) = _state.update { it.copy(phone = v.take(20)) }
    /** 이메일을 바꾸면 인증을 처음부터(웹 onEmail). */
    fun onEmail(v: String) = _state.update { it.copy(email = v, codeSent = false, infoText = null, infoRes = null) }
    fun onCode(v: String) = _state.update { it.copy(code = v.filter(Char::isDigit).take(ResetPasswordUseCase.CODE_LENGTH)) }
    fun onPassword(v: String) = _state.update { it.copy(password = v.take(50)) }
    fun onConfirm(v: String) = _state.update { it.copy(confirm = v.take(50)) }

    fun sendCode() {
        val s = _state.value
        if (s.sending) return
        if (reset.checkEmail(s.email) != null) return errorRes(R.string.auth_recovery_email_invalid)
        _state.update { it.copy(sending = true, infoText = null, infoRes = null) }
        viewModelScope.launch {
            when (val r = reset.sendCode(s.email)) {
                is ApiResult.Success -> _state.update { it.copy(sending = false, codeSent = true, infoRes = R.string.auth_recovery_code_sent, infoText = null, infoError = false) }
                is ApiResult.Error -> _state.update { it.copy(sending = false, infoText = r.message, infoRes = null, infoError = true) }
            }
        }
    }

    fun submit() {
        val s = _state.value
        if (s.submitting) return
        if (!s.isPassword) {
            _state.update { it.copy(submitting = true) }
            viewModelScope.launch {
                when (val r = findId(s.name, s.phone)) {
                    is ApiResult.Success -> _state.update { it.copy(submitting = false, foundEmail = r.data) }
                    is ApiResult.Error -> _state.update { it.copy(submitting = false, resultText = r.message) }
                }
            }
            return
        }
        when (reset.check(s.code, s.password, s.confirm)) {
            ResetPasswordUseCase.Invalid.CODE -> return errorRes(R.string.auth_recovery_code_invalid)
            ResetPasswordUseCase.Invalid.LENGTH -> return errorRes(R.string.auth_recovery_password_short)
            ResetPasswordUseCase.Invalid.MISMATCH -> return errorRes(R.string.auth_recovery_password_mismatch)
            else -> Unit
        }
        _state.update { it.copy(submitting = true, infoText = null, infoRes = null) }
        viewModelScope.launch {
            when (val r = reset(s.email, s.code, s.password)) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, resultRes = R.string.auth_recovery_reset_done) }
                is ApiResult.Error -> _state.update { it.copy(submitting = false, infoText = r.message, infoRes = null, infoError = true) }
            }
        }
    }

    fun dismissResult() = _state.update { it.copy(foundEmail = null, resultRes = null, resultText = null) }

    private fun errorRes(res: Int) = _state.update { it.copy(infoRes = res, infoText = null, infoError = true) }

    companion object {
        /** 웹 /accountRecovery?type=id|pw */
        const val TYPE_ID = "id"
        const val TYPE_PW = "pw"
    }
}
