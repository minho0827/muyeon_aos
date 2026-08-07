package com.muyeon.app.domain.repositories

interface AuthRepository {
    suspend fun hasValidToken(): Boolean
    suspend fun login(username: String, password: String): Pair<String, String>?

    suspend fun refreshToken(accessToken: String, refreshToken: String): Boolean
}