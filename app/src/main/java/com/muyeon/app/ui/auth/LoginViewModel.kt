package com.muyeon.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.R
import com.muyeon.app.auth.LoginRememberStore
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.auth.LoginSession
import com.muyeon.domain.usecase.auth.EmailLoginUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val remember: Boolean = false,
    val loading: Boolean = false,
    /** 안내 문구 — 서버 message 그대로([messageText]) 또는 앱 문구([messageRes]). */
    val messageText: String? = null,
    val messageRes: Int? = null,
    val signupOpen: Boolean = false,
)

/** 이메일 로그인 — 웹 pages/login/Login.js(빈 칸 안내 → POST /auth/login → 저장 켜짐이면 아이디·비밀번호 저장). */
class LoginViewModel(
    private val login: EmailLoginUseCase,
    private val remember: LoginRememberStore,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _loggedIn = MutableSharedFlow<LoginSession>(extraBufferCapacity = 1)
    val loggedIn: SharedFlow<LoginSession> = _loggedIn.asSharedFlow()

    init {
        // 저장된 아이디·비밀번호 자동 채움(웹 REMEMBER_KEY).
        remember.load()?.let { s -> _state.update { it.copy(email = s.email, password = s.password, remember = true) } }
    }

    fun onEmail(v: String) = _state.update { it.copy(email = v.take(MAX)) }
    fun onPassword(v: String) = _state.update { it.copy(password = v.take(MAX)) }
    fun onRemember(v: Boolean) = _state.update { it.copy(remember = v) }
    fun openSignup() = _state.update { it.copy(signupOpen = true) }
    fun closeSignup() = _state.update { it.copy(signupOpen = false) }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        if (!login.check(s.email, s.password)) {
            _state.update { it.copy(messageRes = R.string.auth_login_empty, messageText = null) }
            return
        }
        _state.update { it.copy(loading = true, messageRes = null, messageText = null) }
        viewModelScope.launch {
            when (val r = login(s.email, s.password)) {
                is ApiResult.Success -> {
                    if (s.remember) remember.save(s.email, s.password) else remember.clear()
                    _state.update { it.copy(loading = false) }
                    _loggedIn.emit(r.data)
                }
                is ApiResult.Error -> _state.update { it.copy(loading = false, messageText = r.message) }
            }
        }
    }

    private companion object { const val MAX = 50 }
}
