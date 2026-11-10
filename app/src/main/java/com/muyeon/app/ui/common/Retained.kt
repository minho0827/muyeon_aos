package com.muyeon.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * ★ 2026-11-10 다른 화면에 갔다 와도 남는 값 — 화면(내비 기록 한 칸)의 ViewModel 에 맡겨 둔다.
 *  remember 는 상세로 넘어가면 화면이 구성에서 빠지며 사라져, 돌아올 때 목록을 다시 받는 동안 비어
 *  스크롤 위치가 맨 위로 돌아갔다(사용자 제보). 목록 데이터·목록 상태는 이걸로 쥔다.
 *  [key] 는 화면 안에서 겹치지 않게(계정마다 다르면 accountEpoch 를 붙인다).
 */
class RetainedValues : ViewModel() {
    internal val map = HashMap<String, Any>()
}

@Suppress("UNCHECKED_CAST")
@Composable
fun <T : Any> rememberRetained(key: String, init: () -> T): T {
    val holder: RetainedValues = viewModel(key = "muyeon.retained")
    return remember(holder, key) { holder.map.getOrPut(key, init) as T }
}

/**
 * 값이 '바뀌었을 때만' [onChange] — 처음 그릴 때와 다른 화면에서 돌아와 다시 그릴 때는 부르지 않는다.
 *  (LaunchedEffect(키){ scrollToItem(0) } 는 돌아올 때도 다시 돌아 스크롤이 맨 위로 갔다.)
 */
@Composable
fun OnValueChanged(value: Any?, onChange: suspend () -> Unit) {
    val sig = value.hashCode()
    var last by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(sig) {
        val prev = last
        last = sig
        if (prev != null && prev != sig) onChange()
    }
}
