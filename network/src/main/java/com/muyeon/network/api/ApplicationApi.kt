package com.muyeon.network.api

import com.muyeon.network.dto.application.MyApplicationsPageDto
import com.muyeon.network.dto.application.ReceivedApplicationsPageDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** 출처: me.controller.ts(applications · received-applications). */
interface ApplicationApi {
    @GET(ApiEndpoints.Applications.MINE_PAGE)
    suspend fun minePage(
        @Query("tab") tab: String, @Query("period") period: String?, @Query("cursor") cursor: String?, @Query("limit") limit: Int,
    ): Response<MyApplicationsPageDto>

    @POST(ApiEndpoints.Applications.MINE_WITHDRAW)
    suspend fun withdraw(@Path("kind") kind: String, @Path("id") id: Long): Response<ResponseBody>

    @DELETE(ApiEndpoints.Applications.MINE_ARCHIVE)
    suspend fun archiveMine(@Path("kind") kind: String, @Path("id") id: Long): Response<ResponseBody>

    @GET(ApiEndpoints.Applications.RECEIVED_PAGE)
    suspend fun receivedPage(
        @Query("kind") kind: String?, @Query("postingId") postingId: Long?, @Query("archived") archived: Boolean,
        @Query("cursor") cursor: String?, @Query("limit") limit: Int,
    ): Response<ReceivedApplicationsPageDto>

    @DELETE(ApiEndpoints.Applications.RECEIVED_ARCHIVE)
    suspend fun archiveReceived(@Path("kind") kind: String, @Path("id") id: Long): Response<ResponseBody>

    @POST(ApiEndpoints.Applications.RECEIVED_RESTORE)
    suspend fun restoreReceived(@Path("kind") kind: String, @Path("id") id: Long): Response<ResponseBody>

    @DELETE(ApiEndpoints.Applications.RECEIVED_PURGE)
    suspend fun purgeReceived(@Path("kind") kind: String, @Path("id") id: Long): Response<ResponseBody>
}
