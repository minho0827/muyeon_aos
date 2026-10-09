package com.muyeon.app.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.muyeon.app.chat.socket.ChatEvent
import com.muyeon.app.chat.socket.ChatEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 채팅 목록 상태 — **NavHost 바깥(ChatActivity)에서 생성**해 방을 다녀와도 살아남는다.
 *
 * ⚠️ 왜 화면 안에 두면 안 되나 —
 *   Navigation-Compose 는 다른 목적지로 이동하면 이전 목적지를 composition 에서 빼버린다.
 *   목록 안에 `remember` 로 두면 방에 들어갈 때마다 목록·필터가 통째로 사라져
 *   ① 돌아올 때마다 스켈레톤이 번쩍이고 ② 골라둔 필터가 '전체'로 리셋됐다.
 *
 * 소켓 신호는 재조회 없이 해당 행만 갱신한다(2026-10-09 서버):
 *   - chat-room-added: 방 요약 전체 → upsert
 *   - room-updated: 미리보기·순번·안읽음 → [applyRoomUpdate] 로 해당 행만 갱신
 * 행을 갱신할 수 없는 경우(구버전 서버 payload·목록에 없는 방)와 아래 경로만 [requestReload] 로 합류한다:
 *   1) 최초 진입          2) 화면 복귀(ON_RESUME)
 *   3) 패치 불가 room-updated  4) 소켓 재연결
 *   5) 방·차단목록에서 NavHost 복귀(ChatActivity — CHAT_ROOMS 결과 키)
 */
class ChatListState(private val api: ChatApi) {

    var rooms by mutableStateOf<List<ChatRoomSummary>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var loadFailed by mutableStateOf(false)
        private set
    var refreshing by mutableStateOf(false)
        private set

    /** 필터도 여기 둔다 — 방을 다녀와도 유지되어야 한다. */
    var filter by mutableStateOf(ChatRoomFilter.ALL)

    /** 재조회 요청 큐. CONFLATED — 밀린 요청은 마지막 1건만 남는다. */
    private val reloadRequests = Channel<Unit>(Channel.CONFLATED)

    /** 대기 중인 요청 중 즉시 실행 요청이 있는지(재연결·화면 복귀). */
    private var immediateReload = false

    /**
     * 지금 열려 있는 방 — 이 방의 안읽음은 항상 0 으로 표시한다.
     *  서버는 그 방을 보고 있는 소켓에 room-updated 를 보내지 않지만 chat-room-added 요약은 보내며,
     *  읽음 처리(300ms 묶음)보다 먼저 계산된 안읽음 수가 실려 올 수 있다.
     */
    var activeRoomId: Int? = null
        set(value) {
            field = value
            if (value != null) clearUnread(value)
        }

    /** 소켓 구독·재조회 루프를 한 번만 띄우기 위한 가드(화면 재진입 시 중복 방지). */
    private var started = false

    fun setFilterFrom(initial: ChatRoomFilter) {
        // 웹 브릿지가 넘긴 초기 필터는 최초 1회만 반영한다(사용자가 바꾼 뒤 덮어쓰지 않도록).
        if (!started) filter = initial
    }

    /**
     * 소켓 구독 + 재조회 루프 시작. ChatActivity 에서 1회 호출.
     *  scope 는 Activity 수명(setContent 의 rememberCoroutineScope)에 묶는다.
     */
    fun start(scope: CoroutineScope) {
        if (started) return
        started = true

        // 재조회 루프 — 첫 요청 후 800ms 안에 들어온 요청은 한 번으로 합치고, 조회는 한 번에 하나만 실행한다.
        //  조회 중 들어온 요청은 끝난 뒤 한 번 더 실행된다(CONFLATED 채널에 1건 남는다).
        //  즉시 요청(재연결·화면 복귀)은 대기 없이 바로 실행한다.
        scope.launch {
            for (unused in reloadRequests) {
                if (!immediateReload) {
                    withTimeoutOrNull(RELOAD_DEBOUNCE_MS) {
                        while (!immediateReload) reloadRequests.receive()
                    }
                }
                immediateReload = false
                while (reloadRequests.tryReceive().isSuccess) Unit // 그 사이 쌓인 것 흡수
                load(silent = true)
            }
        }

        scope.launch {
            ChatEventBus.events.collect { e ->
                when (e) {
                    // 요약을 실어 오는 증분 갱신 — 재조회 없이 그 방만 갈아끼운다.
                    is ChatEvent.ChatRoomAdded -> upsert(e.room)
                    // 다른 기기·웹에서 나간 방 — 여기서도 즉시 지운다.
                    is ChatEvent.ChatRoomRemoved ->
                        rooms = rooms.filterNot { it.roomId == e.roomId }
                    // 미리보기·안읽음을 실은 갱신은 그 행만 고치고, 패치할 수 없으면 묶어서 재조회한다.
                    is ChatEvent.RoomUpdated -> applyRoomUpdate(e.update)
                    // 끊겼다 붙는 동안 온 이벤트는 유실된다(replay=0). 붙자마자 전체를 다시 맞춘다.
                    is ChatEvent.Reconnected -> requestReload(immediate = true)
                    // 읽음 처리 — 내 읽음은 서버가 room-updated{unreadCount:0} 으로 따로 알려 준다.
                    //  messages-read 는 join 한 방(=지금 보고 있는 방)에서만 오므로 그 방만 0 으로 둔다.
                    is ChatEvent.MessagesRead -> if (e.roomId == activeRoomId) clearUnread(e.roomId)
                    else -> Unit
                }
            }
        }
    }

    /**
     * 합쳐서 처리되는 재조회 요청. 여러 번 불러도 안전하다.
     *  @param immediate true 면 800ms 대기 없이 바로 실행(재연결·화면 복귀).
     */
    fun requestReload(immediate: Boolean = false) {
        if (immediate) immediateReload = true
        reloadRequests.trySend(Unit)
    }

    /**
     * room-updated payload 로 해당 행만 갱신한다.
     *  - 미리보기: payload 의 lastMessageAt 이 현재 값보다 최신이거나 같을 때만(늦게 도착한 신호가 덮어쓰지 않게)
     *  - 안읽음: 보고 있는 방은 0, unreadCount 가 있으면 그 값, 없으면 +unreadDelta
     *  목록에 없는 방이거나 갱신할 값이 없는 payload(구버전 서버 {roomId}, 수정·삭제 신호)는 재조회한다.
     */
    private fun applyRoomUpdate(u: ChatRoomUpdate) {
        val idx = rooms.indexOfFirst { it.roomId == u.roomId }
        if (idx < 0 || !u.canPatch) {
            requestReload()
            return
        }
        var r = rooms[idx]
        if (u.lastMessageAt != null && isNotOlder(u.lastMessageAt, r.lastMessageAt)) {
            r = r.copy(
                lastMessage = u.lastMessage ?: r.lastMessage,
                lastMessageAt = u.lastMessageAt,
            )
        }
        if (u.lastSeq != null && u.lastSeq > (r.lastSeq ?: 0)) r = r.copy(lastSeq = u.lastSeq)
        val unread = when {
            u.roomId == activeRoomId -> 0
            u.unreadCount != null -> u.unreadCount
            u.unreadDelta != null && u.unreadDelta > 0 -> r.unreadCount + u.unreadDelta
            else -> r.unreadCount
        }
        if (unread != r.unreadCount) r = r.copy(unreadCount = unread)
        if (r == rooms[idx]) return
        rooms = rooms.toMutableList().also { it[idx] = r }.sortedByDescending { it.lastMessageAt ?: "" }
    }

    /** a 가 b 보다 이전이 아니면 true. 시각을 해석하지 못하면 갱신 쪽으로 판단한다. */
    private fun isNotOlder(a: String, b: String?): Boolean {
        if (b == null) return true
        val ta = com.muyeon.app.ui.quote.QuoteUi.parseDate(a) ?: return true
        val tb = com.muyeon.app.ui.quote.QuoteUi.parseDate(b) ?: return true
        return ta >= tb
    }

    private fun clearUnread(roomId: Int) {
        val idx = rooms.indexOfFirst { it.roomId == roomId }
        if (idx < 0 || rooms[idx].unreadCount == 0) return
        rooms = rooms.toMutableList().also { it[idx] = it[idx].copy(unreadCount = 0) }
    }

    /**
     * 목록 조회.
     *  @param silent 이미 목록이 있으면 스켈레톤을 띄우지 않는다(iOS `if rooms.isEmpty` 규칙).
     */
    suspend fun load(silent: Boolean = false) {
        if (!silent && rooms.isEmpty()) isLoading = true
        api.getRooms()
            .onSuccess { list ->
                val active = activeRoomId
                rooms = list
                    .map { if (it.roomId == active && it.unreadCount != 0) it.copy(unreadCount = 0) else it }
                    .sortedByDescending { it.lastMessageAt ?: "" }
                loadFailed = false
            }
            // 캐시가 있으면 조용히 유지 — 일시적 실패로 목록을 비우지 않는다.
            .onFailure { if (rooms.isEmpty()) loadFailed = true }
        isLoading = false
    }

    suspend fun pullToRefresh() {
        refreshing = true
        load(silent = true)
        refreshing = false
    }

    suspend fun retry() {
        isLoading = true
        load()
    }

    /** 스와이프 나가기 — 낙관적 제거 후 서버 반영, 실패하면 재조회로 되돌린다. */
    suspend fun leave(roomId: Int) {
        rooms = rooms.filterNot { it.roomId == roomId }
        api.leaveRoom(roomId).onFailure { load(silent = true) }
    }

    private fun upsert(room: ChatRoomSummary) {
        val fixed = if (room.roomId == activeRoomId && room.unreadCount != 0) room.copy(unreadCount = 0) else room
        rooms = (listOf(fixed) + rooms.filterNot { it.roomId == room.roomId })
            .sortedByDescending { it.lastMessageAt ?: "" }
    }

    private companion object {
        const val RELOAD_DEBOUNCE_MS = 800L
    }
}
