package com.muyeon.app.data.api_endpoint

import android.content.Context
import com.muyeon.app.data.models.login.LoginRequest
import com.muyeon.app.data.models.token.TokenRequest
import okhttp3.OkHttpClient
import com.muyeon.app.utils.WebMessageStatus
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TokenAPI {
    private val logging = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    private val client = OkHttpClient.Builder().addInterceptor(logging).build()


    private fun createApiService(context: Context): ApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(com.muyeon.app.utils.Constants.getBaseUrl(context))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ApiService::class.java)
    }
    suspend fun refreshToken(
        context: Context,
        accessToken: String,
        refreshToken: String,
    ): WebMessageStatus? {
        return try {
            val requestBody = TokenRequest(accessToken, refreshToken)
            val apiService = createApiService(context)

            val response = apiService.refreshTokenCheck(requestBody)

            if (response.isSuccessful) {
                if (response.body()?.data?.isRefreshToken == true) {
                    WebMessageStatus.SUCCESS
                } else {
                    WebMessageStatus.FAILED
                }
            } else {
                WebMessageStatus.fromCode(response.code())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            WebMessageStatus.SERVER_ERROR
        }
    }

    suspend fun login(
        context: Context,
        username: String,
        password: String
    ): Pair<String, String>? {
        return try {
            val requestBody = LoginRequest(username, password)
            val apiService = createApiService(context)
            val response = apiService.login(requestBody)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                Pair(body.accessToken, body.refreshToken)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}