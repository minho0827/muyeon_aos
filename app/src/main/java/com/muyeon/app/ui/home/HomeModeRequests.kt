package com.muyeon.app.ui.home

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 홈 모드를 바깥에서 고르는 요청 — 웹 /lessons 링크·전체 메뉴 '레슨 찾기'는 홈 [레슨] 으로 연다.
 *   홈 화면이 아직 만들어지기 전에 와도 받도록 하나는 쥐고 있다(받은 뒤 비운다).
 */
object HomeModeRequests {
    private val _flow = MutableSharedFlow<HomeMode>(replay = 1, extraBufferCapacity = 1)
    val flow: SharedFlow<HomeMode> = _flow.asSharedFlow()
    fun request(mode: HomeMode) { _flow.tryEmit(mode) }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun consumed() = _flow.resetReplayCache()
}
