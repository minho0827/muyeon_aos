package com.muyeon.network.api

import com.muyeon.network.dto.common.CursorPageDto
import com.muyeon.network.dto.recruit.JobCardDto
import com.muyeon.network.dto.recruit.SeekerCardDto
import com.muyeon.network.dto.recruit.SubRowDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** 출처: ApiEndpoints.Recruit. 여러 개 고르는 필터는 쉼표로 이어 보낸다(웹 JobBoard). */
interface RecruitApi {
    @GET(ApiEndpoints.Recruit.JOBS_PAGE)
    suspend fun jobsPage(
        @Query("regionCode") regionCode: String?, @Query("sido") sido: String?,
        @Query("career") career: String?, @Query("salary") salary: String?,
        @Query("cursor") cursor: String?, @Query("limit") limit: Int,
    ): Response<CursorPageDto<JobCardDto>>

    @GET(ApiEndpoints.Recruit.JOBS_FOR_ME)
    suspend fun jobsForMe(@Query("limit") limit: Int): Response<List<JobCardDto>>

    @GET(ApiEndpoints.Recruit.SEEKERS_PAGE)
    suspend fun seekersPage(
        @Query("types") types: String?, @Query("fields") fields: String?, @Query("targets") targets: String?, @Query("careers") careers: String?,
        @Query("cursor") cursor: String?, @Query("limit") limit: Int,
    ): Response<CursorPageDto<SeekerCardDto>>

    @GET(ApiEndpoints.Recruit.SUBS)
    suspend fun subs(
        @Query("regionCode") regionCode: String?, @Query("sido") sido: String?,
        @Query("field") field: String?, @Query("target") target: String?, @Query("salary") salary: String?,
        @Query("scheduleType") scheduleType: String?, @Query("minClassCount") minClassCount: Int?,
    ): Response<List<SubRowDto>>
}
