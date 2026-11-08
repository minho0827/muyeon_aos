package com.muyeon.app.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.support.SupportChatMenu
import com.muyeon.domain.model.support.SupportChatMessage
import com.muyeon.domain.model.support.SupportChatNotice
import com.muyeon.domain.model.support.SupportChatRoomInfo
import com.muyeon.domain.model.support.SupportChatView
import com.muyeon.domain.usecase.support.SupportChatUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SupportChatListUiState(
    val rooms: List<SupportChatRoomInfo>? = null,
    val notices: List<SupportChatNotice> = emptyList(),
    val delayNotice: String? = null,
    /** 새 문의를 열 수 있다(eligible). */
    val canCreate: Boolean = false,
    val creating: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface SupportChatEvent {
    data class OpenRoom(val id: Long) : SupportChatEvent
    data class Message(val text: String) : SupportChatEvent
}

/** 상담 채팅 목록 — 웹 SupportChat ListView(고객센터 머리 · 지연 안내 · 안 읽은 알림 · 대화 · [새 문의하기]). */
class SupportChatListViewModel(private val chat: SupportChatUseCase) : ViewModel() {
    private val _state = MutableStateFlow(SupportChatListUiState())
    val state: StateFlow<SupportChatListUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<SupportChatEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<SupportChatEvent> = _events.asSharedFlow()

    /** 화면이 보일 때마다(운영자가 방금 연 방이 있을 수 있다). */
    fun load() {
        viewModelScope.launch {
            (chat.access() as? ApiResult.Success)?.let { r -> _state.update { it.copy(canCreate = r.data.eligible) } }
        }
        viewModelScope.launch {
            when (val r = chat.rooms()) {
                is ApiResult.Success -> _state.update { it.copy(rooms = r.data, errorMessage = null) }
                is ApiResult.Error -> _state.update { it.copy(rooms = it.rooms ?: emptyList(), errorMessage = if (it.rooms == null) r.message else null) }
            }
        }
        viewModelScope.launch { (chat.summary() as? ApiResult.Success)?.let { r -> _state.update { it.copy(delayNotice = r.data.delayNotice) } } }
        viewModelScope.launch { (chat.notices() as? ApiResult.Success)?.let { r -> _state.update { it.copy(notices = r.data) } } }
    }

    fun readAllNotices() {
        viewModelScope.launch {
            if (chat.readAllNotices() is ApiResult.Success) _state.update { s -> s.copy(notices = s.notices.map { it.copy(isRead = true) }) }
        }
    }

    fun create() {
        if (_state.value.creating) return
        _state.update { it.copy(creating = true) }
        viewModelScope.launch {
            when (val r = chat.createRoom()) {
                is ApiResult.Success -> if (r.data > 0) _events.tryEmit(SupportChatEvent.OpenRoom(r.data))
                is ApiResult.Error -> _events.tryEmit(SupportChatEvent.Message(r.message))
            }
            _state.update { it.copy(creating = false) }
        }
    }
}

data class SupportChatRoomUiState(
    val room: SupportChatRoomInfo? = null,
    val messages: List<SupportChatMessage> = emptyList(),
    val menu: SupportChatMenu? = null,
    val delayNotice: String? = null,
    val text: String = "",
    val busy: Boolean = false,
    /** 못 엶(403·404 등) — 빈 화면 + [MY로 가기]. */
    val failed: String? = null,
    val confirmClose: Boolean = false,
    val canCreate: Boolean = false,
)

/**
 * 상담방 — 웹 SupportChat RoomView. 처음 읽고 읽음 표시 · 화면이 보이는 동안 3초마다 새 글 · 버튼(챗봇)·보내기·상담원 연결·종료는 한 번에 하나,
 *   응답에 after 이후 글이 실려 온다.
 */
class SupportChatRoomViewModel(private val roomId: Long, private val chat: SupportChatUseCase) : ViewModel() {
    private val _state = MutableStateFlow(SupportChatRoomUiState())
    val state: StateFlow<SupportChatRoomUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<SupportChatEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<SupportChatEvent> = _events.asSharedFlow()
    private var lastId = 0L
    private var poller: Job? = null
    private var inflight = false

    init {
        viewModelScope.launch {
            when (val r = chat.messages(roomId, 0)) {
                is ApiResult.Success -> { apply(r.data); chat.read(roomId) }
                is ApiResult.Error -> _state.update {
                    it.copy(failed = if (r.statusCode == 403 || r.statusCode == 404) NOT_FOUND else r.message)
                }
            }
        }
        viewModelScope.launch { (chat.summary() as? ApiResult.Success)?.let { r -> _state.update { it.copy(delayNotice = r.data.delayNotice) } } }
        viewModelScope.launch { (chat.access() as? ApiResult.Success)?.let { r -> _state.update { it.copy(canCreate = r.data.eligible) } } }
    }

    /** 새 글을 덧붙인다 — 운영자 답장이 새로 왔으면 true. */
    private fun apply(v: SupportChatView): Boolean {
        val fresh = v.messages.filter { it.id > lastId }
        _state.update { s -> s.copy(room = v.room ?: s.room, menu = v.menu, messages = s.messages + fresh) }
        if (fresh.isEmpty()) return false
        lastId = fresh.last().id
        return fresh.any { it.senderType == "ADMIN" }
    }

    /** 화면이 보이는 동안만 3초마다(웹 POLL_MS). */
    fun startPolling() {
        poller?.cancel()
        poller = viewModelScope.launch {
            while (isActive) {
                delay(POLL_MS)
                if (inflight || lastId == 0L) continue
                inflight = true
                (chat.messages(roomId, lastId) as? ApiResult.Success)?.let { if (apply(it.data)) chat.read(roomId) }
                inflight = false
            }
        }
    }

    fun stopPolling() { poller?.cancel(); poller = null }

    fun onText(t: String) = _state.update { it.copy(text = t.take(SupportChatUseCase.MAX_LEN)) }

    private fun run(call: suspend (Long) -> ApiResult<SupportChatView>, onOk: () -> Unit = {}) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        inflight = true
        viewModelScope.launch {
            when (val r = call(lastId)) {
                is ApiResult.Success -> { apply(r.data); onOk() }
                is ApiResult.Error -> _events.tryEmit(SupportChatEvent.Message(r.message))
            }
            inflight = false
            _state.update { it.copy(busy = false) }
        }
    }

    fun send() {
        val t = _state.value.text.trim()
        if (t.isEmpty()) return
        run({ a -> chat.send(roomId, t, a) }) { _state.update { it.copy(text = "") } }
    }

    fun choose(nodeId: Long) = run({ a -> chat.choose(roomId, nodeId, a) })
    fun back() = run({ a -> chat.back(roomId, a) })
    fun home() = run({ a -> chat.home(roomId, a) })
    fun handoff() = run({ a -> chat.handoff(roomId, a) })
    fun askClose(ask: Boolean) = _state.update { it.copy(confirmClose = ask) }
    fun close() { _state.update { it.copy(confirmClose = false) }; run({ a -> chat.close(roomId, a) }) }

    companion object {
        private const val POLL_MS = 3000L
        const val NOT_FOUND = "종료되었거나 이 계정으로는 볼 수 없는 상담이에요. 로그인한 계정을 확인해 주세요."
    }
}
