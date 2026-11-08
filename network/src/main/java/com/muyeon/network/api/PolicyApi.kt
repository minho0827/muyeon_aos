package com.muyeon.network.api

import com.muyeon.network.dto.policy.PolicyDocDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import com.muyeon.network.interceptor.NetworkRetryInterceptor

/** 출처: policy-documents.controller.ts(비로그인 허용). 실패해도 묻지 않는다(웹 silent — 기본 문구로 폴백). */
interface PolicyApi {
    @Headers("${NetworkRetryInterceptor.SILENT_HEADER}: 1")
    @GET(ApiEndpoints.Policies.DOC)
    suspend fun doc(@Path("docKey") docKey: String): Response<PolicyDocDto>

    @Headers("${NetworkRetryInterceptor.SILENT_HEADER}: 1")
    @GET(ApiEndpoints.Policies.VERSION)
    suspend fun version(@Path("docKey") docKey: String, @Path("version") version: String): Response<PolicyDocDto>
}
