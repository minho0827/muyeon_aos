package com.muyeon.app.ui.main

import androidx.annotation.StringRes
import com.muyeon.app.R
import com.muyeon.app.navigation.Screen
import com.muyeon.app.ui.main.bar.BarItem
import com.muyeon.app.ui.main.bar.BarMode

/**
 * ★ 2026-11-08 앱 탭 — 모든 회원·비회원이 같은 탭을 쓴다(유형별 서버 탭 목록은 쓰지 않는다).
 *   탭바 칸([barItem])과 탭바 모드([mode])는 탭에서 정해진다:
 *     홈 · 공연 · 공간(대관·양도양수) · MY      → MAIN
 *     채용(구인·구직) · 대타공고                 → JOBS (채용 2계층) · 캐스팅은 공연 칸 안(2026-11-09)
 *   LESSONS(예전 강사 목록)는 탭바에 없다 — 홈 '강사' 쪽이 대신한다. 웹 /lessons 링크는 이 화면으로 온다.
 */
enum class MainTab(
    val key: String,
    val webPath: String,
    @StringRes val labelRes: Int,
    val route: String,
    val barItem: BarItem,
    val mode: BarMode,
) {
    HOME("home", "/home", R.string.tab_home, Screen.TabHome.route, BarItem.HOME, BarMode.MAIN),
    PERFORMANCE("performance", "/performance", R.string.tab_performance, Screen.TabPerformance.route, BarItem.PERFORMANCE, BarMode.MAIN),
    // 공간 탭 안 위쪽 탭(대관·양도양수) — 탭바 칸은 공간.
    SPACES("spaces", "/spaces", R.string.tab_space_rent, Screen.TabSpaces.route, BarItem.SPACE, BarMode.MAIN),
    TRANSFERS("transfers", "/studio-transfers", R.string.tab_transfer, Screen.TabTransfers.route, BarItem.SPACE, BarMode.MAIN),
    // 채용 2계층 — 채용(안에 구인·구직 위쪽 탭) · 대타공고 · 캐스팅.
    JOBS("jobs", "/jobs", R.string.tab_jobs_hire, Screen.TabJobs.route, BarItem.JOBS, BarMode.JOBS),
    SEEK("seek", "/jobs?tab=seek", R.string.tab_jobs_seek, Screen.TabSeek.route, BarItem.JOBS, BarMode.JOBS),
    SUBS("subs", "/subs", R.string.tab_subs, Screen.TabSubs.route, BarItem.SUBS, BarMode.JOBS),
    // ★ 2026-11-09 캐스팅은 공연 칸 안(공연·캐스팅 통합 탭 — 사용자 결정). 채용 2계층에서 뺐다. 링크 /casting 은 그대로 이 탭.
    CASTING("casting", "/casting", R.string.tab_casting, Screen.TabCasting.route, BarItem.PERFORMANCE, BarMode.MAIN),
    MY("my", "/mypage", R.string.tab_my, Screen.TabMy.route, BarItem.MY, BarMode.MAIN),
    LESSONS("lessons", "/lessons", R.string.tab_lessons, Screen.TabLessons.route, BarItem.HOME, BarMode.MAIN);

    /**
     * 웹 주소(/jobs 등)를 이 앱 탭으로 돌려도 되나 — 앱 화면이 아직 없는 탭(구인구직·공간)은 웹 창이 그대로 그린다.
     *   (앱 탭 자리 화면이 그 웹 창을 여는데, 웹 창이 다시 앱 탭으로 돌아오면 서로 밀어내며 돈다.)
     */
    val nativeReady: Boolean get() = this !in NOT_READY

    companion object {
        /** 앱 화면을 만드는 중인 탭 — 만들면 여기서 뺀다(개편 계획 3·4단계). */
        private val NOT_READY = emptySet<MainTab>()

        fun fromKey(key: String): MainTab? = entries.firstOrNull { it.key == key }
        fun fromRoute(route: String?): MainTab? = entries.firstOrNull { it.route == route }

        /** 탭바 칸 → 그 칸이 여는 탭(부모 칸은 첫 하위 탭). BACK 은 null. */
        fun forBarItem(item: BarItem): MainTab? = when (item) {
            BarItem.HOME -> HOME
            BarItem.PERFORMANCE -> PERFORMANCE
            BarItem.SPACE -> SPACES
            BarItem.JOBS -> JOBS
            BarItem.SUBS -> SUBS
            BarItem.MY -> MY
            BarItem.BACK -> null
        }

        /** 웹 경로가 탭 루트인가(쿼리·끝 '/' 무시). 웹 창에서 이 주소로 가면 앱 탭으로 돌아온다. */
        fun fromWebPath(path: String?): MainTab? {
            val p = path?.substringBefore('?')?.substringBefore('#')?.trimEnd('/')?.ifEmpty { "/" } ?: return null
            if (p == "/") return HOME
            return entries.firstOrNull { it.webPath == p }
        }
    }
}
