package com.muyeon.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.muyeon.app.webview.ActiveRole

/**
 * ★ 2026-11-16 지금 유형(화면용) — 유형이 바뀌면 다시 그려진다.
 *   `remember { ActiveRole.isX(ctx) }` 처럼 한 번만 읽으면 MY 를 두 번 눌러 바꿔도 예전 버튼이 남는다. 이걸 쓴다.
 */
@Composable
fun rememberActiveType(): String {
    val ctx = LocalContext.current
    remember(ctx) { ActiveRole.bind(ctx) }
    return ActiveRole.type.collectAsState().value
}
