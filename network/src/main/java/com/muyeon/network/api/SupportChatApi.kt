package com.muyeon.network.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** 출처: support-chat.controller.ts. 본문은 저장소가 org.json 으로 읽는다(메뉴·알림 모양이 넓다). */
interface SupportChatApi {
    @GET(ApiEndpoints.SupportChat.ELIGIBILITY) suspend fun eligibility(): Response<ResponseBody>
    @GET(ApiEndpoints.SupportChat.SUMMARY) suspend fun summary(): Response<ResponseBody>
    @GET(ApiEndpoints.SupportChat.NOTICES) suspend fun notices(): Response<ResponseBody>
    @POST(ApiEndpoints.SupportChat.NOTICES_READ_ALL) suspend fun readAllNotices(@Body body: RequestBody): Response<ResponseBody>
    @GET(ApiEndpoints.SupportChat.ROOMS) suspend fun rooms(): Response<ResponseBody>
    @POST(ApiEndpoints.SupportChat.ROOMS) suspend fun createRoom(@Body body: RequestBody): Response<ResponseBody>
    @GET(ApiEndpoints.SupportChat.MESSAGES) suspend fun messages(@Path("id") id: Long, @Query("after") after: Long): Response<ResponseBody>
    @POST(ApiEndpoints.SupportChat.MESSAGES) suspend fun send(@Path("id") id: Long, @Body body: RequestBody): Response<ResponseBody>
    @POST(ApiEndpoints.SupportChat.CHOOSE) suspend fun choose(@Path("id") id: Long, @Body body: RequestBody): Response<ResponseBody>
    @POST(ApiEndpoints.SupportChat.HANDOFF) suspend fun handoff(@Path("id") id: Long, @Body body: RequestBody): Response<ResponseBody>
    @POST(ApiEndpoints.SupportChat.CLOSE) suspend fun close(@Path("id") id: Long, @Body body: RequestBody): Response<ResponseBody>
    @POST(ApiEndpoints.SupportChat.READ) suspend fun read(@Path("id") id: Long, @Body body: RequestBody): Response<ResponseBody>
}
