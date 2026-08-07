package com.muyeon.app.data.repository

import android.content.Context
import com.muyeon.app.data.api_endpoint.TokenAPI
import com.muyeon.app.domain.repositories.AuthRepository
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.utils.WebMessageStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthRepository {
    override suspend fun hasValidToken(): Boolean {
        return withContext(dispatcher) {
            val accessToken = TokenManager.getAccessToken(context)
            val refreshToken = TokenManager.getRefreshToken(context)
            if (accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
                false
            } else {
                val result = TokenAPI.refreshToken(context,accessToken, refreshToken)
                result == WebMessageStatus.SUCCESS
            }
        }
    }

    override suspend fun login(username: String, password: String): Pair<String, String>? {
        return withContext(dispatcher) {
            TokenAPI.login(context,username, password)
        }
    }

    override suspend fun refreshToken(accessToken: String, refreshToken: String): Boolean {
        return withContext(dispatcher) {
            val result = TokenAPI.refreshToken(context,accessToken, refreshToken)
            result == WebMessageStatus.SUCCESS
        }
    }
}
