package com.muyeon.app.domain.use_cases

import com.muyeon.app.domain.repositories.AuthRepository

@Suppress("unused")
class LoginUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(username: String, password: String): Pair<String, String>? {
        if (username.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("Please enter full email and password")
        }
        if (password.length < 6) {
            throw IllegalArgumentException("Password must be at least 6 characters")
        }

        return authRepository.login(username, password)
    }
}