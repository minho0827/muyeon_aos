package com.muyeon.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.navigation.AppResults
import com.muyeon.app.result.ResultKeys
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.account.MyAccount
import com.muyeon.domain.usecase.account.EditAccountUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileEditUiState(
    val account: MyAccount? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** 편집 중 값. */
    val name: String = "",
    val image: String? = null,
    val saving: Boolean = false,
    /** 본인확인 표준창 주소 — 있으면 화면이 앱 안 웹창으로 띄운다. */
    val identityUrl: String? = null,
    val identityBusy: Boolean = false,
    /** 휴대폰 칸 아래 빨간 안내(본인확인 실패·취소). */
    val identityMessage: String? = null,
    /** 결과 팝업 문구 — [savedDone] 이면 확인 뒤 화면을 닫는다. */
    val resultMessage: String? = null,
    val savedDone: Boolean = false,
    val confirmLeave: Boolean = false,
) {
    val dirty: Boolean get() = account != null && (name != account.name || (image ?: "") != (account.image ?: ""))
}

sealed interface ProfileEditEvent { data object Close : ProfileEditEvent }

/**
 * 개인정보 수정 — 웹 pages/muyeon/my/ProfileEdit.js.
 *   사진(보기만 — 이력서·강사프로필 사진과 같아 여기서 바꾸지 않는다, 저장 때 지금 사진을 그대로 보낸다) · 이름(본인확인 회원은 고정) · 휴대폰(본인확인으로만 등록/변경 — 앱 안 웹창으로 NICE 표준창) · 회원유형(유형 화면) ·
 *   비밀번호 변경·회원탈퇴(웹). 저장 전 나가면 확인(웹 useUnsavedGuard).
 */
class ProfileEditViewModel(private val edit: EditAccountUseCase) : ViewModel() {
    private val _state = MutableStateFlow(ProfileEditUiState())
    val state: StateFlow<ProfileEditUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<ProfileEditEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<ProfileEditEvent> = _events.asSharedFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = edit.load()) {
                is ApiResult.Success -> apply(r.data)
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, errorMessage = r.message) }
            }
        }
    }

    private fun apply(a: MyAccount) = _state.update { it.copy(account = a, name = a.name, image = a.image, isLoading = false) }

    fun onName(v: String) { if (_state.value.account?.nameLocked != true) _state.update { it.copy(name = v.take(MAX_NAME)) } }

    fun save() {
        val s = _state.value
        val a = s.account ?: return
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val r = edit.save(a, s.name, s.image)) {
                is ApiResult.Success -> {
                    apply(r.data)
                    AppResults.publish(setOf(ResultKeys.PROFILE))
                    _state.update { it.copy(saving = false, savedDone = true, resultMessage = SAVED) }
                }
                is ApiResult.Error -> _state.update { it.copy(saving = false, resultMessage = r.message) }
            }
        }
    }

    // ── 휴대폰 본인확인(NICE) ──

    fun startIdentity() {
        if (_state.value.identityBusy) return
        _state.update { it.copy(identityBusy = true, identityMessage = null) }
        viewModelScope.launch {
            when (val r = edit.startIdentity()) {
                is ApiResult.Success -> _state.update { it.copy(identityUrl = r.data) }
                is ApiResult.Error -> _state.update { it.copy(identityBusy = false, identityMessage = r.message) }
            }
        }
    }

    /** 표준창이 끝나 돌아왔다 — 토큰이면 서버에 반영, 오류 코드면 안내(웹 NICE_ERROR_MESSAGE). null 둘 다면 사용자가 창을 닫음. */
    fun onIdentityResult(token: String?, error: String?) {
        _state.update { it.copy(identityUrl = null) }
        when {
            token != null -> viewModelScope.launch {
                when (val r = edit.linkIdentity(token)) {
                    is ApiResult.Success -> {
                        // 인증된 실명·번호로 바뀐다 — 편집 중이던 이름도 인증 값으로.
                        apply(r.data)
                        AppResults.publish(setOf(ResultKeys.PROFILE))
                        _state.update { it.copy(identityBusy = false, resultMessage = IDENTITY_DONE) }
                    }
                    is ApiResult.Error -> _state.update { it.copy(identityBusy = false, identityMessage = r.message, resultMessage = r.message) }
                }
            }
            error != null -> _state.update { it.copy(identityBusy = false, identityMessage = NICE_ERRORS[error] ?: NICE_ERRORS.getValue("FAILED")) }
            else -> _state.update { it.copy(identityBusy = false) }
        }
    }

    fun dismissResult() {
        val done = _state.value.savedDone
        _state.update { it.copy(resultMessage = null, savedDone = false) }
        if (done) _events.tryEmit(ProfileEditEvent.Close)
    }

    /** 뒤로 — 고친 게 있으면 확인. */
    fun requestBack() { if (_state.value.dirty) _state.update { it.copy(confirmLeave = true) } else _events.tryEmit(ProfileEditEvent.Close) }
    fun cancelLeave() = _state.update { it.copy(confirmLeave = false) }
    fun leave() { _state.update { it.copy(confirmLeave = false) }; _events.tryEmit(ProfileEditEvent.Close) }

    companion object {
        private const val MAX_NAME = 30
        // 서버 계약 문구(웹 ProfileEdit·niceIdentity 와 같은 말). 화면 고정 문구는 strings.xml.
        const val SAVED = "정보가 수정됐어요."
        const val IDENTITY_DONE = "휴대폰 본인인증이 완료됐어요."
        val NICE_ERRORS = mapOf(
            "CANCELLED" to "본인인증을 취소했어요. 다시 시도해 주세요.",
            "EXPIRED" to "본인인증 시간이 지났어요. 다시 시도해 주세요.",
            "ALREADY_USED" to "이미 사용한 본인인증이에요. 다시 시도해 주세요.",
            "NOT_FOUND" to "본인인증 요청을 찾지 못했어요. 다시 시도해 주세요.",
            "FAILED" to "본인인증에 실패했어요. 잠시 후 다시 시도해 주세요.",
        )
    }
}
