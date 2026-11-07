package com.muyeon.app.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 지금 탭 다시 누르기 신호(웹 hooks/useTabReselect · 'muyeon:tab-reselect').
 *   듣는 탭은 다음 칩으로 넘기고 맨 위로(강사·커뮤니티·공연), 듣지 않는 탭은 [onReselect] 에서 맨 위로만.
 */
val LocalTabReselect = compositionLocalOf<SharedFlow<String>> { MutableSharedFlow() }

@Composable
fun TabReselectEffect(tab: MainTab, onReselect: suspend () -> Unit) {
    val flow = LocalTabReselect.current
    val latest = rememberUpdatedState(onReselect)
    LaunchedEffect(flow, tab) {
        flow.collect { key -> if (key == tab.key) latest.value() }
    }
}
