package com.muyeon.app.ui.assistant

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.muyeon.app.chat.socket.ChatEvent
import com.muyeon.app.chat.socket.ChatEventBus
import com.muyeon.app.chat.socket.ChatSocketLifecycleObserver
import com.muyeon.app.chat.socket.ChatSocketManager
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * AI 도우미 뱃지 — 플로팅 버튼 빨간 점, 전체 메뉴 숫자·칩 점(10-23, iOS AssistantBadgeStore 와 같은 규칙).
 *  - 숫자: GET /api/assistant/badges { notifications, chat, quotes, total } (웹 MY 배지와 같은 원천).
 *  - 실시간: 소켓(ChatEventBus) 새 메시지·방 갱신·방 추가·서버 알림·받은 견적·재연결마다 0.8초 묶어 한 번 재조회.
 *    화면이 앞에 올 때(onResume)도 다시 센다. 시간 반복(폴링)은 쓰지 않는다.
 *  - ★ 소켓은 원래 채팅 화면에 들어가야만 연결됐다 → 로그인 상태면 앱 어디서든 연결한다(토큰이 바뀔 때만 connect —
 *    연결 중에 또 부르면 소켓을 다시 만들기 때문).
 */
object AssistantBadges {
    var counts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var total by mutableIntStateOf(0)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pending: Job? = null
    private var started = false
    private var connectedToken: String? = null
    private lateinit var app: Context

    fun start(context: Context) {
        if (started) return
        started = true
        app = context.applicationContext
        scope.launch {
            ChatEventBus.events.collect { e ->
                when (e) {
                    is ChatEvent.NewMessage, is ChatEvent.RoomUpdated, is ChatEvent.ChatRoomAdded, is ChatEvent.ChatRoomRemoved,
                    is ChatEvent.MessagesRead, ChatEvent.ServerNotification, ChatEvent.QuoteBadge, ChatEvent.Reconnected -> schedule()
                    else -> Unit
                }
            }
        }
    }

    /** 화면이 앞에 올 때 — 소켓 연결 확인 + 다시 세기. */
    fun onScreenResumed(context: Context) {
        val token = TokenManager.getAccessToken(context)?.takeIf { it.isNotBlank() }
        if (token == null) {
            counts = emptyMap(); total = 0; connectedToken = null
            return
        }
        if (token != connectedToken) {
            connectedToken = token
            counts = emptyMap(); total = 0 // 계정이 바뀌면 옛 숫자를 지운다
            ChatSocketManager.connect(context)
            ChatSocketLifecycleObserver.ensureAttached(context)
        }
        schedule(300)
    }

    fun refreshSoon() = schedule(300)

    private fun schedule(delayMs: Long = 800) {
        if (pending != null) return
        pending = scope.launch {
            delay(delayMs)
            pending = null
            val b = AssistantApi.badges(app) ?: return@launch
            counts = b
            total = b.values.sum()
        }
    }

    fun count(key: String?): Int = key?.let { counts[it] } ?: 0
}
