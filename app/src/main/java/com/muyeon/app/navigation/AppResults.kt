package com.muyeon.app.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * 다른 화면이 닫히며 돌려준 결과 키(result/ResultKeys — 세 플랫폼 공통 계약)를 탭 화면 ViewModel 에 알린다.
 *   PaceERA NavResults·collectNavResults 와 같은 자리. 웹뷰는 같은 키를 window.__muyeonResult 로 받는다.
 *
 *   보내는 쪽: MainActivity(네이티브 화면 결과)·WebViewActivity(웹 창을 닫을 때).
 *   받는 쪽: `viewModelScope.collectAppResults(ResultKeys.QUOTES) { refresh() }`.
 */
object AppResults {
    private val _events = MutableSharedFlow<Set<String>>(extraBufferCapacity = 16)
    val events: SharedFlow<Set<String>> = _events.asSharedFlow()

    fun publish(keys: Set<String>) {
        if (keys.isNotEmpty()) _events.tryEmit(keys)
    }
}

/** [keys] 중 하나라도 오면 [onResult]. 키를 비우면 모든 결과에 반응한다. */
fun CoroutineScope.collectAppResults(vararg keys: String, onResult: (Set<String>) -> Unit) = launch {
    AppResults.events.collect { got ->
        if (keys.isEmpty() || keys.any { it in got }) onResult(got)
    }
}
