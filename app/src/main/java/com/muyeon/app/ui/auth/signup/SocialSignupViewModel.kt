package com.muyeon.app.ui.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.R
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.auth.NiceIdentity
import com.muyeon.domain.usecase.auth.SignupIdentityUseCase
import com.muyeon.domain.usecase.auth.SocialSignupUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SocialSignupUiState(
    val name: String = "",
    val identityAvailable: Boolean = false,
    val identityRequired: Boolean = false,
    val identity: NiceIdentity? = null,
    val identityBusy: Boolean = false,
    val identityUrl: String? = null,
    val submitting: Boolean = false,
    val messageText: String? = null,
    val messageRes: Int? = null,
    val done: Boolean = false,
)

/**
 * 소셜 신규회원 추가정보 — 웹 pages/account/SocialSignup.js(이름 · 휴대폰 본인인증 → PATCH /auth/me/social-complete).
 *  [currentName] = 로그인 응답 user.name(소셜 기본 이름이면 비운다). 저장되면 갱신된 user 를 [completed] 로(웹 updateUser).
 */
class SocialSignupViewModel(
    currentName: String?,
    private val social: SocialSignupUseCase,
    private val identityUc: SignupIdentityUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SocialSignupUiState(name = social.initialName(currentName)))
    val state: StateFlow<SocialSignupUiState> = _state.asStateFlow()

    private val _completed = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** 갱신된 user(JSON). */
    val completed: SharedFlow<String> = _completed.asSharedFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            val c = identityUc.config()
            _state.update { it.copy(identityAvailable = c.available, identityRequired = c.required) }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v.take(RegisterViewModel.MAX_NAME)) }

    fun startIdentity() {
        if (_state.value.identityBusy) return
        _state.update { it.copy(identityBusy = true) }
        viewModelScope.launch {
            when (val r = identityUc.start(SignupIdentityUseCase.RETURN_SOCIAL)) {
                is ApiResult.Success -> _state.update { it.copy(identityUrl = r.data) }
                is ApiResult.Error -> _state.update { it.copy(identityBusy = false, messageText = r.rejectedOrNull(), messageRes = R.string.auth_signup_identity_start_failed) }
            }
        }
    }

    fun onIdentityResult(token: String?, error: String?) {
        _state.update { it.copy(identityUrl = null, identityBusy = false) }
        when {
            error != null -> _state.update { it.copy(messageRes = NiceErrors.res(error), messageText = null) }
            token != null -> viewModelScope.launch {
                when (val r = identityUc.result(token)) {
                    is ApiResult.Success -> _state.update { it.copy(identity = r.data, name = r.data.name.ifEmpty { it.name }) }
                    is ApiResult.Error -> _state.update { it.copy(messageText = r.rejectedOrNull(), messageRes = R.string.auth_signup_identity_result_failed) }
                }
            }
        }
    }

    fun submit() {
        val s = _state.value
        if (s.submitting) return
        when (social.check(s.name, s.identityRequired, s.identity)) {
            SocialSignupUseCase.Invalid.NAME -> return message(R.string.auth_social_name_missing)
            SocialSignupUseCase.Invalid.IDENTITY_MISSING -> return message(R.string.auth_signup_identity_missing)
            null -> Unit
        }
        _state.update { it.copy(submitting = true, messageText = null, messageRes = null) }
        viewModelScope.launch {
            when (val r = social(s.name, s.identity)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false, done = true) }
                    _completed.emit(r.data)
                }
                is ApiResult.Error -> _state.update { it.copy(submitting = false, messageText = r.rejectedOrNull(), messageRes = R.string.auth_social_failed) }
            }
        }
    }

    fun closeDone() = _state.update { it.copy(done = false) }

    private fun message(res: Int) = _state.update { it.copy(messageRes = res, messageText = null) }
}
