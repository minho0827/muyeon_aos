package com.muyeon.app.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.jwt.JWT
import com.muyeon.app.chat.socket.ChatEvent
import com.muyeon.app.chat.socket.ChatEventBus
import com.muyeon.app.chat.socket.ChatSocketManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 채팅방 상태 — iOS `ChatRoomViewModel.swift` 이식.
 *  - 낙관적 전송(PendingMessage) + 소켓 에코 재조정 + 5초 실패 처리
 *  - 초기 로드 레이스 머지(로드 중 도착한 소켓 메시지 보존)
 *  - 위로 스크롤 페이지네이션(이전 메시지 prepend)
 *  - 읽음 처리 디바운스(200ms) / 타이핑 3초 자동 해제
 *  - 방별 입력 드래프트(인메모리)
 */
class ChatRoomViewModel(
    val roomId: Int,
    initialTitle: String,
    private val api: ChatApi,
    token: String?,
) : ViewModel() {

    /** 전송 대기(낙관적 삽입). 소켓 에코가 오면 제거된다. */
    data class Pending(
        val localId: String = UUID.randomUUID().toString(),
        val type: String,
        val content: String,
        val imageUrl: String?,
        val replyToId: Int?,
        var failed: Boolean = false,
    )

    val messages = mutableStateListOf<ChatMessage>()
    val pending = mutableStateListOf<Pending>()

    var title by mutableStateOf(initialTitle)
    var isLoading by mutableStateOf(false)
    var isLoadingMore by mutableStateOf(false)
    var input by mutableStateOf("")
        private set
    var opponentLastReadAt by mutableStateOf<Long?>(null)   // 내 메시지 '읽음' 판정 기준
    var isOtherTyping by mutableStateOf(false)
    var muted by mutableStateOf(false)
    var opponentImage by mutableStateOf<String?>(null)
    var opponentId by mutableStateOf(0)
    var quoteContext by mutableStateOf<ChatQuoteContext?>(null)
    var lessonSchedule by mutableStateOf<ChatLessonSchedule?>(null)
    var lessonCycles by mutableStateOf<List<ChatLessonCycle>>(emptyList())
    var quickReplies by mutableStateOf<List<ChatQuickReply>>(emptyList())
    var replyingTo by mutableStateOf<ChatMessage?>(null)
    var editingMessage by mutableStateOf<ChatMessage?>(null)
    var isUploadingMedia by mutableStateOf(false)
    var toast by mutableStateOf<String?>(null)

    /** 메시지 좌/우 정렬 기준 — JWT sub 에서 추출(iOS userIdFromToken 동일). */
    val currentUserId: Int = runCatching {
        token?.takeIf { it.isNotBlank() }?.let { JWT.decode(it).subject?.toIntOrNull() } ?: 0
    }.getOrDefault(0)

    /** 제안 카드·신고 시트가 쓰는 토큰(뷰가 다시 만들지 않도록 VM 이 보관). */
    val tokenForCards: String? = token

    private var totalCount = 0
    private var loadedPages = 1
    private val pageLimit = 50
    private var markReadJob: Job? = null
    private var typingJob: Job? = null
    private var lastTypingSent = false
    private var loadingInitial = false

    val hasMore: Boolean get() = messages.size < totalCount

    init {
        collectSocket()
    }

    // ============================================================
    // 로드
    // ============================================================

    fun start() {
        ChatSocketManager.joinRoom(roomId)
        input = ChatDrafts.get(roomId)
        viewModelScope.launch { loadDetail(); loadFirstPage(); loadQuickReplies() }
    }

    override fun onCleared() {
        ChatSocketManager.leaveRoom(roomId)
        if (lastTypingSent) ChatSocketManager.sendTyping(roomId, false)
        super.onCleared()
    }

    private suspend fun loadDetail() {
        api.getRoomDetail(roomId).onSuccess { d ->
            d.opponent?.let {
                opponentId = it.id
                opponentImage = it.image
                if (title.isBlank()) title = it.nickname ?: it.name ?: "채팅"
            }
            opponentLastReadAt = com.muyeon.app.ui.quote.QuoteUi.parseDate(d.opponentLastReadAt)
            muted = d.muted == true
            quoteContext = d.quoteContext
            lessonSchedule = d.lessonSchedule
            lessonCycles = d.lessonCycles ?: emptyList()
        }
    }

    private suspend fun loadFirstPage() {
        isLoading = messages.isEmpty()
        loadingInitial = true
        api.getMessages(roomId, page = 1, limit = pageLimit).onSuccess { res ->
            totalCount = res.total
            loadedPages = 1
            // 레이스 머지 — 로드 중 소켓으로 들어온 메시지를 잃지 않는다.
            val socketArrived = messages.filter { m -> res.messages.none { it.id == m.id } }
            messages.clear()
            messages.addAll((res.messages + socketArrived).sortedBy { it.id })
        }
        loadingInitial = false
        isLoading = false
        scheduleMarkRead()
    }

    /** 위로 스크롤 — 이전 페이지 prepend. */
    fun loadMore() {
        if (isLoadingMore || !hasMore) return
        isLoadingMore = true
        viewModelScope.launch {
            api.getMessages(roomId, page = loadedPages + 1, limit = pageLimit).onSuccess { res ->
                loadedPages += 1
                totalCount = res.total
                val existing = messages.map { it.id }.toSet()
                val older = res.messages.filterNot { existing.contains(it.id) }
                messages.addAll(0, older.sortedBy { it.id })
            }
            isLoadingMore = false
        }
    }

    private suspend fun loadQuickReplies() {
        api.getQuickReplies(roomId).onSuccess { quickReplies = it }
    }

    // ============================================================
    // 소켓 수신
    // ============================================================

    private fun collectSocket() {
        viewModelScope.launch {
            ChatEventBus.events.collect { e ->
                when (e) {
                    is ChatEvent.NewMessage -> if (e.roomId == roomId) onNewMessage(e.message)
                    is ChatEvent.MessageUpdated -> if (e.roomId == roomId) replaceMessage(e.message)
                    is ChatEvent.MessageDeleted -> if (e.roomId == roomId) replaceMessage(e.message)
                    is ChatEvent.MessagesRead ->
                        if (e.roomId == roomId && e.userId != currentUserId) {
                            opponentLastReadAt = com.muyeon.app.ui.quote.QuoteUi.parseDate(e.readAt)
                        }
                    is ChatEvent.Typing ->
                        if (e.roomId == roomId && e.userId != currentUserId) isOtherTyping = e.isTyping
                    is ChatEvent.MessageReaction ->
                        // 서버가 집계를 안 실어준다(뷰어별 mine 이 달라서) → 해당 페이지 재조회.
                        if (e.roomId == roomId) viewModelScope.launch { refreshReactions() }
                    else -> Unit
                }
            }
        }
    }

    private fun onNewMessage(msg: ChatMessage) {
        if (messages.any { it.id == msg.id }) return
        // 내 낙관적 메시지의 에코면 pending 제거(같은 발신자 + 같은 내용).
        if (msg.senderId == currentUserId) {
            val i = pending.indexOfFirst { it.content == msg.content && it.type == msg.type }
            if (i >= 0) pending.removeAt(i)
        }
        messages.add(msg)
        scheduleMarkRead()
    }

    private fun replaceMessage(msg: ChatMessage) {
        val i = messages.indexOfFirst { it.id == msg.id }
        if (i >= 0) messages[i] = msg
    }

    private suspend fun refreshReactions() {
        api.getMessages(roomId, page = 1, limit = pageLimit).onSuccess { res ->
            res.messages.forEach { fresh ->
                val i = messages.indexOfFirst { it.id == fresh.id }
                if (i >= 0) messages[i] = fresh
            }
        }
    }

    // ============================================================
    // 전송
    // ============================================================

    fun onInputChange(text: String) {
        input = text
        ChatDrafts.save(roomId, text)
        handleTypingChanged(text)
    }

    /** 입력 중 → 상대에게 typing emit. 3초 멈추면 자동 false(iOS typingTimer). */
    private fun handleTypingChanged(text: String) {
        val typing = text.isNotEmpty()
        if (typing != lastTypingSent) {
            ChatSocketManager.sendTyping(roomId, typing)
            lastTypingSent = typing
        }
        typingJob?.cancel()
        if (typing) {
            typingJob = viewModelScope.launch {
                delay(3000)
                ChatSocketManager.sendTyping(roomId, false)
                lastTypingSent = false
            }
        }
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty()) return

        // 수정 모드면 전송이 아니라 edit-message.
        editingMessage?.let { target ->
            ChatSocketManager.editMessage(roomId, target.id, text)
            editingMessage = null
            clearInput()
            return
        }

        val p = Pending(type = "TEXT", content = text, imageUrl = null, replyToId = replyingTo?.id)
        pending.add(p)
        val ok = ChatSocketManager.sendMessage(roomId, "TEXT", text, replyToId = p.replyToId)
        replyingTo = null
        clearInput()
        if (!ok) {
            markFailed(p)
        } else {
            // 5초 안에 에코가 안 오면 실패 표시(iOS 동일).
            viewModelScope.launch {
                delay(5000)
                if (pending.any { it.localId == p.localId }) markFailed(p)
            }
        }
    }

    fun retry(p: Pending) {
        pending.removeAll { it.localId == p.localId }
        val np = p.copy(localId = UUID.randomUUID().toString(), failed = false)
        pending.add(np)
        if (!ChatSocketManager.sendMessage(roomId, np.type, np.content, np.imageUrl, np.replyToId)) markFailed(np)
    }

    private fun markFailed(p: Pending) {
        val i = pending.indexOfFirst { it.localId == p.localId }
        if (i >= 0) pending[i] = pending[i].copy(failed = true)
    }

    private fun clearInput() {
        input = ""
        ChatDrafts.clear(roomId)
        if (lastTypingSent) {
            ChatSocketManager.sendTyping(roomId, false)
            lastTypingSent = false
        }
    }

    fun deleteMessage(m: ChatMessage) = ChatSocketManager.deleteMessage(roomId, m.id)

    /** 약속 제안 수락/거절/취소 후 — 진행 카드·일정 배너가 바뀌므로 상세를 다시 읽는다. */
    fun reloadContext() { viewModelScope.launch { loadDetail() } }

    /**
     * 사진 전송 — 업로드 후 imageUrl 을 콤마로 join 해 한 건으로 보낸다(iOS sendImage 규약).
     *  여러 장을 개별 메시지로 쪼개면 상대 화면에서 도배가 된다.
     */
    fun sendImages(context: android.content.Context, uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        isUploadingMedia = true
        viewModelScope.launch {
            val urls = uris.mapNotNull { uri ->
                readBytes(context, uri)?.let { api.uploadImage(it).getOrNull() }
            }
            isUploadingMedia = false
            if (urls.isEmpty()) { toast = "사진을 올리지 못했어요."; return@launch }
            val joined = urls.joinToString(",")
            val p = Pending(type = "IMAGE", content = "", imageUrl = joined, replyToId = null)
            pending.add(p)
            if (!ChatSocketManager.sendMessage(roomId, "IMAGE", "", joined)) markFailed(p)
        }
    }

    /** 동영상 전송 — 업로드 후 imageUrl 에 동영상 URL(iOS sendVideo 와 동일 규약). */
    fun sendVideo(context: android.content.Context, uri: android.net.Uri) {
        isUploadingMedia = true
        viewModelScope.launch {
            val bytes = readBytes(context, uri)
            val url = bytes?.let { api.uploadVideo(it).getOrNull() }
            isUploadingMedia = false
            if (url == null) { toast = "동영상을 올리지 못했어요."; return@launch }
            val p = Pending(type = "VIDEO", content = "", imageUrl = url, replyToId = null)
            pending.add(p)
            if (!ChatSocketManager.sendMessage(roomId, "VIDEO", "", url)) markFailed(p)
        }
    }

    private suspend fun readBytes(context: android.content.Context, uri: android.net.Uri): ByteArray? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        }

    fun toggleReaction(m: ChatMessage, emoji: String) {
        // 낙관적 갱신 후 서버 반영 — 소켓 에코(message-reaction)로 재조정.
        val i = messages.indexOfFirst { it.id == m.id }
        if (i >= 0) {
            val list = (messages[i].reactions ?: emptyList()).toMutableList()
            val j = list.indexOfFirst { it.emoji == emoji }
            if (j >= 0) {
                val r = list[j]
                val next = if (r.mine) r.count - 1 else r.count + 1
                if (next <= 0) list.removeAt(j) else list[j] = r.copy(count = next, mine = !r.mine)
            } else {
                list.add(ChatReaction(emoji, 1, true))
            }
            messages[i] = messages[i].copy(reactions = list)
        }
        viewModelScope.launch { api.toggleReaction(roomId, m.id, emoji) }
    }

    fun toggleMute(value: Boolean) {
        muted = value
        viewModelScope.launch { api.setRoomMute(roomId, value).onFailure { muted = !value } }
    }

    /** 읽음 처리 디바운스 — 메시지가 연속 도착해도 emit 은 200ms 에 한 번. */
    private fun scheduleMarkRead() {
        markReadJob?.cancel()
        markReadJob = viewModelScope.launch {
            delay(200)
            ChatSocketManager.markRead(roomId)
        }
    }
}

/** 방별 입력 드래프트(인메모리) — iOS ChatDraftManager. */
object ChatDrafts {
    private val drafts = java.util.concurrent.ConcurrentHashMap<Int, String>()
    fun save(roomId: Int, text: String) { if (text.isEmpty()) drafts.remove(roomId) else drafts[roomId] = text }
    fun get(roomId: Int): String = drafts[roomId] ?: ""
    fun clear(roomId: Int) { drafts.remove(roomId) }
}
