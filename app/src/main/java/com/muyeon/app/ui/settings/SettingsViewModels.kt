package com.muyeon.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.model.safety.BlockedUser
import com.muyeon.domain.usecase.my.LogoutUseCase
import com.muyeon.domain.usecase.safety.BlockedUsersUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(val isLoggedIn: Boolean = false, val confirmLogout: Boolean = false)

sealed interface SettingsEvent { data object LoggedOut : SettingsEvent }

/** 설정 — 웹 pages/setting/Setting.js + 계정(로그아웃 — MY 와 같은 LogoutUseCase 길). */
class SettingsViewModel(private val logoutUseCase: LogoutUseCase, private val tokens: TokenStore) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState(isLoggedIn = tokens.isLoggedIn))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    /** 화면이 다시 보일 때 — 웹 로그인 창에서 돌아왔을 수 있다. */
    fun onResume() = _state.update { it.copy(isLoggedIn = tokens.isLoggedIn) }

    fun askLogout(ask: Boolean) = _state.update { it.copy(confirmLogout = ask) }

    fun logout() {
        _state.update { it.copy(confirmLogout = false, isLoggedIn = false) }
        logoutUseCase()
        _events.tryEmit(SettingsEvent.LoggedOut)
    }
}

data class BlockedUsersUiState(
    val items: List<BlockedUser> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val busyId: Long? = null,
)

sealed interface BlockedUsersEvent {
    data class Unblocked(val name: String?) : BlockedUsersEvent
    data class Message(val text: String) : BlockedUsersEvent
}

/** 차단한 사용자 — 웹 pages/muyeon/my/BlockedUsers.js(해제). */
class BlockedUsersViewModel(private val blocks: BlockedUsersUseCase) : ViewModel() {
    private val _state = MutableStateFlow(BlockedUsersUiState())
    val state: StateFlow<BlockedUsersUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<BlockedUsersEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<BlockedUsersEvent> = _events.asSharedFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = blocks.list()) {
                is ApiResult.Success -> _state.update { it.copy(items = r.data, isLoading = false) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, errorMessage = r.message) }
            }
        }
    }

    fun unblock(u: BlockedUser) {
        if (_state.value.busyId != null) return
        _state.update { it.copy(busyId = u.userId) }
        viewModelScope.launch {
            when (val r = blocks.unblock(u.userId)) {
                is ApiResult.Success -> {
                    _state.update { s -> s.copy(items = s.items.filterNot { it.userId == u.userId }) }
                    _events.tryEmit(BlockedUsersEvent.Unblocked(u.name))
                }
                is ApiResult.Error -> _events.tryEmit(BlockedUsersEvent.Message(r.message))
            }
            _state.update { it.copy(busyId = null) }
        }
    }
}
