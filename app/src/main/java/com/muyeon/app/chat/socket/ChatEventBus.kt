package com.muyeon.app.chat.socket

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 채팅 socket 이벤트 → 여러 화면으로 broadcast 하는 singleton.
 *  PaceERA `ChatEventBus` 와 동일 패턴(object + SharedFlow).
 *
 *  - Producer: [ChatSocketManager] (/chat 이벤트 핸들러)
 *  - Consumer: 채팅 목록 화면 / 채팅방 화면
 *  - replay = 0 — 화면이 살아있을 때만 받는다(과거 이벤트 재생 금지).
 *  - extraBufferCapacity = 256 — 메시지가 몰릴 때 drop 방지(2026-10-09: 16 → 256).
 *    소켓 이벤트가 구독자 처리 속도보다 빨리 오면 초과분이 버려지고, 버려진 new-message 는
 *    다음 메시지의 순번 빈칸 감지로만 복구된다. 묶음 반영(50ms)과 맞춰 여유를 둔다.
 */
object ChatEventBus {

    private const val TAG = "ChatEventBus"

    private val _events = MutableSharedFlow<ChatEvent>(replay = 0, extraBufferCapacity = 256)
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    fun emit(e: ChatEvent) {
        if (!_events.tryEmit(e)) Log.w(TAG, "drop — buffer full event=${e::class.simpleName}")
    }
}
