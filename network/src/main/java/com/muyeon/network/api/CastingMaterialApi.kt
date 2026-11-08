package com.muyeon.network.api

import com.muyeon.network.dto.castingmaterial.CastingMaterialListDto
import com.muyeon.network.dto.castingmaterial.CreateCastingMaterialBody
import com.muyeon.network.dto.my.AllowedDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** ★ 2026-11-08 묶음 2 — 출처: casting-materials.controller.ts(access · list · create). */
interface CastingMaterialApi {
    @GET(ApiEndpoints.My.CASTING_MATERIAL_ACCESS) suspend fun access(): Response<AllowedDto>
    @GET(ApiEndpoints.CastingMaterial.LIST) suspend fun list(@Query("kind") kind: String): Response<CastingMaterialListDto>
    @POST(ApiEndpoints.CastingMaterial.LIST) suspend fun create(@Body body: CreateCastingMaterialBody): Response<ResponseBody>
}
