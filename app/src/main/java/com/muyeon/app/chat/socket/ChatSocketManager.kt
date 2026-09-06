package com.muyeon.app.chat.socket

import android.content.Context
import android.util.Log
import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.chat.ChatMessage
import com.muyeon.app.ui.chat.ChatRoomSummary
import com.muyeon.app.utils.TokenManager
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 채팅 소켓 — PaceERA `chat/socket/SocketIOManager.kt` 구조를 무용연 계약에 맞춰 이식.
 *
 *  **구조는 PaceERA, 이벤트는 muyeon-backend `chat.gateway.ts`.**
 *   iOS(Muyeon)는 방용/목록용 SocketManager 를 각각 따로 연결하지만, Android 는 PaceERA 처럼
 *   앱 전체 단일 소켓 + room join ref-count 로 간다. 서버는 접속 시 자동으로 `user:{id}` 에
 *   join 시키므로 방에 join 하지 않아도 목록 갱신(chat-room-added/room-updated)은 받는다.
 *
 *  PaceERA 에서 가져온 함정 회피 3가지(모두 실기기 사고 이력):
 *   1. **인터셉터 없는 bare OkHttp** — 앱 공용 OkHttp 를 재사용하면 연결성 인터셉터가 재연결마다
 *      예외를 던져 'xhr poll error' 를 유발한다. 소켓은 자체 reconnection 으로 연결성을 관리한다.
 *      기본 readTimeout(10s)도 서버 pingInterval(25s)보다 짧아 idle long-poll 이 timeout 되므로
 *      readTimeout=0(무제한, ping/pong 으로 liveness 판단).
 *   2. **새 소켓 만들기 전 기존 소켓 off()+disconnect()** — forceNew 라 정리하지 않으면 이전 Manager
 *      가 누수돼 무한 재연결 루프가 된다.
 *   3. **room join ref-count** — Navigation Compose 가 race 로 화면(VM)을 두 번 만들면 두 번째의
 *      정리 콜백이 leave-room 을 쏴서 서버 room set 에서 빠지고 본인 메시지가 안 온다.
 *      join 마다 ref++, leave 마다 ref--, ref==0 일 때만 실제 leave emit.
 */
object ChatSocketManager {

    private const val TAG = "ChatSocket"

    /** 서버 → 클라 (chat.gateway.ts 가 emit 하는 것만). */
    private const val EV_NEW_MESSAGE = "new-message"
    private const val EV_MESSAGE_UPDATED = "message-updated"
    private const val EV_MESSAGE_DELETED = "message-deleted"
    private const val EV_MESSAGES_READ = "messages-read"
    private const val EV_MESSAGE_REACTION = "message-reaction"
    private const val EV_USER_TYPING = "user-typing"
    private const val EV_ROOM_UPDATED = "room-updated"
    private const val EV_CHAT_ROOM_ADDED = "chat-room-added"

    /** 클라 → 서버 (@SubscribeMessage). */
    private const val EV_JOIN_ROOM = "join-room"
    private const val EV_LEAVE_ROOM = "leave-room"
    private const val EV_SEND_MESSAGE = "send-message"
    private const val EV_MARK_READ = "mark-read"
    private const val EV_TYPING = "typing"
    private const val EV_EDIT_MESSAGE = "edit-message"
    private const val EV_DELETE_MESSAGE = "delete-message"

    /** 함정 1 — 인터셉터 없는 bare 클라이언트. */
    private val socketOkHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var socket: Socket? = null

    /**
     * 백그라운드 pause/resume 용 "연결 의도" 플래그.
     *  connect() 에서 true, 명시적 disconnect()(로그아웃 등)에서 false.
     *  [pauseForBackground] 는 소켓만 끊고 이 플래그는 보존한다.
     */
    @Volatile
    private var desired = false

    @Volatile
    private var token: String? = null

    /** 함정 3 — room:{id} → 활성 화면 refcount. */
    private val roomJoinRefs = ConcurrentHashMap<Int, Int>()

    // ============================================================
    // 연결 / 해제
    // ============================================================

    @Synchronized
    fun connect(context: Context) {
        val t = TokenManager.getAccessToken(context)?.takeIf { it.isNotBlank() }
        if (t == null) {
            Log.w(TAG, "connect skip — no token")
            return
        }
        token = t
        if (socket?.connected() == true) return

        // 함정 2 — 기존 소켓 정리 후 생성. roomJoinRefs 는 유지(재연결 시 재join).
        socket?.also { it.off(); it.disconnect() }
        socket = null
        desired = true

        val s = IO.socket(URI.create(BuildConfig.API_BASE_URL.trimEnd('/')), buildOptions(t))
        socket = s

        s.on(Socket.EVENT_CONNECT) {
            val active = roomJoinRefs.entries.filter { it.value > 0 }.map { it.key }
            Log.i(TAG, "/chat connected — re-joining ${active.size} room(s)")
            // 재연결 시 자동 재진입 — ref>0 인 방만.
            active.forEach { emitJoinRoom(it) }
        }
        s.on(Socket.EVENT_DISCONNECT) { args ->
            Log.i(TAG, "/chat disconnected reason=${args.firstOrNull()}")
        }
        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.w(TAG, "/chat connect_error — ${describeError(args)}")
        }

        // === 서버 이벤트 8종 ===

        s.on(EV_NEW_MESSAGE) { args -> onMessageEvent(args) { r, m -> ChatEvent.NewMessage(r, m) } }
        s.on(EV_MESSAGE_UPDATED) { args -> onMessageEvent(args) { r, m -> ChatEvent.MessageUpdated(r, m) } }
        s.on(EV_MESSAGE_DELETED) { args -> onMessageEvent(args) { r, m -> ChatEvent.MessageDeleted(r, m) } }

        s.on(EV_MESSAGES_READ) { args ->
            val json = asJson(args.firstOrNull()) ?: return@on
            val roomId = json.optInt("roomId", 0).takeIf { it > 0 } ?: return@on
            val userId = json.optInt("userId", 0).takeIf { it > 0 } ?: return@on
            ChatEventBus.emit(ChatEvent.MessagesRead(roomId, userId, json.optString("readAt")))
        }
        s.on(EV_MESSAGE_REACTION) { args ->
            val json = asJson(args.firstOrNull()) ?: return@on
            val roomId = json.optInt("roomId", 0).takeIf { it > 0 } ?: return@on
            val messageId = json.optInt("messageId", 0).takeIf { it > 0 } ?: return@on
            ChatEventBus.emit(ChatEvent.MessageReaction(roomId, messageId))
        }
        s.on(EV_USER_TYPING) { args ->
            val json = asJson(args.firstOrNull()) ?: return@on
            val roomId = json.optInt("roomId", 0).takeIf { it > 0 } ?: return@on
            val userId = json.optInt("userId", 0).takeIf { it > 0 } ?: return@on
            ChatEventBus.emit(ChatEvent.Typing(roomId, userId, json.optBoolean("isTyping", false)))
        }
        s.on(EV_ROOM_UPDATED) { args ->
            val json = asJson(args.firstOrNull()) ?: return@on
            val roomId = json.optInt("roomId", 0).takeIf { it > 0 } ?: return@on
            ChatEventBus.emit(ChatEvent.RoomUpdated(roomId))
        }
        s.on(EV_CHAT_ROOM_ADDED) { args ->
            val json = asJson(args.firstOrNull()) ?: return@on
            // 서버가 getRoomSummary() 결과를 통째로 실어 보낸다(리스트 upsert 용).
            runCatching { ChatRoomSummary.from(json) }
                .onSuccess { ChatEventBus.emit(ChatEvent.ChatRoomAdded(it)) }
                .onFailure { Log.w(TAG, "chat-room-added 파싱 실패: ${json.toString().take(200)}") }
        }

        // 알림(인증 승인 등) — 채팅과 무관하지만 같은 /chat 네임스페이스로 온다(iOS 와 동일).
        s.on("notification") { ChatEventBus.emit(ChatEvent.ServerNotification) }

        s.connect()
    }

    /** new-message / message-updated / message-deleted 는 payload 모양이 같다({roomId, message}). */
    private inline fun onMessageEvent(args: Array<Any?>, build: (Int, ChatMessage) -> ChatEvent) {
        val json = asJson(args.firstOrNull()) ?: return
        val roomId = json.optInt("roomId", 0).takeIf { it > 0 } ?: return
        val msgJson = json.optJSONObject("message") ?: return
        val msg = runCatching { ChatMessage.from(msgJson) }.getOrNull() ?: return
        ChatEventBus.emit(build(roomId, msg))
    }

    @Synchronized
    fun disconnect() {
        desired = false
        socket?.also { it.off(); it.disconnect() }
        socket = null
        roomJoinRefs.clear()
        Log.i(TAG, "/chat disconnect 완료")
    }

    /**
     * 백그라운드 진입(ProcessLifecycleOwner ON_STOP) — 소켓만 끊고 의도 플래그·ref 는 보존.
     *  끊지 않으면 reconnectionAttempts=MAX 라 화면이 꺼져도 재연결 로그가 폭주하고 배터리를 먹는다.
     *  백그라운드 알림은 FCM 이 담당하므로 단절해도 안전(chat.gateway 가 방 미접속자에게 푸시 발송).
     */
    @Synchronized
    fun pauseForBackground() {
        if (!desired) return
        Log.i(TAG, "pauseForBackground (의도 플래그·room ref 보존)")
        socket?.also { it.off(); it.disconnect() }
        socket = null
    }

    /** 포그라운드 복귀(ON_START) — 직전에 연결돼 있던 경우에만 재연결. connect() 는 idempotent. */
    @Synchronized
    fun resumeFromBackground(context: Context) {
        if (!desired) return
        Log.i(TAG, "resumeFromBackground")
        connect(context)
    }

    // ============================================================
    // 방 join / leave (ref-count)
    // ============================================================

    /** ref+1. ref==1(첫 진입)일 때만 실제 join-room emit. */
    fun joinRoom(roomId: Int) {
        val ref = roomJoinRefs.merge(roomId, 1) { old, _ -> old + 1 } ?: 1
        if (ref == 1) emitJoinRoom(roomId) else Log.i(TAG, "join-room ref=$ref → emit skip room=$roomId")
    }

    private fun emitJoinRoom(roomId: Int) {
        socket?.emit(EV_JOIN_ROOM, JSONObject().put("roomId", roomId))
        Log.i(TAG, "join-room emit room=$roomId")
    }

    /** ref-1. ref==0 일 때만 실제 leave-room emit. */
    fun leaveRoom(roomId: Int) {
        val ref = roomJoinRefs.compute(roomId) { _, old ->
            val v = (old ?: 0) - 1
            if (v <= 0) null else v
        } ?: 0
        if (ref == 0) {
            socket?.emit(EV_LEAVE_ROOM, JSONObject().put("roomId", roomId))
            Log.i(TAG, "leave-room ref=0 → emit room=$roomId")
        } else {
            Log.i(TAG, "leave-room ref=$ref → emit skip room=$roomId")
        }
    }

    // ============================================================
    // emit
    // ============================================================

    /**
     * 메시지 전송. payload 는 gateway `onSend` 시그니처와 동일
     *  ({roomId, type, content, imageUrl?, replyToId?}).
     *  미연결이면 false — 호출부가 안내/재시도를 결정한다(PaceERA 규약).
     */
    fun sendMessage(
        roomId: Int,
        type: String,
        content: String,
        imageUrl: String? = null,
        replyToId: Int? = null,
    ): Boolean {
        val s = socket
        if (s?.connected() != true) {
            Log.w(TAG, "send-message skip — not connected")
            return false
        }
        val payload = JSONObject()
            .put("roomId", roomId)
            .put("type", type)
            .put("content", content)
        if (!imageUrl.isNullOrBlank()) payload.put("imageUrl", imageUrl)
        if (replyToId != null) payload.put("replyToId", replyToId)
        s.emit(EV_SEND_MESSAGE, payload)
        return true
    }

    fun markRead(roomId: Int) {
        socket?.emit(EV_MARK_READ, JSONObject().put("roomId", roomId))
    }

    fun sendTyping(roomId: Int, isTyping: Boolean) {
        socket?.emit(EV_TYPING, JSONObject().put("roomId", roomId).put("isTyping", isTyping))
    }

    fun editMessage(roomId: Int, messageId: Int, content: String) {
        socket?.emit(
            EV_EDIT_MESSAGE,
            JSONObject().put("roomId", roomId).put("messageId", messageId).put("content", content),
        )
    }

    fun deleteMessage(roomId: Int, messageId: Int) {
        socket?.emit(EV_DELETE_MESSAGE, JSONObject().put("roomId", roomId).put("messageId", messageId))
    }

    // ============================================================
    // 헬퍼
    // ============================================================

    private fun buildOptions(token: String): IO.Options {
        val opts = IO.Options.builder()
            .setPath("/socket.io/")
            // 게이트웨이는 auth.token → query.token → Authorization 순으로 본다. auth 가 표준.
            .setAuth(mapOf("token" to token))
            .setForceNew(true)
            .setReconnection(true)
            .setReconnectionDelay(2_000L)
            .setReconnectionAttempts(Int.MAX_VALUE)
            .build()
        // 2.x 는 builder setter 가 없어 public 필드 직접 할당. polling·websocket 양쪽 모두 적용.
        opts.callFactory = socketOkHttp
        opts.webSocketFactory = socketOkHttp
        return opts
    }

    /** connect_error 의 표면 메시지("xhr poll error") 뒤에 숨은 실제 원인까지 펼친다. */
    private fun describeError(args: Array<Any?>): String {
        val first = args.firstOrNull()
        if (first !is Throwable) return first?.toString() ?: "?"
        return buildString {
            append(first.javaClass.simpleName).append(": ").append(first.message ?: "?")
            var cause = first.cause
            var depth = 0
            while (cause != null && depth < 3) {
                append(" | cause=").append(cause.javaClass.name).append(": ").append(cause.message ?: "?")
                cause = cause.cause
                depth++
            }
        }
    }

    private fun asJson(raw: Any?): JSONObject? = when (raw) {
        is JSONObject -> raw
        is String -> runCatching { JSONObject(raw) }.getOrNull()
        else -> null
    }
}
