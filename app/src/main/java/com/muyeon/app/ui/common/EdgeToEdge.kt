package com.muyeon.app.ui.common

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * ★ 2026-11-08 모든 화면 edge-to-edge(사용자 요청) — 상태바·내비게이션 바까지 앱이 그린다(탭 셸 MainActivity 와 같다).
 *   바 막은 투명, 아이콘은 밝은 바탕용(검정). 내비게이션 바 회색 대비 막은 끈다.
 */
fun ComponentActivity.applyEdgeToEdge() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
    )
}

/**
 * edge-to-edge 화면 내용 — 바깥 면([background])이 상태바·내비게이션 바 뒤까지 깔리고, 내용은 바·키보드·노치를 피한다.
 *   안쪽 화면이 이미 statusBarsPadding·imePadding 을 쓰면 그 여백은 여기서 먼저 소비돼 두 번 들어가지 않는다.
 */
fun ComponentActivity.setEdgeContent(background: Color = Color.White, content: @Composable () -> Unit) {
    // ★ 2026-11-14 무용연 테마를 씌운다 — 없으면 색을 안 준 시스템 부품이 안드로이드 기본 보라로 보였다.
    //  ★ 2026-11-23 '학원 인증 마무리하기' 플로팅을 모든 앱 화면에 띄운다 — 화면 안쪽 여백 밖(맨 위 층)에 둔다.
    //   탭 셸(MainActivity)·웹 창(WebViewActivity)은 이 함수를 쓰지 않고 FloatingOverlay 가 같은 플로팅을 그린다(화면당 하나).
    val activity = this
    setContent {
        com.muyeon.app.theme.MuyeonTheme {
            val suppress = androidx.compose.runtime.remember { com.muyeon.app.ui.floating.ResumeFloatingSuppress() }
            val api = androidx.compose.runtime.remember {
                com.muyeon.app.ui.floating.FloatingApi { com.muyeon.app.utils.TokenManager.getAccessToken(activity) }
            }
            androidx.compose.runtime.CompositionLocalProvider(com.muyeon.app.ui.floating.LocalResumeFloatingSuppress provides suppress) {
                Box(Modifier.fillMaxSize().background(background)) {
                    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) { content() }
                    com.muyeon.app.ui.floating.ResumeVerificationFloating(api, selfRefresh = true)
                }
            }
        }
    }
}
