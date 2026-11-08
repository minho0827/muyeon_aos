package com.muyeon.network.api

import com.muyeon.network.dto.academy.AcademyOwnerDto
import com.muyeon.network.dto.academy.AcademyProfileDto
import com.muyeon.network.dto.academy.AcademyPublicDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

/** ★ 2026-11-08 묶음 2 — 출처: auth.controller.ts(me · me/academy) · profiles.controller.ts(academies/:id). */
interface AcademyProfileApi {
    @GET(ApiEndpoints.AcademyProfile.ME) suspend fun load(): Response<AcademyProfileDto>
    @PATCH(ApiEndpoints.AcademyProfile.ME) suspend fun save(@Body body: AcademyProfileDto): Response<ResponseBody>
    @GET(ApiEndpoints.AcademyProfile.OWNER) suspend fun owner(): Response<AcademyOwnerDto>
    @GET(ApiEndpoints.AcademyProfile.PUBLIC) suspend fun academy(@Path("id") id: Long): Response<AcademyPublicDto>
}
