package com.muyeon.network.api

import com.muyeon.network.dto.scrap.ScrapItemDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** 출처: scraps.controller.ts. segment 는 ScrapTarget.pathSegment(performances·teachers·lesson-products …). */
interface ScrapApi {
    @GET(ApiEndpoints.Scraps.MINE)
    suspend fun mine(@Query("targetType") targetType: String): Response<List<ScrapItemDto>>

    @POST(ApiEndpoints.Scraps.TOGGLE)
    suspend fun add(@Path("segment", encoded = true) segment: String, @Path("id") id: Long): Response<ResponseBody>

    @DELETE(ApiEndpoints.Scraps.TOGGLE)
    suspend fun remove(@Path("segment", encoded = true) segment: String, @Path("id") id: Long): Response<ResponseBody>
}
