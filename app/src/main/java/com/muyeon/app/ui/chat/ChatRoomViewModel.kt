package com.muyeon.app.ui.chat

import android.os.SystemClock
import android.util.Log
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
import com.muyeon.app.chat.socket.ChatSocketManager.SendResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 채팅방 상태 — iOS `ChatRoomViewModel.swift` 이식, 2026-10-09 PACERA iOS 4e3d3784 방식으로 개편.
 *  - 전송: 메시지마다 clientMsgId(UUID) + 서버 응답(ack)으로 확정/실패. 재시도는 같은 clientMsgId(서버 중복 저장 방지)
 *  - 합치기: 모든 경로(초기 로드·소켓·ack·채우기·이전 메시지)가 [merge] 하나로 — id 중복 제거 + seq 정렬(없으면 id)
 *  - 소켓 수신은 50ms 묶어 한 번에 반영
 *  - 놓친 메시지 채우기: 재연결·화면 복귀·빈 순번 감지 시 afterSeq(100건 × 최대 10회). 구버전 서버는 첫 페이지 재조회
 *  - 이전 메시지: beforeSeq 커서(구버전 서버는 page)
 *  - 읽음 1초 간격(앞·뒤 가장자리) / 타이핑 3초에 한 번·4초 무입력 시 해제
 *  - 방별 입력 드래프트(인메모리)
 */
class ChatRoomViewModel(
    val roomId: Int,
    initialTitle: String,
    internal val api: ChatApi,
    token: String?,
) : ViewModel() {

    /**
     * 전송 대기(낙관적 삽입). [localId] 는 서버에 clientMsgId 로 보내며 재시도해도 바뀌지 않는다.
     *  ack 의 메시지 또는 같은 clientMsgId 를 가진 소켓 에코가 도착하면 제거된다.
     */
    data class Pending(
        val localId: String = UUID.randomUUID().toString(),
        val type: String,
        val content: String,
        val imageUrl: String?,
        val replyToId: Int?,
        val failed: Boolean = false,
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
    var progress by mutableStateOf<ChatLessonProgress?>(null)              // 대표 진행(구백엔드 폴백 카드)
    var pendingProposal by mutableStateOf<ChatPendingProposal?>(null)      // 재입장으로 숨겨진 대기 중 약속 제안

    /** 채택(매칭)된 견적 방인지 — 회원도 레슨 약속 제안을 보낼 수 있다(iOS isQuoteMatched). */
    val isQuoteMatched: Boolean get() = quoteContext?.matched == true

    /**
     * 회원으로서 예약을 잡는 견적 맥락 — 약속 제안 작성의 금액·예약금 안내에 쓴다(iOS memberBookingContext).
     *  여러 레슨이 있으면 회원 쪽 ACCEPTED 사이클을 우선한다.
     */
    val memberBookingContext: ChatQuoteContext?
        get() = lessonCycles.firstOrNull { !it.isTeacher && it.progress.step == "ACCEPTED" }?.asContext
            ?: quoteContext?.takeIf { !it.isTeacher }

    // ── 상단 레슨 컨텍스트 판정 — iOS ChatRoomViewModel 과 같은 조건 ──

    /** 고객(강사 아님)이고 아직 미채택이면 '이 강사로 진행하기' 노출. */
    val canAcceptQuote: Boolean get() = quoteContext?.let { !it.isTeacher && !it.matched } ?: false

    /**
     * 채택된 견적의 일반회원이며 아직 일정이 확정되지 않은 경우(상단 '레슨 일정 잡기' 고정 버튼).
     *  단일/양방향/구백엔드 응답 모두 같은 조건으로 흡수한다.
     */
    val memberNeedsSchedule: Boolean
        get() {
            if (lessonCycles.any { !it.isTeacher && it.progress.step == "ACCEPTED" }) return true
            return lessonCycles.isEmpty() &&
                quoteContext?.isTeacher == false &&
                quoteContext?.matched == true &&
                progress?.step == "ACCEPTED"
        }

    /** 견적요청이 마감(14일 경과)됐는지 — 헤더 '견적 마감' 표시. */
    val isQuoteExpired: Boolean get() = quoteContext?.quoteStatus == "EXPIRED"

    /** 내가 이 방의 강사 측인지. */
    val isTeacherSide: Boolean get() = quoteContext?.isTeacher == true

    /** 방에 도착한 견적 수 — 서버 컨텍스트 우선(재입장 시 카드가 숨겨져도 유지), 없으면 메시지 카드 수. */
    val quoteCount: Int
        get() = quoteContext?.quoteCount?.takeIf { it > 0 } ?: messages.count { it.type == "QUOTE_CARD" }

    /** 컨텍스트 헤더 과목명("발레 레슨") — 서버 컨텍스트(categoryId) 우선, 메시지 견적 카드 폴백. */
    val contextCategory: String?
        get() {
            quoteContext?.categoryId?.let { cid -> GENRE_LABELS[cid]?.let { return "$it 레슨" } }
            val card = messages.firstOrNull { it.type == "QUOTE_CARD" } ?: return null
            val service = runCatching { org.json.JSONObject(card.content).optString("service") }.getOrNull()
            return service?.takeIf { it.isNotEmpty() }?.let { "$it 레슨" }
        }

    /** 재입장 방: 견적 카드 메시지는 없는데 견적 컨텍스트는 있음 → 헤더 탭 시 요약 시트로. */
    val quoteCardHiddenByRejoin: Boolean
        get() = quoteContext != null && messages.none { it.type == "QUOTE_CARD" }

    /** 컨텍스트 헤더 탭 → 스크롤할 첫 견적 카드 id. */
    val firstQuoteCardId: Int? get() = messages.firstOrNull { it.type == "QUOTE_CARD" }?.id

    /**
     * 고객: 이 강사 채택. 성공하면 상태·시스템 메시지를 다시 읽는다(iOS acceptQuote → loadInitial).
     *  이미 채택됐거나 강사 측이면 요청하지 않고 실패로 돌려준다.
     */
    suspend fun acceptQuote(): Boolean {
        val q = quoteContext ?: return false
        if (q.isTeacher || q.matched) return false
        return api.acceptQuote(q.quoteId, q.responseId).fold(
            onSuccess = {
                loadDetail()
                fetchLatest().onSuccess { res -> merge(res.messages); scheduleGapFillIfNeeded() }
                true
            },
            onFailure = { false },
        )
    }

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

    private val pageLimit = 50

    /**
     * 순번 조회 사용 여부 — 첫 로드 응답에 lastSeq 가 있고 0 보다 크면 true.
     *  false(구버전 서버, 또는 순번 백필 전)면 page 방식과 첫 페이지 재조회로 동작한다.
     */
    private var seqMode = false

    /** "여기까지는 빠짐없이 받았다" 는 순번. 0 이면 알 수 없음(순번 미사용). */
    private var contiguousSeq = 0

    /** 순번 방식에서 이전 메시지가 더 있는지. */
    private var hasMoreBefore = false

    // page 방식(구버전 서버) 페이지 상태.
    private var totalCount = 0
    private var loadedPages = 1

    /** 첫 로드 성공 여부 — 성공 전에는 채우기 대신 첫 로드를 다시 시도한다. */
    private var initialLoaded = false

    // 놓친 메시지 채우기 — 한 번에 하나만 실행하고, 실행 중 요청이 오면 끝난 뒤 한 번 더 실행한다.
    private var fillingGap = false
    private var fillAgain = false
    private var gapCheckJob: Job? = null

    // 소켓 수신 묶음(50ms).
    private val incomingBuffer = mutableListOf<ChatMessage>()
    private var incomingFlushJob: Job? = null

    // 읽음 전송(1초 간격, 앞·뒤 가장자리).
    private var markReadJob: Job? = null
    private var lastMarkReadAt: Long? = null

    // 타이핑 전송(3초에 한 번, 4초 무입력 시 false) / 수신 만료(6초).
    private var typingStopJob: Job? = null
    private var lastTypingSentAt: Long? = null
    private var otherTypingExpiryJob: Job? = null

    val hasMore: Boolean get() = if (seqMode) hasMoreBefore else messages.size < totalCount

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

    /**
     * 화면 복귀(ON_RESUME) — 다른 화면·백그라운드에 있던 동안 놓친 메시지를 채운다.
     *  최초 진입의 ON_RESUME 은 첫 로드와 겹치므로 첫 로드 완료 전에는 무시한다.
     */
    fun onResume() {
        if (!initialLoaded) return
        viewModelScope.launch { fillGap("resume") }
    }

    override fun onCleared() {
        ChatSocketManager.leaveRoom(roomId)
        if (lastTypingSentAt != null) ChatSocketManager.sendTyping(roomId, false)
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
            progress = d.progress
            pendingProposal = d.pendingProposal
        }
    }

    /**
     * 첫 로드 — cursor=seq 로 최신 [pageLimit] 건.
     *  구버전 서버는 cursor 를 무시하고 page=1 응답을 주므로 응답 형식([ChatMessagesResponse.isSeqMode])으로 구분한다.
     *  로드 중 소켓으로 들어온 메시지는 [merge] 가 id 로 합치므로 유실되지 않는다.
     */
    private suspend fun loadFirstPage() {
        isLoading = messages.isEmpty()
        val res = api.getMessagesBySeq(roomId, limit = pageLimit).getOrNull()
        if (res != null && res.isSeqMode && (res.lastSeq ?: 0) > 0) {
            seqMode = true
            hasMoreBefore = res.hasMore == true
            merge(res.messages)
            contiguousSeq = maxOf(contiguousSeq, res.lastSeq ?: 0)
            advanceContiguousSeq()
            initialLoaded = true
        } else {
            // 구버전 서버 또는 순번 백필 전(lastSeq=0) — page 방식.
            val page = if (res != null && !res.isSeqMode) Result.success(res)
            else api.getMessages(roomId, page = 1, limit = pageLimit)
            page.onSuccess { p ->
                seqMode = false
                totalCount = p.total
                loadedPages = 1
                merge(p.messages)
                initialLoaded = true
            }
        }
        isLoading = false
        scheduleGapFillIfNeeded()
        scheduleMarkRead()
    }

    /**
     * 위로 스크롤 — 이전 메시지.
     *  순번 방식은 beforeSeq 커서라 그 사이 새 메시지가 와도 밀리지 않는다(page 방식은 offset 이 밀려 중복이 생겼다).
     */
    fun loadMore() {
        if (isLoadingMore || !hasMore) return
        isLoadingMore = true
        viewModelScope.launch {
            val oldestSeq = messages.firstOrNull { it.seq != null }?.seq
            if (seqMode && oldestSeq != null) {
                api.getMessagesBySeq(roomId, beforeSeq = oldestSeq, limit = pageLimit).onSuccess { res ->
                    merge(res.messages)
                    hasMoreBefore = res.hasMore == true
                }
            } else {
                api.getMessages(roomId, page = loadedPages + 1, limit = pageLimit).onSuccess { res ->
                    loadedPages += 1
                    totalCount = res.total
                    merge(res.messages)
                }
            }
            isLoadingMore = false
        }
    }

    private suspend fun loadQuickReplies() {
        api.getQuickReplies(roomId).onSuccess { quickReplies = it }
    }

    /** 최신 메시지 한 묶음 — 순번 방식이면 cursor=seq, 아니면 page=1. */
    private suspend fun fetchLatest(): Result<ChatMessagesResponse> =
        if (seqMode) api.getMessagesBySeq(roomId, limit = pageLimit)
        else api.getMessages(roomId, page = 1, limit = pageLimit).onSuccess { totalCount = it.total }

    // ============================================================
    // 소켓 수신
    // ============================================================

    private fun collectSocket() {
        viewModelScope.launch {
            ChatEventBus.events.collect { e ->
                when (e) {
                    is ChatEvent.NewMessage -> if (e.roomId == roomId) enqueueIncoming(e.message)
                    // 수정·삭제도 같은 id 로 합치면 교체된다(목록에 없는 메시지는 추가하지 않는다).
                    is ChatEvent.MessageUpdated -> if (e.roomId == roomId) replaceMessage(e.message)
                    is ChatEvent.MessageDeleted -> if (e.roomId == roomId) replaceMessage(e.message)
                    is ChatEvent.MessagesRead ->
                        if (e.roomId == roomId && e.userId != currentUserId) {
                            opponentLastReadAt = com.muyeon.app.ui.quote.QuoteUi.parseDate(e.readAt)
                        }
                    is ChatEvent.Typing ->
                        if (e.roomId == roomId && e.userId != currentUserId) onOtherTyping(e.isTyping)
                    is ChatEvent.MessageReaction ->
                        // 서버가 집계를 안 실어준다(뷰어별 mine 이 달라서) → 최신 묶음 재조회.
                        if (e.roomId == roomId) viewModelScope.launch { refreshReactions() }
                    // 끊긴 동안 온 메시지는 replay=0 이라 통째로 유실된다 — 놓친 순번부터 채운다.
                    is ChatEvent.Reconnected -> viewModelScope.launch { fillGap("reconnect") }
                    else -> Unit
                }
            }
        }
    }

    /** 소켓 new-message — 50ms 묶어서 한 번에 반영한다(몰려도 화면 갱신은 묶음당 한 번). */
    private fun enqueueIncoming(msg: ChatMessage) {
        incomingBuffer.add(msg)
        if (incomingFlushJob != null) return
        incomingFlushJob = viewModelScope.launch {
            delay(INCOMING_BATCH_MS)
            val batch = incomingBuffer.toList()
            incomingBuffer.clear()
            incomingFlushJob = null
            receive(batch)
        }
    }

    /** 받은 메시지 반영 — 합치기 → 빈 순번 확인 → 읽음. */
    private fun receive(batch: List<ChatMessage>) {
        if (batch.isEmpty()) return
        // 채택·일정 확정 등 상태 변화 메시지(SYSTEM·카드류) → 상단 진행 카드 즉시 갱신(iOS bindSocket 과 같다).
        if (batch.any { it.type in CONTEXT_MESSAGE_TYPES }) reloadContext()
        merge(batch)
        scheduleGapFillIfNeeded()
        scheduleMarkRead()
    }

    private fun replaceMessage(msg: ChatMessage) {
        if (messages.any { it.id == msg.id }) merge(listOf(msg))
    }

    /**
     * 메시지 합치기 — 초기 로드·소켓·ack·채우기·이전 메시지 어디서 왔든 이 함수 하나로 반영한다.
     *  id 로 중복을 없애고(같은 id 는 새 값으로 교체) [ChatMessage.displayOrder](seq, 없으면 id) 순으로 정렬한다.
     *  같은 clientMsgId 의 전송 대기 말풍선은 여기서 제거된다(ack 보다 에코가 먼저 와도 한 번만 보인다).
     */
    private fun merge(incoming: List<ChatMessage>) {
        if (incoming.isEmpty()) return
        val index = HashMap<Int, Int>(messages.size * 2)
        messages.forEachIndexed { i, m -> index[m.id] = i }
        val fresh = ArrayList<ChatMessage>()
        for (m in incoming) {
            val i = index[m.id]
            if (i != null) {
                if (messages[i] != m) messages[i] = m
            } else if (fresh.none { it.id == m.id }) {
                fresh.add(m)
            }
        }
        if (fresh.isNotEmpty()) {
            val sortedFresh = sortForDisplay(fresh)
            val last = messages.lastOrNull()
            if (last == null || ChatMessage.displayOrder.compare(last, sortedFresh.first()) < 0) {
                // 일반적인 경우 — 전부 뒤에 붙는다.
                messages.addAll(sortedFresh)
            } else {
                val all = sortForDisplay(messages + sortedFresh)
                messages.clear()
                messages.addAll(all)
            }
        }
        val ids = incoming.mapNotNullTo(HashSet()) { it.clientMsgId }
        if (ids.isNotEmpty()) pending.removeAll { it.localId in ids }
        advanceContiguousSeq()
    }

    /**
     * 표시 순서 정렬. 서버 백필은 순번을 id 순으로 매기므로 두 기준은 일치하지만,
     *  백필 전후 데이터가 섞여 비교 규칙이 어긋나면 정렬이 예외를 던질 수 있어 id 순으로 대체한다.
     */
    private fun sortForDisplay(list: List<ChatMessage>): List<ChatMessage> =
        runCatching { list.sortedWith(ChatMessage.displayOrder) }.getOrElse { list.sortedBy { it.id } }

    /** "빠짐없이 받은 순번" 을 앞으로 민다 — 다음 순번이 목록에 있는 동안. */
    private fun advanceContiguousSeq() {
        if (contiguousSeq <= 0) return
        val seqs = messages.mapNotNullTo(HashSet()) { it.seq }
        while (seqs.contains(contiguousSeq + 1)) contiguousSeq++
    }

    private fun maxSeenSeq(): Int = messages.maxOfOrNull { it.seq ?: 0 } ?: 0

    /**
     * 받은 순번이 "빠짐없이" 보다 크면 사이가 비어 있다 — 순서만 늦게 도착한 것일 수 있으니
     *  500ms 기다린 뒤에도 비어 있으면 채운다.
     */
    private fun scheduleGapFillIfNeeded() {
        if (contiguousSeq <= 0 || gapCheckJob != null) return
        if (maxSeenSeq() <= contiguousSeq) return
        gapCheckJob = viewModelScope.launch {
            delay(GAP_CHECK_DELAY_MS)
            gapCheckJob = null
            advanceContiguousSeq()
            if (maxSeenSeq() > contiguousSeq) fillGap("gap")
        }
    }

    /**
     * 놓친 메시지 채우기 — 재연결·화면 복귀·빈 순번 감지 시.
     *  순번 방식: contiguousSeq 다음부터 afterSeq 100건씩 최대 10회(1,000건).
     *  구버전 서버: 첫 페이지를 다시 받아 합친다.
     *  채운 **뒤에** 읽음을 보낸다(먼저 보내면 아직 화면에 없는 메시지가 읽음 처리된다).
     */
    private suspend fun fillGap(reason: String) {
        if (!initialLoaded) {
            // 첫 로드가 실패했던 경우(오프라인 진입 등) — 채울 기준이 없으므로 첫 로드를 다시 한다.
            loadFirstPage()
            return
        }
        if (fillingGap) {
            fillAgain = true
            return
        }
        fillingGap = true
        try {
            do {
                fillAgain = false
                fillGapOnce(reason)
            } while (fillAgain)
        } finally {
            fillingGap = false
        }
        scheduleMarkRead()
    }

    private suspend fun fillGapOnce(reason: String) {
        if (seqMode && contiguousSeq > 0) {
            var after = contiguousSeq
            for (round in 0 until GAP_FILL_MAX_ROUNDS) {
                val res = api.getMessagesBySeq(roomId, afterSeq = after, limit = GAP_FILL_LIMIT).getOrElse {
                    Log.w(TAG, "fillGap($reason) 실패 room=$roomId after=$after: ${it.message}")
                    return
                }
                merge(res.messages)
                after = res.messages.lastOrNull()?.seq ?: after
                if (res.hasMore != true) {
                    // 끝까지 받았다 — 화면에 노출되지 않는 순번(나가기 이전 등)으로 생긴 빈칸은 건너뛴다.
                    contiguousSeq = maxOf(contiguousSeq, res.lastSeq ?: 0)
                    break
                }
                if (round == GAP_FILL_MAX_ROUNDS - 1) {
                    Log.w(TAG, "fillGap($reason) 최대 ${GAP_FILL_MAX_ROUNDS * GAP_FILL_LIMIT}건 도달 room=$roomId after=$after")
                }
            }
            Log.i(TAG, "fillGap($reason) room=$roomId upTo=$contiguousSeq")
        } else {
            fetchLatest().onSuccess { merge(it.messages) }
        }
    }

    private suspend fun refreshReactions() {
        fetchLatest().onSuccess { res ->
            // 반응 갱신은 이미 보이는 메시지만 교체한다.
            val shown = messages.mapTo(HashSet()) { it.id }
            merge(res.messages.filter { it.id in shown })
        }
    }

    /** 상대 입력 중 — 6초 동안 신호가 없으면 스스로 끈다(멈춤 신호가 유실돼도 남지 않게). */
    private fun onOtherTyping(typing: Boolean) {
        otherTypingExpiryJob?.cancel()
        isOtherTyping = typing
        if (typing) {
            otherTypingExpiryJob = viewModelScope.launch {
                delay(TYPING_EXPIRY_MS)
                isOtherTyping = false
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

    /**
     * 입력 중 신호 — 글자마다 보내지 않는다. 입력하는 동안 3초에 한 번 true,
     *  4초 동안 입력이 없거나 입력창이 비면 false.
     */
    private fun handleTypingChanged(text: String) {
        typingStopJob?.cancel()
        if (text.isEmpty()) {
            stopTyping()
            return
        }
        val now = SystemClock.elapsedRealtime()
        val last = lastTypingSentAt
        if (last == null || now - last >= TYPING_SEND_INTERVAL_MS) {
            ChatSocketManager.sendTyping(roomId, true)
            lastTypingSentAt = now
        }
        typingStopJob = viewModelScope.launch {
            delay(TYPING_IDLE_MS)
            stopTyping()
        }
    }

    private fun stopTyping() {
        typingStopJob?.cancel()
        if (lastTypingSentAt != null) {
            ChatSocketManager.sendTyping(roomId, false)
            lastTypingSentAt = null
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
        replyingTo = null
        clearInput()
        dispatch(p)
    }

    /** 실패한 말풍선 재전송 — 같은 clientMsgId 로 보낸다(이미 저장됐으면 서버가 처음 메시지를 돌려준다). */
    fun retry(p: Pending) {
        val i = pending.indexOfFirst { it.localId == p.localId }
        if (i < 0 || !pending[i].failed) return
        val again = pending[i].copy(failed = false)
        pending[i] = again
        dispatch(again)
    }

    /** 소켓 전송 후 ack 로 결과를 정한다. 에코 대기 타이머·내용 비교는 쓰지 않는다. */
    private fun dispatch(p: Pending) {
        viewModelScope.launch {
            val result = ChatSocketManager.sendMessage(
                roomId, p.type, p.content, p.imageUrl, p.replyToId, clientMsgId = p.localId,
            )
            when (result) {
                is SendResult.Sent ->
                    if (result.message != null) {
                        receiveOwn(result.message, p.localId)
                    } else {
                        // 저장은 됐으나 응답 본문이 없다(구버전 형식 등) — 에코가 목록에 반영한다.
                        pending.removeAll { it.localId == p.localId }
                    }
                is SendResult.Rejected -> {
                    Log.w(TAG, "send rejected code=${result.code} clientMsgId=${p.localId}")
                    markFailed(p.localId)
                    toast = rejectMessage(result.code)
                }
                SendResult.TimedOut, SendResult.NotConnected -> markFailed(p.localId)
            }
        }
    }

    /** 내 메시지 확정 — 대기 말풍선을 지우고 서버 메시지를 합친다(에코가 와도 id 로 한 번만 남는다). */
    private fun receiveOwn(msg: ChatMessage, localId: String) {
        pending.removeAll { it.localId == localId }
        merge(listOf(msg))
        scheduleGapFillIfNeeded()
    }

    private fun markFailed(localId: String) {
        val i = pending.indexOfFirst { it.localId == localId }
        if (i >= 0) pending[i] = pending[i].copy(failed = true)
    }

    private fun rejectMessage(code: String): String = when (code) {
        "RATE_LIMITED" -> "메시지를 너무 빠르게 보내고 있어요. 잠시 후 다시 보내 주세요."
        "TOO_LONG" -> "메시지가 너무 길어요(최대 5,000자)."
        "EMPTY" -> "빈 메시지는 보낼 수 없어요."
        "ACCOUNT_RESTRICTED" -> "이용이 제한된 계정이에요."
        "FORBIDDEN", "INVALID_ROOM" -> "이 채팅방에는 메시지를 보낼 수 없어요."
        else -> "메시지를 보내지 못했어요."
    }

    private fun clearInput() {
        input = ""
        ChatDrafts.clear(roomId)
        stopTyping()
    }

    fun deleteMessage(m: ChatMessage) = ChatSocketManager.deleteMessage(roomId, m.id)

    /** 약속 제안 수락/거절/취소 후 — 진행 카드·일정 배너가 바뀌므로 상세를 다시 읽는다. */
    fun reloadContext() { viewModelScope.launch { loadDetail() } }

    /**
     * 설문 등 방 밖 화면에서 돌아왔을 때 — 맥락 + 최신 메시지를 다시 읽어 **있는 건 교체,
     *  없는 건 끼워 넣는다**(카드 응답 상태 반영). 목록을 비우지 않아 스크롤이 튀지 않는다.
     */
    fun reloadMessagesAndContext() {
        viewModelScope.launch {
            loadDetail()
            fetchLatest().onSuccess { res ->
                merge(res.messages)
                scheduleGapFillIfNeeded()
                scheduleMarkRead()
            }
        }
    }

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
            dispatch(p)
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
            dispatch(p)
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

    /**
     * 읽음 전송 — 1초 간격, 앞·뒤 가장자리.
     *  첫 호출은 바로 보내고, 이후 1초 안의 호출은 한 번으로 합쳐 구간 끝에 반드시 보낸다.
     *  (디바운스는 메시지가 계속 오면 끝까지 보내지 않다가 멈추는 순간 몰아서 보냈다.)
     */
    private fun scheduleMarkRead() {
        if (markReadJob != null) return
        val last = lastMarkReadAt
        val wait = if (last == null) 0L else (MARK_READ_INTERVAL_MS - (SystemClock.elapsedRealtime() - last)).coerceAtLeast(0L)
        markReadJob = viewModelScope.launch {
            if (wait > 0) delay(wait)
            markReadJob = null
            lastMarkReadAt = SystemClock.elapsedRealtime()
            ChatSocketManager.markRead(roomId)
        }
    }

    private companion object {
        const val TAG = "ChatRoomVM"
        const val INCOMING_BATCH_MS = 50L
        const val GAP_CHECK_DELAY_MS = 500L
        const val GAP_FILL_LIMIT = 100
        const val GAP_FILL_MAX_ROUNDS = 10
        const val MARK_READ_INTERVAL_MS = 1_000L
        const val TYPING_SEND_INTERVAL_MS = 3_000L
        const val TYPING_IDLE_MS = 4_000L
        const val TYPING_EXPIRY_MS = 6_000L

        /** 도착하면 방 상단 맥락을 다시 읽어야 하는 메시지 종류. */
        val CONTEXT_MESSAGE_TYPES = setOf("SYSTEM", "QUOTE_CARD", "LESSON_CARD")

        /** 과목 코드 → 표시명(iOS ChatRoomViewModel.genreLabel 과 같은 표). */
        val GENRE_LABELS = mapOf(
            "ballet" to "발레", "barre" to "바레", "korean" to "한국무용", "modern" to "현대무용",
            "practical" to "실용무용", "balletfit" to "발레핏", "musical" to "뮤지컬",
        )
    }
}

/** 방별 입력 드래프트(인메모리) — iOS ChatDraftManager. */
object ChatDrafts {
    private val drafts = java.util.concurrent.ConcurrentHashMap<Int, String>()
    fun save(roomId: Int, text: String) { if (text.isEmpty()) drafts.remove(roomId) else drafts[roomId] = text }
    fun get(roomId: Int): String = drafts[roomId] ?: ""
    fun clear(roomId: Int) { drafts.remove(roomId) }
}
