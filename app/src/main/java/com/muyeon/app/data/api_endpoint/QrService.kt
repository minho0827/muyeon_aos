package com.muyeon.app.data.api_endpoint

import android.util.Log
import com.muyeon.app.data.models.qr.QrRequest
import com.muyeon.app.data.models.qr.QrResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object QrService {
    private val logging = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    private val client = OkHttpClient.Builder().addInterceptor(logging).build()

    private fun createApiService(): ApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(com.muyeon.app.utils.Constants.BASE_URL_QR)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ApiService::class.java)
    }

    suspend fun checkQrCode(
         rsvSeq: Int?,
         mbrSeq: Int?,
         cfFdSeq: Int?,
         rsvStatusCd: String?,
         rsvDate: String?,
         rsvCnt: Int?
    ): QrResponse? {
        try {
            val requestBody = QrRequest(rsvSeq,mbrSeq,cfFdSeq,rsvStatusCd,rsvDate,rsvCnt)
            val apiService = createApiService()

            val response = apiService.checkQrCode(requestBody)
            val bodyStatus = response.body()

            return bodyStatus

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("QrService", "Network or parsing error: ${e.message}")
            return null
        }
    }
}