package com.muyeon.app.ui.login

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import com.muyeon.app.data.api_endpoint.TokenAPI
import kotlinx.coroutines.Dispatchers
import com.muyeon.app.utils.BaseScreenState
import androidx.compose.runtime.State

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    override val isLoading: Boolean = false,
    override val errorMessage: String? = null
) : BaseScreenState

class LoginViewModel(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = mutableStateOf(LoginUiState())
    val uiState: State<LoginUiState> = _uiState

    fun onUsernameChange(newUsername: String) {
        _uiState.value = _uiState.value.copy(username = newUsername)
    }

    fun onTogglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.value = _uiState.value.copy(password = newPassword)
    }

    fun login(context: Context) {
        val state = uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter full email and password", isLoading = false)
            return
        }

        if (state.password.length < 6) {
            _uiState.value = state.copy(
                errorMessage = "Password must be at least 6 characters",
                isLoading = false
            )
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch(dispatcher) {
            val result = try {
                TokenAPI.login(context,state.username, state.password)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Login failed. Please check your information again",
                    isLoading = false
                )
                null
            }

            if (result != null) {
                TokenManager.saveTokens(context, result.first, result.second)

                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
                (context as? Activity)?.finish()
            }
        }
    }
}