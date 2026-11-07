package com.muyeon.network.api

import com.muyeon.network.dto.common.CursorPageDto
import com.muyeon.network.dto.performance.PerformanceDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** 출처: performances.controller.ts. */
interface PerformanceApi {
    @GET(ApiEndpoints.Performances.PAGE)
    suspend fun page(
        @Query("genre") genre: String? = null,
        @Query("subGenre") subGenre: String? = null,
        @Query("tag") tag: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int,
    ): Response<CursorPageDto<PerformanceDto>>

    @GET(ApiEndpoints.Performances.DETAIL)
    suspend fun detail(@Path("id") id: Long): Response<PerformanceDto>
}
