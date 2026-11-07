package com.muyeon.domain.usecase.menu

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.menu.AppMenuItem
import com.muyeon.domain.model.menu.AppMenuSurface
import com.muyeon.domain.repository.AppMenuRepository

/**
 * 하단 탭 목록 — 웹 hooks/useTabMenus.js 와 같은 순서로 정한다.
 *   1) 서버 응답(성공하면 유형별 캐시에 저장)  2) 실패면 그 유형의 캐시  3) 캐시도 없으면 [fallbackTabs].
 *   빈 목록은 쓰지 않는다 — 탭바가 사라지면 앱을 쓸 수 없다.
 */
class GetTabMenusUseCase(private val repo: AppMenuRepository) {
    suspend operator fun invoke(activeType: String?): List<AppMenuItem> {
        val type = activeType?.uppercase() ?: ActiveType.GENERAL
        val r = repo.menus(AppMenuSurface.TAB)
        if (r is ApiResult.Success && r.data.isNotEmpty()) {
            repo.saveCache(AppMenuSurface.TAB, type, r.data)
            return r.data
        }
        return repo.cached(AppMenuSurface.TAB, type)?.takeIf { it.isNotEmpty() } ?: fallbackTabs(type)
    }

    /** 오프라인 기본 목록 — 문구는 화면이 tabKey 로 고른다(title = null). 웹 fallbackTabs 와 같은 구성. */
    fun cachedOrFallback(activeType: String?): List<AppMenuItem> {
        val type = activeType?.uppercase() ?: ActiveType.GENERAL
        return repo.cached(AppMenuSurface.TAB, type)?.takeIf { it.isNotEmpty() } ?: fallbackTabs(type)
    }

    companion object {
        fun fallbackTabs(activeType: String?): List<AppMenuItem> {
            fun tab(k: String, path: String, icon: String) = AppMenuItem("tab.$k", path, icon, null)
            return if (ActiveType.isPro(activeType)) listOf(
                tab("home", "/home", "filled-home"),
                tab("jobs", "/jobs", "filled-note"),
                tab("casting", "/casting", "filled-star"),
                tab("subs", "/subs", "filled-paper"),
                tab("my", "/mypage", "filled-my"),
            ) else listOf(
                tab("home", "/home", "filled-home"),
                tab("lessons", "/lessons", "filled-note"),
                tab("community", "/community", "filled-community"),
                tab("performance", "/performance", "filled-star"),
                tab("my", "/mypage", "filled-my"),
            )
        }
    }
}

/** MY 메뉴 — 서버 응답, 실패면 캐시. 둘 다 없으면 빈 목록(화면이 기본 메뉴를 그린다). */
class GetMyMenusUseCase(private val repo: AppMenuRepository) {
    suspend operator fun invoke(activeType: String?): ApiResult<List<AppMenuItem>> {
        val type = activeType?.uppercase() ?: ActiveType.GENERAL
        val r = repo.menus(AppMenuSurface.MY)
        if (r is ApiResult.Success && r.data.isNotEmpty()) repo.saveCache(AppMenuSurface.MY, type, r.data)
        if (r is ApiResult.Error) repo.cached(AppMenuSurface.MY, type)?.let { return ApiResult.Success(it) }
        return r
    }
}
