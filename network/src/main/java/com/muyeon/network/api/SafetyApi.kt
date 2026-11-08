package com.muyeon.network.api

import com.muyeon.network.dto.safety.ReportBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/** 출처: reports.controller.ts · blocks.controller.ts. */
interface SafetyApi {
    @POST(ApiEndpoints.Safety.REPORTS)
    suspend fun report(@Body body: ReportBody): Response<ResponseBody>

    @POST(ApiEndpoints.Safety.BLOCK)
    suspend fun block(@Path("id") userId: Long): Response<ResponseBody>

    @retrofit2.http.DELETE(ApiEndpoints.Safety.BLOCK)
    suspend fun unblock(@Path("id") userId: Long): Response<ResponseBody>

    @retrofit2.http.GET(ApiEndpoints.Safety.MY_BLOCKS)
    suspend fun myBlocks(): Response<List<com.muyeon.network.dto.safety.BlockedUserDto>>
}
