package com.muyeon.network.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 출처: profiles.controller.ts(dancers) · chat(rooms/direct) · me.controller.ts(postings).
 *   무용수 프로필은 자유 입력(profile JSON)을 그대로 펼쳐 보낸다 — 같은 칸이 글자/배열/숫자로 섞여 와서 본문은 저장소가 직접 읽는다.
 */
interface DancerApi {
    @GET(ApiEndpoints.Dancers.PAGE)
    suspend fun page(
        @Query("major") major: String?, @Query("gender") gender: String?,
        @Query("hasVideo") hasVideo: Boolean?, @Query("hasAward") hasAward: Boolean?,
        @Query("cursor") cursor: String?, @Query("limit") limit: Int,
    ): Response<ResponseBody>

    @GET(ApiEndpoints.Dancers.DETAIL)
    suspend fun detail(@Path("id") id: Long): Response<ResponseBody>

    @POST(ApiEndpoints.Dancers.OFFER)
    suspend fun offer(@Path("id") id: Long, @Body body: RequestBody): Response<ResponseBody>

    @POST(ApiEndpoints.Dancers.DIRECT_ROOM)
    suspend fun directRoom(@Body body: RequestBody): Response<ResponseBody>

    @GET(ApiEndpoints.Dancers.MY_POSTINGS)
    suspend fun myPostings(): Response<ResponseBody>
}
