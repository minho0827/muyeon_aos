package com.muyeon.network.api

import com.muyeon.network.dto.write.CommunityPostBody
import com.muyeon.network.dto.write.SubEditDto
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/** ★ 2026-11-08 묶음 3 — 출처: subs.controller.ts(GET·PATCH :id) · community.controller.ts(POST /posts · PATCH /posts/:id). */
interface WriteApi {
    @GET(ApiEndpoints.Write.SUB)
    suspend fun sub(@Path("id") id: Long): Response<SubEditDto>

    /** 본문은 SubUpdateBody.of — 비운 급여를 null 로 보내야 해서 직접 만든 JSON. */
    @PATCH(ApiEndpoints.Write.SUB)
    suspend fun updateSub(@Path("id") id: Long, @Body body: RequestBody): Response<ResponseBody>

    @POST(ApiEndpoints.Write.POSTS)
    suspend fun createPost(@Body body: CommunityPostBody): Response<ResponseBody>

    @PATCH(ApiEndpoints.Write.POST)
    suspend fun updatePost(@Path("id") id: Long, @Body body: CommunityPostBody): Response<ResponseBody>
}
