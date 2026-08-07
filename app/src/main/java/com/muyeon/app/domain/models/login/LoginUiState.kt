package com.muyeon.app.domain.models.login

import com.muyeon.app.utils.BaseScreenState

@Suppress("unused")
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    override val isLoading: Boolean = false,
    override val errorMessage: String? = null
) : BaseScreenState