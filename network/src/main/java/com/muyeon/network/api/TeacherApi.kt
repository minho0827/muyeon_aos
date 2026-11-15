package com.muyeon.network.api

import com.muyeon.network.dto.common.CursorPageDto
import com.muyeon.network.dto.teacher.TeacherCardDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** 출처: profiles.controller.ts teachers/page. 서버는 lesson 을 문자열 'true' 로만 인식한다. */
interface TeacherApi {
    @GET(ApiEndpoints.Teachers.PAGE)
    suspend fun page(
        @Query("lesson") lesson: String?,
        @Query("genre") genre: String?,
        @Query("field") field: String?,
        @Query("region") region: String?,
        @Query("regionKind") regionKind: String?,
        @Query("sort") sort: String?,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
        /** ★ 2026-11-15 홈 '강사 · 무용수' 그리드 — "1"이면 구직 공개한 무용수도 섞는다(카드에 role). */
        @Query("withDancers") withDancers: String? = null,
    ): Response<CursorPageDto<TeacherCardDto>>
}
