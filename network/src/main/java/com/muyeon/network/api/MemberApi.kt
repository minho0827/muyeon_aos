package com.muyeon.network.api

import com.muyeon.network.dto.member.ActiveTypeBody
import com.muyeon.network.dto.member.MyRolesDto
import com.muyeon.network.dto.member.RoleHeroDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

/** 출처: roles.controller.ts(GET /me/roles) · auth.controller.ts(PATCH /auth/me/active-type). */
interface MemberApi {
    @GET(ApiEndpoints.Roles.MINE)
    suspend fun myRoles(): Response<MyRolesDto>

    /** 유형 편집 화면에 원문 그대로 넘긴다(그 화면이 held·roles 를 직접 읽는다). */
    @GET(ApiEndpoints.Roles.MINE)
    suspend fun myRolesRaw(): Response<ResponseBody>

    @GET(ApiEndpoints.Home.ROLE_HERO)
    suspend fun roleHero(): Response<RoleHeroDto>

    @PATCH(ApiEndpoints.Auth.ACTIVE_TYPE)
    suspend fun setActiveType(@Body body: ActiveTypeBody): Response<ResponseBody>
}
