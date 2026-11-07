package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.data.local.MenuCache
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.menu.AppMenuItem
import com.muyeon.domain.model.menu.AppMenuSurface
import com.muyeon.domain.repository.AppMenuRepository
import com.muyeon.network.api.AppMenuApi
import com.squareup.moshi.Moshi

class AppMenuRepositoryImpl(
    private val api: AppMenuApi,
    private val moshi: Moshi,
    private val cache: MenuCache,
) : AppMenuRepository {
    override suspend fun menus(surface: AppMenuSurface): ApiResult<List<AppMenuItem>> =
        safeApiCall(moshi) { api.list(surface.value) }.map { it.toDomain() }

    override fun cached(surface: AppMenuSurface, activeType: String): List<AppMenuItem>? =
        cache.read(surface.value, activeType)

    override fun saveCache(surface: AppMenuSurface, activeType: String, items: List<AppMenuItem>) =
        cache.write(surface.value, activeType, items)
}
