package com.muyeon.network.api

import com.muyeon.network.dto.common.CursorPageDto
import com.muyeon.network.dto.common.SearchCountDto
import com.muyeon.network.dto.common.SearchPageDto
import com.muyeon.network.dto.performance.PerformanceDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

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

    /** 검색 — [params] 는 q·statuses·genres·regions·sort(빈 값 없음). */
    @GET(ApiEndpoints.Performances.SEARCH)
    suspend fun search(
        @QueryMap params: Map<String, String>,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int,
    ): Response<SearchPageDto<PerformanceDto>>

    @GET(ApiEndpoints.Performances.SEARCH_COUNT)
    suspend fun searchCount(@QueryMap params: Map<String, String>): Response<SearchCountDto>
}
