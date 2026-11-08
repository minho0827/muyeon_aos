package com.muyeon.network.api

import com.muyeon.network.dto.alert.AlertPrefsBody
import com.muyeon.network.dto.alert.AlertPrefsDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

/** 출처: alerts.controller.ts. */
interface AlertPrefsApi {
    @GET(ApiEndpoints.AlertPrefs.ME)
    suspend fun get(): Response<AlertPrefsDto>

    @PATCH(ApiEndpoints.AlertPrefs.ME)
    suspend fun save(@Body body: AlertPrefsBody): Response<ResponseBody>
}
