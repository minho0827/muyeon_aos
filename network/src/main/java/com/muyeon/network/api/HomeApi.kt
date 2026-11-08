package com.muyeon.network.api

import com.muyeon.network.dto.common.CursorPageDto
import com.muyeon.network.dto.home.AcademyProfileDto
import com.muyeon.network.dto.home.HomeFeedItemDto
import com.muyeon.network.dto.home.HomeLessonDto
import com.muyeon.network.dto.home.HomeLessonPreviewDto
import com.muyeon.network.dto.home.HomeSpaceDto
import com.muyeon.network.dto.home.HomeNoticeDto
import com.muyeon.network.dto.home.HomeTransferPageDto
import com.muyeon.network.dto.home.MyApplicationDto
import com.muyeon.network.dto.home.MyPostingDto
import com.muyeon.network.dto.home.RecommendBlockBody
import com.muyeon.network.dto.home.TeacherHomePreviewDto
import com.muyeon.network.dto.teacher.TeacherCardDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** 출처: ApiEndpoints.Home 주석. 지역은 웹 regionParams 와 같이 regionCode(시군구) 또는 sido(시·도). */
interface HomeApi {
    @GET(ApiEndpoints.Home.LESSON_PREVIEW)
    suspend fun lessonPreview(@Query("regionCode") regionCode: String?, @Query("sido") sido: String?): Response<HomeLessonPreviewDto>

    /** 레슨 목록(커서) — 홈 '레슨' 장르 탭 · 새로 열린 레슨. */
    @GET(ApiEndpoints.Home.LESSON_PAGE)
    suspend fun lessonPage(@Query("genre") genre: String?, @Query("cursor") cursor: String?, @Query("limit") limit: Int): Response<CursorPageDto<HomeLessonDto>>

    /**
     * ★ 2026-11-08 홈 '전체보기' 레슨 목록(웹 LessonListView) — 홈 칸과 같은 조건.
     *   추천 = lessonKind=REGULAR · 체험 = lessonKind=EXPERIENCE&academyOnly=true · 원데이 = scheduled=true(서버가 7일 안 회차로 거른다).
     */
    @GET(ApiEndpoints.Home.LESSON_PAGE)
    suspend fun lessonListPage(
        @Query("genre") genre: String?, @Query("genreMatch") genreMatch: String?,
        @Query("lessonKind") lessonKind: String?, @Query("academyOnly") academyOnly: Boolean?, @Query("scheduled") scheduled: Boolean?,
        @Query("regionCode") regionCode: String?, @Query("sido") sido: String?,
        @Query("cursor") cursor: String?, @Query("limit") limit: Int,
    ): Response<CursorPageDto<HomeLessonDto>>

    /** 배열 또는 { items } — 저장소가 읽는다. */
    @GET(ApiEndpoints.Home.JOBS) suspend fun jobs(): Response<ResponseBody>
    @GET(ApiEndpoints.Home.SUBS) suspend fun subs(): Response<ResponseBody>
    @GET(ApiEndpoints.Home.SPACES) suspend fun spaces(): Response<List<HomeSpaceDto>>
    @GET(ApiEndpoints.Home.TRANSFERS) suspend fun transfers(@Query("limit") limit: Int): Response<HomeTransferPageDto>
    @GET(ApiEndpoints.Home.NOTICES) suspend fun notices(): Response<List<HomeNoticeDto>>
    @GET(ApiEndpoints.Home.QUOTES) suspend fun homeQuotes(): Response<ResponseBody>
    @POST(ApiEndpoints.Home.RECOMMEND_BLOCKS) suspend fun blockRecommendation(@Body body: RecommendBlockBody): Response<ResponseBody>

    @GET(ApiEndpoints.Home.ACADEMY_PROFILE) suspend fun academyProfile(): Response<AcademyProfileDto>
    @GET(ApiEndpoints.Home.RECOMMENDED_TEACHERS) suspend fun recommendedTeachers(): Response<List<TeacherCardDto>>
    @GET(ApiEndpoints.Home.MY_LESSONS) suspend fun myLessons(): Response<List<HomeLessonDto>>
    @GET(ApiEndpoints.Home.MY_POSTINGS) suspend fun myPostings(): Response<List<MyPostingDto>>

    @GET(ApiEndpoints.Home.TEACHER_PREVIEW)
    suspend fun teacherPreview(
        @Query("regionCode") regionCode: String?, @Query("sido") sido: String?,
        @Query("regionName") regionName: String?, @Query("today") today: String,
    ): Response<TeacherHomePreviewDto>

    @GET(ApiEndpoints.Home.FEED_PAGE)
    suspend fun feedPage(
        @Query("regionCode") regionCode: String?, @Query("sido") sido: String?,
        @Query("domain") domain: String, @Query("sub") sub: String, @Query("cursor") cursor: String?, @Query("limit") limit: Int,
    ): Response<CursorPageDto<HomeFeedItemDto>>

    @GET(ApiEndpoints.Home.MY_APPLICATIONS) suspend fun myApplications(): Response<List<MyApplicationDto>>
    @GET(ApiEndpoints.Home.MY_VIEWED_JOBS) suspend fun myViewedJobs(): Response<List<Long>>
}
