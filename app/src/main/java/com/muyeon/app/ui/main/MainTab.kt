package com.muyeon.app.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.muyeon.app.R
import com.muyeon.app.navigation.Screen

/**
 * 하단 탭 종류 — 서버 탭 목록(GET /app-menus?surface=TAB)의 `key` 마지막 마디로 고른다.
 *
 *   [Kind.NATIVE]   앱 화면(MainScreen 안쪽 NavHost)
 *   [Kind.WEB]      웹 페이지 창(WebViewActivity)을 탭으로 연다 — 구인구직·대타(아직 웹). 그 창 아래에도 같은 탭바를 그린다.
 *
 *   서버가 모르는 키를 보내면 [fromKey] 가 null — 그 탭은 웹 경로로 연다(무반응 금지).
 */
enum class MainTab(
    val key: String,
    val webPath: String,
    val kind: Kind,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val route: String? = null,
) {
    HOME("home", "/home", Kind.NATIVE, R.string.tab_home, R.drawable.ic_tab_home, Screen.TabHome.route),
    LESSONS("lessons", "/lessons", Kind.NATIVE, R.string.tab_lessons, R.drawable.ic_tab_note, Screen.TabLessons.route),
    COMMUNITY("community", "/community", Kind.NATIVE, R.string.tab_community, R.drawable.ic_tab_community, Screen.TabCommunity.route),
    PERFORMANCE("performance", "/performance", Kind.NATIVE, R.string.tab_performance, R.drawable.ic_tab_star, Screen.TabPerformance.route),
    MY("my", "/mypage", Kind.NATIVE, R.string.tab_my, R.drawable.ic_tab_my, Screen.TabMy.route),
    JOBS("jobs", "/jobs", Kind.WEB, R.string.tab_jobs, R.drawable.ic_tab_note),
    SUBS("subs", "/subs", Kind.WEB, R.string.tab_subs, R.drawable.ic_tab_paper),
    /** 캐스팅·무대 — 첫 화면은 탭, 상세·목록은 탭 위 화면(StageRoutes.SHELL). 2026-11-08 액티비티에서 옮김. */
    CASTING("casting", "/casting", Kind.NATIVE, R.string.tab_casting, R.drawable.ic_tab_star, Screen.TabCasting.route);

    enum class Kind { NATIVE, WEB }

    companion object {
        fun fromKey(key: String): MainTab? = entries.firstOrNull { it.key == key }
        fun fromRoute(route: String?): MainTab? = entries.firstOrNull { it.route != null && it.route == route }

        /** 웹 경로가 탭 루트인가(쿼리·끝 '/' 무시). 웹 창에서 이 주소로 가면 앱 탭으로 돌아온다. */
        fun fromWebPath(path: String?): MainTab? {
            val p = path?.substringBefore('?')?.substringBefore('#')?.trimEnd('/')?.ifEmpty { "/" } ?: return null
            if (p == "/") return HOME
            return entries.firstOrNull { it.webPath == p }
        }

        /** 서버 아이콘 이름(웹 _icons.scss 클래스) → 그림. 모르는 이름이면 탭 기본 그림. */
        @DrawableRes
        fun iconFor(serverIcon: String?, tab: MainTab?): Int = when (serverIcon) {
            "filled-home" -> R.drawable.ic_tab_home
            "filled-note" -> R.drawable.ic_tab_note
            "filled-star" -> R.drawable.ic_tab_star
            "filled-community" -> R.drawable.ic_tab_community
            "filled-paper" -> R.drawable.ic_tab_paper
            "filled-my" -> R.drawable.ic_tab_my
            else -> tab?.iconRes ?: R.drawable.ic_tab_home
        }
    }
}

/** 탭바 한 칸(그리기 전용 값). */
data class TabBarItem(
    val key: String,
    val label: String,
    @DrawableRes val iconRes: Int,
    val showDot: Boolean = false,
)
