package com.muyeon.app.ui.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.R
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.auth.LoginSession
import com.muyeon.domain.model.auth.NiceIdentity
import com.muyeon.domain.usecase.auth.RegisterUseCase
import com.muyeon.domain.usecase.auth.SignupIdentityUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val email: String = "",
    val code: String = "",
    val codeSent: Boolean = false,
    val emailVerified: Boolean = false,
    val sending: Boolean = false,
    val verifying: Boolean = false,
    /** 이메일 인증 안내(빨강=오류 · 회색=안내). 서버 문구는 [emailInfoText], 앱 문구는 [emailInfoRes]. */
    val emailInfoText: String? = null,
    val emailInfoRes: Int? = null,
    val emailInfoError: Boolean = false,
    /** 본인확인 — 서버가 정한다(키 발급 전에도 화면이 깨지지 않게). */
    val identityAvailable: Boolean = false,
    val identityRequired: Boolean = false,
    val identity: NiceIdentity? = null,
    val identityBusy: Boolean = false,
    val identityText: String? = null,
    val identityRes: Int? = null,
    /** 표준창 주소 — 있으면 앱 안 웹창(IdentityWebView)을 띄운다. */
    val identityUrl: String? = null,
    val password: String = "",
    val confirm: String = "",
    val name: String = "",
    val phone: String = "",
    val submitting: Boolean = false,
    /** 아래 안내(빨강) — 서버 문구 [messageText] · 앱 문구 [messageRes]. */
    val messageText: String? = null,
    val messageRes: Int? = null,
    /** 가입 완료 팝업. */
    val done: Boolean = false,
)

/**
 * 이메일 가입 — 웹 pages/account/Register.js 와 같은 단계·문구.
 *  이메일 인증번호(SIGNUP) → (서버 설정이면) 휴대폰 본인확인(NICE 표준창만 앱 안 웹창) → 비밀번호·이름·휴대폰 → POST /auth/register.
 *  가입되면 바로 로그인([registered]) — 웹 register() 가 저장 후 완료 팝업을 띄우는 것과 같다.
 *  [agreements] = 앞 화면(약관 동의) 체크 상태 — 웹 sessionStorage 캐리(popSignupAgreements)를 대신한다.
 */
class RegisterViewModel(
    private val agreements: Map<String, Boolean>,
    private val register: RegisterUseCase,
    private val identityUc: SignupIdentityUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    private val _registered = MutableSharedFlow<LoginSession>(extraBufferCapacity = 1)
    val registered: SharedFlow<LoginSession> = _registered.asSharedFlow()

    private var started = false

    /** 화면이 처음 뜰 때 한 번 — 본인확인 사용 여부(실패 = 미사용). */
    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            val c = identityUc.config()
            _state.update { it.copy(identityAvailable = c.available, identityRequired = c.required) }
        }
    }

    /** 이메일이 바뀌면 인증을 처음부터(다른 이메일로 인증 통과 방지 — 웹 onEmail). */
    fun onEmail(v: String) = _state.update {
        it.copy(email = v, emailVerified = false, codeSent = false, code = "", emailInfoText = null, emailInfoRes = null)
    }
    fun onCode(v: String) = _state.update { it.copy(code = v.filter(Char::isDigit).take(RegisterUseCase.CODE_LENGTH)) }
    fun onPassword(v: String) = _state.update { it.copy(password = v.take(MAX_PASSWORD)) }
    fun onConfirm(v: String) = _state.update { it.copy(confirm = v.take(MAX_PASSWORD)) }
    fun onName(v: String) = _state.update { it.copy(name = v.take(MAX_NAME)) }
    fun onPhone(v: String) = _state.update { it.copy(phone = v.take(MAX_PHONE)) }

    fun sendCode() {
        val s = _state.value
        if (s.sending) return
        if (register.checkEmail(s.email) != null) return emailError(R.string.auth_signup_email_invalid)
        _state.update { it.copy(sending = true, emailInfoText = null, emailInfoRes = null) }
        viewModelScope.launch {
            when (val r = register.sendCode(s.email)) {
                is ApiResult.Success -> _state.update { it.copy(sending = false, codeSent = true, emailInfoRes = R.string.auth_signup_code_sent, emailInfoText = null, emailInfoError = false) }
                is ApiResult.Error -> _state.update { it.copy(sending = false, emailInfoText = r.rejectedOrNull(), emailInfoRes = R.string.auth_signup_send_failed, emailInfoError = true) }
            }
        }
    }

    fun verifyCode() {
        val s = _state.value
        if (s.verifying) return
        if (register.checkCode(s.code) != null) return emailError(R.string.auth_signup_code_invalid)
        _state.update { it.copy(verifying = true) }
        viewModelScope.launch {
            when (val r = register.verifyCode(s.email, s.code)) {
                is ApiResult.Success -> _state.update { it.copy(verifying = false, emailVerified = true, emailInfoText = null, emailInfoRes = null) }
                is ApiResult.Error -> _state.update { it.copy(verifying = false, emailInfoText = r.rejectedOrNull(), emailInfoRes = R.string.auth_signup_code_mismatch, emailInfoError = true) }
            }
        }
    }

    // ── 본인확인(NICE 표준창) ──

    fun startIdentity() {
        if (_state.value.identityBusy) return
        _state.update { it.copy(identityBusy = true, identityText = null, identityRes = null) }
        viewModelScope.launch {
            when (val r = identityUc.start(SignupIdentityUseCase.RETURN_REGISTER)) {
                is ApiResult.Success -> _state.update { it.copy(identityUrl = r.data) }
                is ApiResult.Error -> _state.update { it.copy(identityBusy = false, identityText = r.rejectedOrNull(), identityRes = R.string.auth_signup_identity_start_failed) }
            }
        }
    }

    /** 표준창이 닫혔다 — 토큰이면 결과(이름)를 붙이고, 오류면 안내, 둘 다 없으면(뒤로) 그대로. */
    fun onIdentityResult(token: String?, error: String?) {
        _state.update { it.copy(identityUrl = null, identityBusy = false) }
        when {
            error != null -> _state.update { it.copy(identityRes = NiceErrors.res(error), identityText = null) }
            token != null -> viewModelScope.launch {
                when (val r = identityUc.result(token)) {
                    // 이름만 폼에 — 휴대폰은 가린 표시용이라 넣지 않는다(실제 번호는 서버가 토큰에서 꺼낸다).
                    is ApiResult.Success -> _state.update { it.copy(identity = r.data, name = r.data.name.ifEmpty { it.name }, identityText = null, identityRes = null) }
                    is ApiResult.Error -> _state.update { it.copy(identityText = r.rejectedOrNull(), identityRes = R.string.auth_signup_identity_result_failed) }
                }
            }
        }
    }

    // ── 가입 ──

    fun submit() {
        val s = _state.value
        if (s.submitting) return
        val invalid = register.check(s.email, s.password, s.confirm, s.name, s.emailVerified, s.identityRequired, s.identity, agreements)
        if (invalid != null) return message(INVALID_RES.getValue(invalid))
        _state.update { it.copy(submitting = true, messageText = null, messageRes = null) }
        viewModelScope.launch {
            when (val r = register(s.email, s.password, s.name, s.phone, s.identity, agreements)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false, done = true) }
                    _registered.emit(r.data)
                }
                is ApiResult.Error -> _state.update { it.copy(submitting = false, messageText = r.rejectedOrNull(), messageRes = R.string.auth_signup_failed) }
            }
        }
    }

    fun closeDone() = _state.update { it.copy(done = false) }

    private fun emailError(res: Int) = _state.update { it.copy(emailInfoRes = res, emailInfoText = null, emailInfoError = true) }
    private fun message(res: Int) = _state.update { it.copy(messageRes = res, messageText = null) }

    companion object {
        const val MAX_PASSWORD = 50
        const val MAX_NAME = 30
        const val MAX_PHONE = 20
        private val INVALID_RES = mapOf(
            RegisterUseCase.Invalid.MISSING_FIELDS to R.string.auth_signup_missing_fields,
            RegisterUseCase.Invalid.EMAIL_UNVERIFIED to R.string.auth_signup_email_unverified,
            RegisterUseCase.Invalid.IDENTITY_MISSING to R.string.auth_signup_identity_missing,
            RegisterUseCase.Invalid.PASSWORD_SHORT to R.string.auth_signup_password_short,
            RegisterUseCase.Invalid.PASSWORD_MISMATCH to R.string.auth_signup_password_mismatch,
            RegisterUseCase.Invalid.TERMS_MISSING to R.string.auth_signup_terms_missing,
        )
    }
}

/** 서버가 거절 사유를 줬으면 그 문구(웹 err.response.data.message), 아니면 null(화면별 기본 문구). */
internal fun ApiResult.Error.rejectedOrNull(): String? = message.takeIf { kind == com.muyeon.domain.common.ErrorKind.REJECTED }
