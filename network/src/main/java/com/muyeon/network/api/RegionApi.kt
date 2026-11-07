package com.muyeon.network.api

import com.muyeon.network.dto.region.RegionDto
import com.muyeon.network.dto.region.UiFlagsDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Query

/** 출처: regions.controller.ts(nearest) · auth.controller.ts(ui-flags). */
interface RegionApi {
    /** 서버가 null 을 주면 본문이 비어 온다 — 원문으로 받아 저장소가 읽는다. */
    @GET(ApiEndpoints.Regions.NEAREST)
    suspend fun nearest(@Query("lat") lat: Double, @Query("lng") lng: Double): Response<ResponseBody>

    @GET(ApiEndpoints.Auth.UI_FLAGS)
    suspend fun uiFlags(): Response<UiFlagsDto>

    @PATCH(ApiEndpoints.Auth.UI_FLAGS)
    suspend fun setUiFlags(@Body flags: Map<String, Boolean>): Response<ResponseBody>
}
