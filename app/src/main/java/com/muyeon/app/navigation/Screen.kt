package com.muyeon.app.navigation

/**
 * 앱 셸(MainActivity) 안 화면 경로의 단일 출처 — 문자열 경로 하드코딩 금지(PaceERA Screen.kt 와 같은 규칙).
 *   탭 루트는 [MainTab.route] 가 이 값을 쓴다. 상세 화면은 [build] 로 인자를 채운다.
 *   새 화면 = 여기 한 줄 + AppNavHost composable 한 줄 — docs/ARCHITECTURE.md 9단계 체크리스트 8번.
 */
sealed class Screen(val route: String) {
    /** 탭 셸(탭바 + 탭 화면). */
    data object Main : Screen("main")

    // === 탭 루트(MainScreen 안쪽 NavHost) ===
    data object TabHome : Screen("tab/home")
    data object TabLessons : Screen("tab/lessons")
    data object TabCommunity : Screen("tab/community")
    data object TabPerformance : Screen("tab/performance")
    data object TabMy : Screen("tab/my")

    // === 탭 위 상세(바깥 NavHost — 탭바가 없다) ===
    data object CommunityPost : Screen("community/post/{id}") {
        const val ARG_ID = "id"
        fun build(id: Long) = "community/post/$id"
    }

    data object PerformanceDetail : Screen("performance/{id}") {
        const val ARG_ID = "id"
        fun build(id: Long) = "performance/$id"
    }
}
