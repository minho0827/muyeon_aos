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
    setContent {
        Box(Modifier.fillMaxSize().background(background).windowInsetsPadding(WindowInsets.safeDrawing)) { content() }
    }
}
