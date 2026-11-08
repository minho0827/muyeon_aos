package com.muyeon.app.navigation

import android.net.Uri

/**
 * 앱 화면이 여는 웹 페이지 주소(아직 웹인 화면) — 한 곳에 모은다. 웹 MuyeonRouter 경로와 같아야 한다.
 */
object WebPaths {
    const val COMMUNITY_WRITE = "/community/write"
    const val SETTINGS = "/setting"
    /** 대표 지역 고르기(MY 대표 지역 줄). */
    const val MY_REGIONS = "/myRegions"

    /**
     * 로그인 — 마치면 [redirect] 로(웹 useLikes 와 같은 규칙).
     *  ★ 2026-11-08 이 주소는 웹 창으로 가지 않는다 — MainActivity.openWeb·openPath·WebViewActivity.startPath 가 앱 로그인 화면(LoginActivity)으로 바꾼다.
     */
    fun login(redirect: String): String = "/login?redirect=" + Uri.encode(redirect)
}
