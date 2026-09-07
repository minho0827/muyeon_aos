package com.muyeon.app.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.muyeon.app.chat.socket.ChatEvent
import com.muyeon.app.chat.socket.ChatEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 채팅 목록 상태 — **NavHost 바깥(ChatActivity)에서 생성**해 방을 다녀와도 살아남는다.
 *
 * ⚠️ 왜 화면 안에 두면 안 되나 —
 *   Navigation-Compose 는 다른 목적지로 이동하면 이전 목적지를 composition 에서 빼버린다.
 *   목록 안에 `remember` 로 두면 방에 들어갈 때마다 목록·필터가 통째로 사라져
 *   ① 돌아올 때마다 스켈레톤이 번쩍이고 ② 골라둔 필터가 '전체'로 리셋됐다.
 *
 * 갱신 경로는 4가지이고 전부 [requestReload] 로 합류시켜 폭주를 막는다:
 *   1) 최초 진입          2) 화면 복귀(ON_RESUME)
 *   3) 소켓 room-updated  4) 소켓 재연결
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

        // 재조회 루프 — 요청이 몰려도 200ms 안의 것들은 한 번으로 합친다.
        //  ⚠️ 서버는 메시지 1건마다 chat-room-added 와 room-updated 를 **둘 다** 쏜다
        //    (구버전 하위호환). 합치지 않으면 대화가 오갈 때마다 전체 목록을 다시 받는다.
        scope.launch {
            for (unused in reloadRequests) {
                delay(200)
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
                    // 요약이 없는 갱신(수정·삭제 등)은 재조회로 맞춘다.
                    is ChatEvent.RoomUpdated -> requestReload()
                    // 끊겼다 붙는 동안 온 이벤트는 유실된다(replay=0). 붙자마자 전체를 다시 맞춘다.
                    is ChatEvent.Reconnected -> requestReload()
                    else -> Unit
                }
            }
        }
    }

    /** 합쳐서 처리되는 재조회 요청. 여러 번 불러도 안전하다. */
    fun requestReload() {
        reloadRequests.trySend(Unit)
    }

    /**
     * 목록 조회.
     *  @param silent 이미 목록이 있으면 스켈레톤을 띄우지 않는다(iOS `if rooms.isEmpty` 규칙).
     */
    suspend fun load(silent: Boolean = false) {
        if (!silent && rooms.isEmpty()) isLoading = true
        api.getRooms()
            .onSuccess { list ->
                rooms = list.sortedByDescending { it.lastMessageAt ?: "" }
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
        rooms = (listOf(room) + rooms.filterNot { it.roomId == room.roomId })
            .sortedByDescending { it.lastMessageAt ?: "" }
    }
}
