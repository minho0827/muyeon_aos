package com.muyeon.app.navigation

import com.muyeon.app.ui.main.MainTab

/**
 * 웹 주소 → 앱 화면 표(앱으로 옮긴 화면만). 푸시·링크·웹 창 이동·네이티브 복귀(NativeWebRoute)가 모두 이 표를 본다.
 *   여기 없는 주소는 웹 창으로 연다. 새 앱 화면이 생기면 한 줄 — docs/ARCHITECTURE.md §1.
 */
object NativeRoutes {
    sealed interface Target {
        data class Tab(val tab: MainTab) : Target
        /** 탭 위 앱 화면(바깥 NavHost 경로). */
        data class Route(val route: String) : Target
    }

    private val COMMUNITY_POST = Regex("^/community/post/(\\d+)/?$")
    private val PERFORMANCE = Regex("^/performance/(\\d+)/?$")

    fun resolve(path: String?): Target? {
        val clean = path?.substringBefore('#') ?: return null
        val p = clean.substringBefore('?')
        COMMUNITY_POST.find(p)?.let { return Target.Route(Screen.CommunityPost.build(it.groupValues[1].toLong())) }
        PERFORMANCE.find(p)?.let { return Target.Route(Screen.PerformanceDetail.build(it.groupValues[1].toLong())) }
        // 탭 루트는 쿼리가 없을 때만(쿼리 = 웹 화면 상태 지정 — 웹이 그린다).
        if (clean.contains('?')) return null
        return MainTab.fromWebPath(p)?.takeIf { it.kind == MainTab.Kind.NATIVE }?.let { Target.Tab(it) }
    }
}
