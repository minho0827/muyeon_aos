package com.muyeon.app.ui.chat

import com.muyeon.app.ui.resume.RecruitChatLimitException
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
    var hasPendingProposal by mutableStateOf(false)                        // 방에 응답 대기 중인 약속 제안(서버 hasPendingProposal)
    var locked by mutableStateOf(false)                                    // 대화 종료(요청 취소·다른 강사 채택·만료) — 입력창 대신 안내 띠
    var lockReason by mutableStateOf<String?>(null)                        // CANCELED | MATCHED_OTHER | EXPIRED
    var reRequest by mutableStateOf<ChatReRequest?>(null)                  // 잠긴 방 '이 강사님께 다시 레슨 요청하기'(요청자에게만)
    var locksAt by mutableStateOf<String?>(null)                           // 이대로면 대화가 끝나는 시각(ISO UTC) — 상단 카운트다운
    // ★ 2026-11-14 상단 고정 바·첫 안내·전화 버튼(서버 pair·myRole·contact)
    var pair by mutableStateOf<String?>(null)
    var myRole by mutableStateOf<String?>(null)
    var contact by mutableStateOf(ChatContact.HIDDEN)

    /** 상단 'D-N 후 대화 종료' 일수 — 열린 방이고 locksAt 이 있을 때만(잠긴 방은 안내 띠가 대신한다). */
    fun lockDaysLeft(nowMillis: Long = System.currentTimeMillis()): Int? =
        if (locked) null else ChatLockCountdown.daysLeft(locksAt, nowMillis)

    /** 잠긴 방 안내 띠 문구. */
    val lockNotice: String get() = ChatRoomDetail.lockNotice(lockReason)

    /**
     * 응답 대기 중인 약속 제안이 있어 회원 '일정 잡기'를 숨겨야 하는지(서버가 방당 대기 제안 1건만 받는다).
     *  구 서버는 hasPendingProposal 이 없으므로 재입장 고정 카드(pendingProposal)로 폴백.
     */
    val proposalWaiting: Boolean get() = hasPendingProposal || pendingProposal != null

    /** 채택 확인창의 대상 사이클 — null 이면 대표 견적(quoteContext). 사이클이 여러 개인 방에서 엉뚱한 견적 채택 방지. */
    var acceptTarget by mutableStateOf<ChatLessonCycle?>(null)

    /** 채택(매칭)된 견적 방인지 — 회원도 레슨 약속 제안을 보낼 수 있다(iOS isQuoteMatched). */
    val isQuoteMatched: Boolean get() = quoteContext?.matched == true

    /** ★ 11-10 학원 채용 방(afterAccept=HIRE) — 채택 = 채용 확정. 일정 잡기·약속 제안이 없다(서버도 400). */
    val isHire: Boolean get() = quoteContext?.isHire == true

    /** ★ 11-10 채택 확인창이 학원 채용인지 — 사이클에서 누른 채택은 그 사이클 기준, 아니면 방 기준. */
    val acceptIsHire: Boolean
        get() = acceptTarget?.let { t -> t.isHire || (t.afterAccept == null && isHire) } ?: isHire

    /**
     * 회원으로서 예약을 잡는 견적 맥락 — 약속 제안 작성의 금액·예약금 안내에 쓴다(iOS memberBookingContext).
     *  여러 레슨이 있으면 회원 쪽 ACCEPTED 사이클을 우선한다.
     */
    val memberBookingContext: ChatQuoteContext?
        get() = lessonCycles.firstOrNull { !it.isTeacher && !it.isHire && it.progress.step == "ACCEPTED" }?.asContext
            ?: quoteContext?.takeIf { !it.isTeacher }

    // ── 상단 레슨 컨텍스트 판정 — iOS ChatRoomViewModel 과 같은 조건 ──

    /** 고객(강사 아님)이고 아직 미채택이면 '이 강사로 진행하기' 노출. */
    val canAcceptQuote: Boolean get() = !acceptBlocked && (quoteContext?.let { !it.isTeacher && !it.matched } ?: false)

    /** 채택 불가(대표 견적) — 대화 종료 방이거나 요청 마감(서버가 EXPIRED 채택을 거절한다). */
    val acceptBlocked: Boolean get() = acceptBlockedFor(null)

    /**
     * 채택 불가 판정. 잠금은 방 전체, 마감(quoteStatus)은 대표 견적 기준이라
     *  사이클([target])에서 누른 채택은 잠금만 본다(같은 방의 다른 새 요청은 열려 있을 수 있다).
     */
    fun acceptBlockedFor(target: ChatLessonCycle?): Boolean = locked || (target == null && (isQuoteExpired || isQuoteCanceled))

    /**
     * 채택된 견적의 일반회원이며 아직 일정이 확정되지 않은 경우(상단 '레슨 일정 잡기' 고정 버튼).
     *  단일/양방향/구백엔드 응답 모두 같은 조건으로 흡수한다.
     */
    val memberNeedsSchedule: Boolean
        get() {
            if (lessonCycles.any { !it.isTeacher && !it.isHire && it.progress.step == "ACCEPTED" }) return true
            return !isHire && lessonCycles.isEmpty() &&
                quoteContext?.isTeacher == false &&
                quoteContext?.matched == true &&
                progress?.step == "ACCEPTED"
        }

    /** 견적요청이 마감(서버 EXPIRED, 요청 후 30일)됐는지 — 헤더 '견적 마감' 표시. */
    val isQuoteExpired: Boolean get() = quoteContext?.quoteStatus == "EXPIRED"
    /** 대표 견적의 요청이 취소됨 — 다시 요청해 방이 열려도 이 견적은 채택할 수 없다(11-07 실기기). */
    val isQuoteCanceled: Boolean get() = quoteContext?.quoteStatus.let { it == "CANCELED" || it == "CLOSED" }

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
     *  ★ 11-10 성공이면 (채택한 quoteId, 서버 응답) — 학원 채용의 같이 올린 공고 마감 질문에 쓴다. 실패는 null.
     */
    suspend fun acceptQuote(): Pair<Int, com.muyeon.app.ui.quote.AcceptQuoteResult>? {
        // 사이클에서 누른 채택이면 그 사이클의 견적(quoteId·responseId), 아니면 대표 견적.
        val target = acceptTarget
        acceptTarget = null
        if (acceptBlockedFor(target)) return null
        val ids: Pair<Int, Int> = if (target != null) {
            if (target.isTeacher || target.matched || target.isProposal || target.quoteId <= 0 || target.responseId <= 0) return null
            target.quoteId to target.responseId
        } else {
            val q = quoteContext ?: return null
            if (q.isTeacher || q.matched) return null
            q.quoteId to q.responseId
        }
        return api.acceptQuote(ids.first, ids.second).fold(
            onSuccess = { r ->
                loadDetail()
                fetchLatest().onSuccess { res -> merge(res.messages); scheduleGapFillIfNeeded() }
                ids.first to r
            },
            onFailure = { null },
        )
    }

    /**
     * 상단 부제 — 상대 최근 읽음 시각으로 접속·활동 상태를 표시한다(iOS presenceText).
     *  5분 미만 "접속 중", 1시간 미만 "N분 전 활동", 24시간 미만 "N시간 전 활동", 그 외 표시 없음.
     *  [now] 는 화면이 1분마다 갱신해 넘긴다(시간 경과에 따라 문구가 바뀌어야 하므로).
     */
    fun presenceText(now: Long): String? {
        val lr = opponentLastReadAt ?: return null
        val diffSec = ((now - lr) / 1000).coerceAtLeast(0)
        return when {
            diffSec < 300 -> PRESENCE_ONLINE
            diffSec < 3600 -> "${diffSec / 60}분 전 활동"
            diffSec < 86400 -> "${diffSec / 3600}시간 전 활동"
            else -> null
        }
    }

    var quickReplies by mutableStateOf<List<ChatQuickReply>>(emptyList())

    // ── AI 답장 추천(10-23, 프로 · 강사/원장) — 칩을 누르면 입력창에 채우고, 보내면 그 답을 서버가 기억한다.
    var aiReplies by mutableStateOf<List<String>>(emptyList())
    var aiReplyIntent: String? = null
    /** 지금 입력창 글이 AI 칩에서 왔는지(고쳐서 보내도 기억한다). */
    private var aiReplyPicked = false
    /** 화면이 넣어 준다 — 칩으로 채운 글을 보냈을 때(intent, 보낸 글). */
    var onAiReplySent: ((String?, String) -> Unit)? = null

    fun pickAiReply(text: String) {
        onInputChange(text)
        aiReplyPicked = true
    }
    var replyingTo by mutableStateOf<ChatMessage?>(null)
    var editingMessage by mutableStateOf<ChatMessage?>(null)
    var isUploadingMedia by mutableStateOf(false)
    var toast by mutableStateOf<String?>(null)
    /** 전송 거절 RECRUIT_CHAT_LIMIT → 채팅 한도 팝업([멤버십 보기]). */
    var chatLimit by mutableStateOf<RecruitChatLimitException?>(null)

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

    /**
     * 첫 로드 성공 여부 — 성공 전에는 채우기 대신 첫 로드를 다시 시도한다.
     *  화면은 푸시 딥링크 대상 카드 찾기를 첫 로드 이후에 시작하는 기준으로 쓴다.
     */
    var initialLoaded by mutableStateOf(false)
        private set

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

    // 화면 재구성·회전으로 LaunchedEffect 가 다시 돌아도 입장은 한 번만 — 두 번 세면 onCleared 의 한 번 퇴장으로 안 풀린다.
    private var started = false

    fun start() {
        if (started) return
        started = true
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
        if (started) ChatSocketManager.leaveRoom(roomId)
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
            hasPendingProposal = d.hasPendingProposal == true
            locked = d.locked
            lockReason = d.lockReason
            reRequest = d.reRequest
            locksAt = d.locksAt
            pair = d.pair
            myRole = d.myRole
            contact = d.contact
        }
    }

    // ── ★ 2026-11-14 상단 고정 바(ChatDealBar) 입력 — 판정은 ChatDealBarResolver, 여기는 값만 모은다 ──

    /** 바가 따르는 대표 사이클 — 내 할 일(일정 확정·채택)이 있는 사이클 우선, 없으면 첫 사이클. */
    val dealCycle: ChatLessonCycle?
        get() = lessonCycles.firstOrNull { it.needsMyAction || it.awaitsMyAccept } ?: lessonCycles.firstOrNull()

    /**
     * 내가 답할 약속 제안이 있는가 — 최신 LESSON_PROPOSAL 카드가 대기(PROPOSED·결제 대기)이고 상대가 보냈다.
     *  proposerId 가 없는 옛 카드는 강사가 보낸 것(서버와 같은 해석).
     */
    val proposalAwaitingMe: Boolean
        get() {
            val m = messages.lastOrNull { it.type == "LESSON_PROPOSAL" } ?: return false
            val o = runCatching { org.json.JSONObject(m.content) }.getOrNull() ?: return false
            val st = o.optString("status")
            if (st != "PROPOSED" && st != "AWAITING_PAYMENT") return false
            val proposer = if (o.has("proposerId")) o.optInt("proposerId") else m.senderId
            return proposer != currentUserId
        }

    /** 최신 대기 중 약속 제안 카드 id(바 [약속 확인하기] → 그 카드로 스크롤). */
    val pendingProposalMessageId: Int?
        get() = messages.lastOrNull { m ->
            m.type == "LESSON_PROPOSAL" && runCatching { org.json.JSONObject(m.content).optString("status") }
                .getOrNull().let { it == "PROPOSED" || it == "AWAITING_PAYMENT" }
        }?.id

    /** 바에 쓸 레슨 일정 시각 문자열 — "11/26(목) 오후 3:00". */
    private fun shortWhen(iso: String?): String? {
        val t = com.muyeon.app.ui.quote.QuoteUi.parseDate(iso) ?: return null
        return java.text.SimpleDateFormat("M/d(E) a h:mm", java.util.Locale.KOREAN).format(java.util.Date(t))
    }

    /** 대표 레슨의 시작 시각(ms) — 레슨 완료 확인창의 '시작 전' 경고. */
    val dealStartMillis: Long?
        get() = com.muyeon.app.ui.quote.QuoteUi.parseDate(
            dealCycle?.lessonSchedule?.startAt ?: dealCycle?.progress?.scheduleStartAt ?: progress?.scheduleStartAt ?: lessonSchedule?.startAt,
        )

    /** 대표 레슨 id(일정 보기·레슨 완료). */
    val dealLessonId: Int?
        get() = dealCycle?.let { it.progress.lessonId ?: it.lessonSchedule?.lessonId } ?: progress?.lessonId ?: lessonSchedule?.lessonId

    /** 확인창 문구용 레슨 한 줄 — "발레 레슨 11/26(목) 오후 3:00". */
    val dealLessonLine: String
        get() = listOfNotNull(dealCycle?.title ?: contextCategory ?: "레슨", shortWhen(dealCycle?.lessonSchedule?.startAt ?: progress?.scheduleStartAt))
            .joinToString(" ")

    fun dealBarInput(): com.muyeon.app.ui.chat.dealbar.DealBarInput {
        val c = dealCycle
        val ctx = c?.asContext ?: quoteContext
        val step = c?.progress?.step ?: progress?.step ?: if (quoteContext != null) "RESPONDED" else null
        val startIso = c?.lessonSchedule?.startAt ?: c?.progress?.scheduleStartAt ?: progress?.scheduleStartAt ?: lessonSchedule?.startAt
        return com.muyeon.app.ui.chat.dealbar.DealBarInput(
            pair = pair,
            myRole = myRole ?: c?.let { if (it.isTeacher) "pro" else "customer" } ?: quoteContext?.let { if (it.isTeacher) "pro" else "customer" },
            locked = locked,
            step = step,
            title = c?.title ?: contextCategory,
            priceText = ctx?.priceText,
            scheduleText = shortWhen(startIso),
            lessonId = dealLessonId,
            canAccept = if (c != null) !c.isTeacher && !c.matched && !c.isProposal && !acceptBlockedFor(c) else canAcceptQuote,
            proposalWaiting = proposalWaiting,
            proposalAwaitingMe = proposalAwaitingMe,
            contactRevealed = contact.revealed,
            cyclesCount = lessonCycles.size,
            hasQuote = quoteContext != null,
            // ★ 11-14 시작 시각을 모르면 막지 않는다(서버가 시작 전 완료를 한 번 더 막는다).
            lessonStarted = (dealStartMillis ?: 0L) <= System.currentTimeMillis(),
        )
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
                    // 방 상태 변경(잠금·번호 공개) — 상세를 다시 읽고 최신 메시지를 다시 받아 교체한다(마스킹 해제 반영).
                    is ChatEvent.RoomRefresh -> if (e.roomId == roomId) viewModelScope.launch { reloadRoom() }
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
        // 수정 중인 문구는 드래프트로 남기지 않는다(취소하면 사라져야 한다).
        if (editingMessage == null) ChatDrafts.save(roomId, text)
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
            editingMessage = null
            clearInput()
            viewModelScope.launch {
                val r = ChatSocketManager.editMessage(roomId, target.id, text)
                if (r is SendResult.Rejected) onRejected(r.code)
            }
            return
        }

        val p = Pending(type = "TEXT", content = text, imageUrl = null, replyToId = replyingTo?.id)
        pending.add(p)
        replyingTo = null
        if (aiReplyPicked) { onAiReplySent?.invoke(aiReplyIntent, text); aiReplyPicked = false }
        aiReplies = emptyList()
        clearInput()
        dispatch(p)
    }

    /** 빠른 답변 칩 탭 → 그 문구를 바로 전송(입력창·드래프트는 건드리지 않는다 — iOS sendQuick). */
    fun sendQuick(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        val p = Pending(type = "TEXT", content = t, imageUrl = null, replyToId = null)
        pending.add(p)
        dispatch(p)
    }

    /**
     * 빠른 답변 칩 노출 조건 — 내가 이 방에서 아직 한마디도 하지 않았을 때만(iOS hasMyMessage).
     *  한 번이라도 보내면 숨긴다. 재입장한 새 대화에서는 이전 메시지가 안 보이므로 다시 나타난다.
     */
    val hasMyMessage: Boolean
        get() = pending.isNotEmpty() || messages.any { it.senderId == currentUserId && it.type != "SYSTEM" }

    /**
     * 상대 메시지 묶음의 첫 말풍선에만 아바타를 보인다(iOS showAvatar — 카톡 방식).
     *  직전 메시지가 내 것이거나 시스템 안내면 새 묶음으로 본다. 견적 카드에는 아바타가 없다.
     */
    fun showAvatar(index: Int): Boolean {
        val m = messages.getOrNull(index) ?: return false
        if (m.senderId == currentUserId || m.type == "SYSTEM" || m.type == "QUOTE_CARD") return false
        if (index == 0) return true
        val prev = messages[index - 1]
        return prev.senderId == currentUserId || prev.type == "SYSTEM"
    }

    // ── 길게 누르기 메뉴의 답장·수정 시작/취소(iOS startReply·startEdit·cancelCompose) ──

    fun startReply(m: ChatMessage) {
        if (editingMessage != null) cancelCompose()
        replyingTo = m
    }

    /** 수정 시작 — 입력값을 원문으로 채운다(드래프트에는 남기지 않는다). */
    fun startEdit(m: ChatMessage) {
        replyingTo = null
        editingMessage = m
        input = m.content
    }

    /** 답장·수정 취소. 수정 중이었다면 원문으로 채운 입력값도 비운다. */
    fun cancelCompose() {
        val wasEditing = editingMessage != null
        replyingTo = null
        editingMessage = null
        if (wasEditing) clearInput()
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
                    if (result.code == RecruitChatLimitException.CODE) {
                        // 이번 달 새 강사·무용수 채팅 한도 — 말풍선은 실패로 두고 멤버십 안내 팝업(이력서 화면과 같은 것)
                        chatLimit = RecruitChatLimitException.fromAck(result.message, result.data, result.raw)
                    } else {
                        onRejected(result.code)
                    }
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

    /** 거절 안내 — 잠긴 방이면 상세를 다시 읽어 입력창을 안내 띠로 바꾼다. */
    private fun onRejected(code: String) {
        toast = rejectMessage(code)
        if (code == "ROOM_LOCKED") viewModelScope.launch { loadDetail() }
    }

    /** room-refresh — 상세(잠금·컨텍스트)와 최신 메시지 묶음을 다시 받는다. 서버 기준 내용으로 교체된다. */
    private suspend fun reloadRoom() {
        loadDetail()
        fetchLatest().onSuccess { res -> merge(res.messages); scheduleGapFillIfNeeded() }
    }

    private fun rejectMessage(code: String): String = when (code) {
        "ROOM_LOCKED" -> "레슨 요청이 마감되어 대화할 수 없어요."
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
     *  앨범·카메라 모두 긴 변 1200px·JPEG 품질 60% 로 줄여 올린다(iOS compressJPEG 와 같은 기준).
     *  디코딩할 수 없는 형식이면 원본을 그대로 올린다.
     */
    fun sendImages(context: android.content.Context, uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        isUploadingMedia = true
        viewModelScope.launch {
            val urls = uris.mapNotNull { uri ->
                val bytes = ChatImageCompressor.compress(context, uri) ?: readBytes(context, uri)
                bytes?.let { api.uploadImage(it).getOrNull() }
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

    /**
     * 반응 누르기(PACERA 규칙) — 한 사람 하나: 같은 반응 = 취소, 다른 반응 = 교체(옛것 해제 + 새것).
     *  낙관적으로 바꾼 뒤 서버 토글을 차례로 보낸다. 소켓 'message-reaction' 이 오면 반응 묶음을 다시 읽어 맞춘다.
     */
    fun toggleReaction(m: ChatMessage, emoji: String) {
        // 잠긴 방은 반응을 받지 않는다(시트에서 숨기고 칩도 누를 수 없다 — 여기서도 한 번 더 막는다).
        if (m.id <= 0 || locked) return
        val i = messages.indexOfFirst { it.id == m.id }
        val current = if (i >= 0) messages[i].reactions.orEmpty() else m.reactions.orEmpty()
        val (next, calls) = ChatReactionRules.toggle(current, emoji)
        if (i >= 0) messages[i] = messages[i].copy(reactions = next)
        viewModelScope.launch {
            var failure: Throwable? = null
            for (e in calls) {
                failure = api.toggleReaction(roomId, m.id, e).exceptionOrNull()
                if (failure != null) break
            }
            when {
                failure == null -> Unit
                // 잠긴 방(서버 ROOM_LOCKED) — 안내 후 방(상세·메시지)을 다시 읽어 안내 띠·반응을 서버 기준으로 맞춘다.
                ChatApi.isRoomLocked(failure) -> {
                    toast = ROOM_LOCKED_REACTION_TOAST
                    reloadRoom()
                }
                else -> refreshReactions()
            }
        }
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

    companion object {
        /** 접속 중 문구 — 화면이 초록 점 표시 여부를 이 값으로 판정한다. */
        const val PRESENCE_ONLINE = "접속 중"
        /** 잠긴 방 반응 거절(REST ROOM_LOCKED) 안내. */
        const val ROOM_LOCKED_REACTION_TOAST = "대화가 종료된 채팅방이에요."
        private const val TAG = "ChatRoomVM"
        private const val INCOMING_BATCH_MS = 50L
        private const val GAP_CHECK_DELAY_MS = 500L
        private const val GAP_FILL_LIMIT = 100
        private const val GAP_FILL_MAX_ROUNDS = 10
        private const val MARK_READ_INTERVAL_MS = 1_000L
        private const val TYPING_SEND_INTERVAL_MS = 3_000L
        private const val TYPING_IDLE_MS = 4_000L
        private const val TYPING_EXPIRY_MS = 6_000L

        /** 도착하면 방 상단 맥락을 다시 읽어야 하는 메시지 종류. */
        private val CONTEXT_MESSAGE_TYPES = setOf("SYSTEM", "QUOTE_CARD", "LESSON_CARD")

        /** 과목 코드 → 표시명(iOS ChatRoomViewModel.genreLabel 과 같은 표). */
        private val GENRE_LABELS = mapOf(
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
