package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.menu.AppMenuItem
import com.muyeon.domain.model.menu.AppMenuSurface

/** 서버 메뉴(탭·MY). 서버가 `X-Active-Type` 으로 유형을 고른다. */
interface AppMenuRepository {
    suspend fun menus(surface: AppMenuSurface): ApiResult<List<AppMenuItem>>

    /** 유형별 마지막 성공 응답(웹 localStorage `tabMenus:<type>` 와 같은 역할). 없으면 null. */
    fun cached(surface: AppMenuSurface, activeType: String): List<AppMenuItem>?
    fun saveCache(surface: AppMenuSurface, activeType: String, items: List<AppMenuItem>)
}
