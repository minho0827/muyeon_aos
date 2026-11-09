package com.muyeon.network.api

import com.muyeon.network.dto.quotefeed.QuoteFeedAvailableDto
import com.muyeon.network.dto.quotefeed.QuoteFeedHomePageDto
import com.muyeon.network.dto.quotefeed.QuoteFeedItemDto
import com.muyeon.network.interceptor.NetworkRetryInterceptor
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

/** 출처: ApiEndpoints.QuoteFeed 주석(quotes.controller.ts). */
interface QuoteFeedApi {
    /** 홈 섹션은 조용히 — 실패면 섹션만 숨긴다(웹 HomeQuoteRequests catch → []). */
    @Headers("${NetworkRetryInterceptor.SILENT_HEADER}: 1")
    @GET(ApiEndpoints.QuoteFeed.HOME)
    suspend fun home(): Response<List<QuoteFeedItemDto>>

    @GET(ApiEndpoints.QuoteFeed.HOME_PAGE)
    suspend fun homePage(
        @Query("categoryId") categoryId: String?,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
    ): Response<QuoteFeedHomePageDto>

    @GET(ApiEndpoints.QuoteFeed.AVAILABLE)
    suspend fun available(@Query("categoryId") categoryId: String?, @Query("page") page: Int): Response<QuoteFeedAvailableDto>
}
