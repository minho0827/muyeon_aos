package com.muyeon.app.ui.home

import com.muyeon.app.ui.main.MainTab

/**
 * 홈이 여는 곳 — 탭 셸(MainScreen)이 채워 준다.
 *   [openPath] 웹 주소(앱으로 옮긴 주소면 앱 화면 · 아니면 웹 창 — NativeRoutes),
 *   [native] 기존 앱 화면(NativeActionRouter 액션), [openTab] 다른 탭.
 */
class HomeActions(
    val openPath: (String) -> Unit,
    val native: (String, Map<String, String>) -> Unit,
    val openTab: (MainTab) -> Unit,
    val login: () -> Unit,
    val dial: (String) -> Unit,
    val mail: (String) -> Unit,
)
