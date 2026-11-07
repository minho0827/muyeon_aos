package com.muyeon.network.api

import com.muyeon.network.dto.menu.AppMenuListDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** 출처: muyeon-backend `src/modules/app-menus/app-menus.controller.ts`. */
interface AppMenuApi {
    @GET(ApiEndpoints.AppMenus.LIST)
    suspend fun list(@Query("surface") surface: String): Response<AppMenuListDto>
}
