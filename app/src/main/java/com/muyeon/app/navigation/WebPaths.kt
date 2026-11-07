package com.muyeon.app.navigation

import android.net.Uri

/**
 * 앱 화면이 여는 웹 페이지 주소(아직 웹인 화면) — 한 곳에 모은다. 웹 MuyeonRouter 경로와 같아야 한다.
 */
object WebPaths {
    /** 로그인 — 마치면 [redirect] 로(웹 useLikes 와 같은 규칙). */
    fun login(redirect: String): String = "/login?redirect=" + Uri.encode(redirect)
}
