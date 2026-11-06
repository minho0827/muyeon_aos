package com.muyeon.app.ui.chat

import android.app.Activity
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import com.muyeon.app.webview.ActiveRole
import com.muyeon.app.webview.NativeWebRoute
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.survey.SurveyPickerSheet
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.border

/**
 * 채팅방 — iOS `ChatRoomView.swift` + `ChatRoomView+Rendering.swift` 이식(코어).
 *  말풍선(좌/우) · 낙관 전송 · 읽음표시 · 입력중 · 답장/수정 · 위로 스크롤 페이징.
 *
 * ⚠️ iOS 수치: 버블 라운드 18 / 내 버블 primary·흰글씨 / 상대 버블 F2F2F7 / 본문 15 /
 *   시간 11 secondary / 아바타 34(상대 묶음 첫 말풍선만) / 입력바 상단 구분선 + 전송 버튼 원형 34.
 */
@Composable
fun ChatRoomScreen(
    vm: ChatRoomViewModel,
    onBack: () -> Unit,
    initialSurveyDispatchId: Int? = null,   // 설문 응답 푸시 진입 — 이 설문 카드로 스크롤·강조
    initialProposalId: Int? = null,         // 약속 제안 푸시·웹 진입 — 이 제안 카드로 스크롤·강조
) {
    // 채팅방에선 AI 버튼을 숨긴다 — 보내기 버튼과 겹친다(10-23 요청, iOS ChatRoomView 와 같음).
    androidx.compose.runtime.DisposableEffect(Unit) {
        com.muyeon.app.ui.assistant.AssistantOverlay.chatRooms++
        onDispose { com.muyeon.app.ui.assistant.AssistantOverlay.chatRooms = maxOf(0, com.muyeon.app.ui.assistant.AssistantOverlay.chatRooms - 1) }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showAttach by remember { mutableStateOf(false) }
    var showProposal by remember { mutableStateOf(false) }
    var showSurveyPicker by remember { mutableStateOf(false) }
    // 강사: 견적 상담 첫 채팅에 + 버튼 위 설문 안내(앱 설치 후 1회, iOS 와 같은 규칙).
    //  견적 정보(강사 측 여부)는 방을 연 뒤 늦게 도착하므로 그 값이 바뀔 때 본다.
    var showSurveyHint by remember { mutableStateOf(false) }
    LaunchedEffect(vm.quoteContext?.isTeacher) {
        if (vm.quoteContext?.isTeacher == true && !showSurveyHint && ChatSurveyHint.consume(context)) showSurveyHint = true
    }
    var showReport by remember { mutableStateOf(false) }
    var reportMessage by remember { mutableStateOf<ChatMessage?>(null) }   // 상대 메시지 길게 누르기 → 메시지 신고
    var confirmBlock by remember { mutableStateOf(false) }
    var reactionTarget by remember { mutableStateOf<ChatMessage?>(null) }
    // ── 상단 레슨 컨텍스트(iOS lessonContextArea) 시트·확인창 상태 ──
    var showCyclesSheet by remember { mutableStateOf(false) }
    var showLegacyTimeline by remember { mutableStateOf(false) }               // 레거시 대표 진행 타임라인
    var timelineCycle by remember { mutableStateOf<ChatLessonCycle?>(null) }   // 사이클별 타임라인
    var showQuoteSummary by remember { mutableStateOf(false) }
    var showAcceptConfirm by remember { mutableStateOf(false) }
    var showReviewSwitch by remember { mutableStateOf(false) }
    var teacherReviewInfo by remember { mutableStateOf(false) }

    LaunchedEffect(vm.roomId) { vm.start() }

    // ── 카드 찾아가기(iOS attemptSurveyJump·attemptProposalJump·highlightSurveyCard) ──
    //  대상이 아직 로드되지 않았으면 이전 메시지를 더 불러오며 찾고, 찾으면 스크롤 후 잠깐 흔들어 강조한다.
    var jumpTarget by remember {
        mutableStateOf(
            initialSurveyDispatchId?.let { CardJumpTarget.Survey(it) }
                ?: initialProposalId?.let { CardJumpTarget.Proposal(it) },
        )
    }
    var jumpLoadCount by remember { mutableIntStateOf(0) }   // 실패 반복 시 무한 재조회 방지
    var highlightId by remember { mutableStateOf<Int?>(null) }
    // ★ 2026-11-04 PACERA: 남의 사이트 링크 → "공유된 웹사이트" 확인 시트.
    var externalLink by remember { mutableStateOf<String?>(null) }

    // 화면 복귀(다른 화면·백그라운드에서 돌아옴) — 그 사이 놓친 메시지를 순번 기준으로 채운다.
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) vm.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 방에서 연 네이티브 화면에서 돌아오면 바뀐 것만 다시 읽는다(결과 키 — 타이머 없음).
    //  · 레슨 상세(LESSONS·LESSON_SCHEDULE): 일정 카드·방 상단 맥락이 바뀔 수 있다 → reloadContext
    //  · 설문(CHAT_ROOM): 설문 카드 응답 상태가 바뀐다 → 메시지 첫 페이지 + 맥락 재조회
    val lessonLauncher = com.muyeon.app.result.rememberResultLauncher { keys ->
        if (com.muyeon.app.result.ResultKeys.LESSONS in keys ||
            com.muyeon.app.result.ResultKeys.LESSON_SCHEDULE in keys
        ) vm.reloadContext()
    }
    val surveyLauncher = com.muyeon.app.result.rememberResultLauncher { keys ->
        if (com.muyeon.app.result.ResultKeys.CHAT_ROOM in keys) vm.reloadMessagesAndContext()
    }
    // 받은 견적 상세(채택·재요청 등)에서 돌아오면 방 맥락(진행 카드·배너)을 다시 읽는다.
    val quoteLauncher = com.muyeon.app.result.rememberResultLauncher { vm.reloadContext() }
    // 전체 알림 설정에서 돌아오면 방 상세(음소거 상태 등)를 다시 읽는다.
    val notiSettingsLauncher = com.muyeon.app.result.rememberResultLauncher { vm.reloadContext() }

    /**
     * 상대 공개 프로필 — iOS PublicProfileView(userId: recipientId, src: "chat", hideCta: true).
     *  강사가 아니면 서버가 404 를 돌려주고 프로필 화면이 "불러오지 못했어요" 를 표시한다(iOS 와 같다).
     */
    fun openOpponentProfile() {
        val uid = vm.opponentId
        if (uid <= 0) return
        com.muyeon.app.ui.resume.ResumeActivity.startProfile(context, uid, "chat")
    }

    // ── 상단 레슨 컨텍스트 동작 — iOS ChatRoomView+Rendering 의 handleProgressPrimary 등과 같은 분기 ──

    /** 레슨 일정 상세(확정·완료·취소는 상세 화면이 담당). */
    fun openLesson(lessonId: Int) {
        lessonLauncher.launch(com.muyeon.app.ui.lesson.LessonActivity.detailIntent(context, lessonId))
    }

    /**
     * 강사 [일정 확정하기/일정 정하기] — iOS 는 일정 확정 폼(LessonEditView)을 띄운다.
     *  AOS 는 같은 확정 기능(PENDING → SCHEDULED)이 레슨 일정 상세에 있으므로 상세로 연다.
     */
    fun openProgressSchedule(prog: ChatLessonProgress) {
        prog.lessonId?.let { openLesson(it) }
    }

    /** 고객 [후기 쓰기] — 채팅을 닫고 웹 강사 상세의 후기 폼(?review=1)으로 이동한다. */
    fun openReviewPage() {
        val teacherId = vm.opponentId
        if (teacherId == 0) return
        (context as? Activity)?.let { NativeWebRoute.openWebAndFinish(it, "/teachers/$teacherId?review=1") }
    }

    /** 후기 진입 — 활성유형이 일반이 아니면 전환 안내를 먼저 띄운다. */
    fun startReviewFlow() {
        if (ActiveRole.current(context) == "GENERAL") openReviewPage() else showReviewSwitch = true
    }

    fun handleProgressPrimary(prog: ChatLessonProgress, isTeacher: Boolean) {
        when (prog.step) {
            "DONE" -> if (isTeacher) teacherReviewInfo = true else startReviewFlow()
            "SCHEDULED" -> prog.lessonId?.let { openLesson(it) }
            "ACCEPTED" -> if (isTeacher) openProgressSchedule(prog) else showProposal = true
            else -> if (!isTeacher) showAcceptConfirm = true   // 채팅방을 나가지 않고 바로 채택
        }
    }

    /** 메시지 id 로 목록 위치를 찾아 스크롤한다(상단 '이전 메시지 로딩' 줄이 있으면 한 칸 밀린다). */
    suspend fun scrollToMessage(id: Int): Boolean {
        val i = vm.messages.indexOfFirst { it.id == id }
        if (i < 0) return false
        val offset = if (vm.isLoadingMore) 1 else 0
        listState.animateScrollToItem(i + offset)
        return true
    }

    /** 인용 상자 → 원문으로 스크롤하고 잠깐 강조(PACERA scrollToSeq). 화면에 받아 둔 메시지에 없으면 조용히 알린다. */
    fun jumpToReply(id: Int) {
        scope.launch {
            if (!scrollToMessage(id)) { vm.toast = "원문이 화면에 없어요"; return@launch }
            highlightId = id
            kotlinx.coroutines.delay(1200)
            if (highlightId == id) highlightId = null
        }
    }

    /** 말풍선 링크 — 무용연 링크는 앱 안 링크 처리, 남의 사이트는 확인 시트, 그 밖(전화 등)은 시스템으로. */
    fun openChatLink(url: String) {
        when {
            isMuyeonLink(url) -> openMuyeonLinkInApp(context, url)
            isExternalWebLink(url) -> externalLink = url
            else -> openExternal(context, url)
        }
    }

    LaunchedEffect(jumpTarget, vm.messages.size, vm.isLoadingMore, vm.initialLoaded) {
        val target = jumpTarget ?: return@LaunchedEffect
        if (!vm.initialLoaded || vm.isLoadingMore) return@LaunchedEffect
        val id = vm.messages.firstOrNull { target.matches(it) }?.id
        when {
            id != null -> {
                jumpTarget = null
                jumpLoadCount = 0
                // 키 변경으로 이 효과가 취소돼도 스크롤·강조는 끝까지 진행되도록 화면 범위에서 실행한다.
                scope.launch {
                    kotlinx.coroutines.delay(450)   // 첫 로드 직후 하단 스크롤과 겹치지 않게(iOS 0.45초)
                    if (!scrollToMessage(id)) return@launch
                    kotlinx.coroutines.delay(350)
                    highlightId = id
                    kotlinx.coroutines.delay(700)
                    if (highlightId == id) highlightId = null
                }
            }
            vm.hasMore && jumpLoadCount < MAX_JUMP_LOADS -> {
                jumpLoadCount++
                vm.loadMore()
            }
            else -> { jumpTarget = null; jumpLoadCount = 0 }   // 못 찾으면 포기(방은 이미 열려 있다)
        }
    }

    /** 견적 헤더 탭 — 견적 카드로 스크롤. 재입장으로 카드가 숨겨진 방은 서버 컨텍스트 요약 시트. */
    fun onQuoteHeader() {
        if (vm.quoteCardHiddenByRejoin) {
            showQuoteSummary = true
            return
        }
        val id = vm.firstQuoteCardId ?: return
        scope.launch { scrollToMessage(id) }
    }

    // ── 하단 자동 스크롤 ──
    //  · 마지막 메시지 id 기준 — 이전 메시지를 위에 붙일 때(id 변화 없음)는 내려가지 않는다.
    //  · 내 전송·전송 대기 말풍선 → 항상 하단으로.
    //  · 상대 새 메시지 → 이미 하단 근처를 보고 있을 때만 따라 내려간다(위 기록을 읽는 중이면 위치 유지).
    //  · 첫 로드 → 하단으로 즉시 이동.
    var prevLastId by remember { mutableStateOf<Int?>(null) }
    var prevPendingCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(vm.messages.lastOrNull()?.id, vm.pending.size) {
        val lastMsg = vm.messages.lastOrNull()
        val total = vm.messages.size + vm.pending.size
        val before = prevLastId
        val pendingGrew = vm.pending.size > prevPendingCount
        prevLastId = lastMsg?.id
        prevPendingCount = vm.pending.size
        if (total == 0) return@LaunchedEffect
        val offset = if (vm.isLoadingMore) 1 else 0
        val lastIndex = total - 1 + offset
        if (before == null) {
            listState.scrollToItem(lastIndex)
            return@LaunchedEffect
        }
        val mine = pendingGrew || (lastMsg != null && lastMsg.id != before && lastMsg.senderId == vm.currentUserId)
        // 직전 마지막 메시지가 화면 하단 근처(2칸 이내)에 보였으면 '하단을 보고 있던 중' 으로 본다.
        val prevIndex = vm.messages.indexOfFirst { it.id == before }.takeIf { it >= 0 }?.plus(offset)
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        val nearBottom = prevIndex == null || lastVisible >= prevIndex - 2
        if (mine || nearBottom) listState.animateScrollToItem(lastIndex)
    }

    // 키보드가 열리면(adjustResize 로 목록 높이가 줄어듦) 하단으로 — iOS 와 같다.
    //  답장 배너·칩 정도의 작은 변화는 무시하도록 일정 높이 이상 줄었을 때만 반응한다.
    val keyboardThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 120.dp.roundToPx() }
    LaunchedEffect(listState) {
        var prevHeight = 0
        snapshotFlow { listState.layoutInfo.viewportSize.height }
            .collect { h ->
                val shrunk = prevHeight > 0 && prevHeight - h >= keyboardThresholdPx
                prevHeight = h
                val total = listState.layoutInfo.totalItemsCount
                if (shrunk && total > 0) listState.animateScrollToItem(total - 1)
            }
    }

    // 위로 끝까지 → 이전 페이지
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { if (it <= 1) vm.loadMore() }
    }

    // 강사 쪽 견적 채팅방은 강사·학원 유형에서만 연다(알림·채팅 목록 어디서 들어오든 — 웹·iOS 와 같은 규칙).
    //  AOS 는 유형 전환이 웹에 있으므로, 전환 버튼은 웹 채팅방(같은 전환 화면 + 전환 후 그 방)으로 넘긴다.
    //  웹에서 전환하면 syncActiveType 브릿지로 ActiveRole 도 갱신된다.
    if (vm.quoteContext?.isTeacher == true &&
        ActiveRole.current(context) !in setOf(ActiveRole.TEACHER, ActiveRole.ACADEMY)
    ) {
        ProviderSwitchGate(
            onBack = onBack,
            onSwitch = {
                (context as? Activity)?.let { NativeWebRoute.openWebAndFinish(it, "/chat/${vm.roomId}") }
            },
        )
        return
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        RoomNavBar(
            vm, onBack,
            onOpenProfile = { openOpponentProfile() },
            onOpenNotificationSettings = {
                notiSettingsLauncher.launch(
                    com.muyeon.app.ui.notification.NotificationSettingsActivity.intent(context),
                )
            },
            onReport = { showReport = true },
            onBlock = { confirmBlock = true },
        )

        // 상단 레슨 컨텍스트(진행 카드·헤더·배너·CTA) — iOS 와 같이 상단바와 메시지 목록 사이.
        LessonContextArea(
            vm = vm,
            onOpenCycles = { showCyclesSheet = true },
            onCycleTimeline = { timelineCycle = it },
            // 채택 전(RESPONDED) 사이클의 [채택하기]는 그 사이클의 견적을 채택한다(대표 견적 아님).
            onCyclePrimary = { vm.acceptTarget = it; handleProgressPrimary(it.progress, it.isTeacher) },
            onLegacyTimeline = { showLegacyTimeline = true },
            onLegacyPrimary = { vm.acceptTarget = null; handleProgressPrimary(it, vm.quoteContext?.isTeacher == true) },
            onProposalChanged = { vm.reloadContext() },
            onOpenProposalPayment = { pid ->
                (context as? Activity)?.let { NativeWebRoute.openWebAndFinish(it, "/lesson-proposals/$pid/payment") }
            },
            onMemberSchedule = { showProposal = true },
            onQuoteHeader = { onQuoteHeader() },
            onOpenLesson = { openLesson(it) },
            onAcceptQuote = { vm.acceptTarget = null; showAcceptConfirm = true },
            onMatchedAction = {
                // iOS 와 같은 분기 — 이 배너는 progress 가 없을 때만 보이므로 실제로는 약속 제안 작성이 열린다.
                val p = vm.progress
                if (vm.isTeacherSide && p != null) openProgressSchedule(p) else showProposal = true
            },
        )

        // 메시지 목록 빈 곳을 누르면 키보드를 내린다(iOS 와 같다, PACERA c5611acf).
        //  스크롤(움직임)·말풍선 탭·꾹 누르기(자식이 소비)는 건드리지 않는다 — Final 단계에서 소비되지 않은 탭만 본다.
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF7F7F8))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val final = PointerEventPass.Final
                        val down = awaitFirstDown(requireUnconsumed = false, pass = final)
                        while (true) {
                            val change = awaitPointerEvent(final).changes.firstOrNull { it.id == down.id } ?: break
                            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
                            if (!change.pressed) {
                                if (!change.isConsumed) {
                                    focusManager.clearFocus()
                                    keyboard?.hide()
                                }
                                break
                            }
                        }
                    }
                },
        ) {
            if (vm.isLoading && vm.messages.isEmpty()) {
                CircularProgressIndicator(
                    color = MuyeonColors.primary,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (vm.isLoadingMore) {
                        item(key = "loading-more") {
                            Box(Modifier.fillMaxWidth().padding(8.dp), Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = MuyeonColors.primary, strokeWidth = 2.dp)
                            }
                        }
                    }
                    // key = 메시지 id — 위에 이전 메시지가 붙어도 보고 있던 위치가 유지된다.
                    items(vm.messages.size, key = { vm.messages[it].id }) { i ->
                        val m = vm.messages[i]
                        // ★ PACERA 묶음 — 날짜가 바뀌면 구분선, 같은 사람·같은 분이면 아바타·이름은 첫 말풍선에만, 시간은 마지막에만.
                        val divider = ChatGrouping.dayDivider(vm.messages, i)
                        val prev = vm.messages.getOrNull(i - 1)
                        val next = vm.messages.getOrNull(i + 1)
                        val startsGroup = divider != null || !ChatGrouping.continuesGroup(prev, m)
                        val endsGroup = ChatGrouping.dayDivider(vm.messages, i + 1) != null || !ChatGrouping.continuesGroup(m, next)
                        Column {
                        divider?.let { ChatDateDivider(it) }
                        MessageBubble(
                            message = m,
                            isMine = m.senderId == vm.currentUserId,
                            opponentImage = vm.opponentImage,
                            onOpenProfile = { openOpponentProfile() },
                            showAvatar = startsGroup && m.senderId != vm.currentUserId,
                            // PACERA iOS showsTimeLabel — 묶음 마지막 줄이거나 고친 글(수정됨)이면 시간.
                            showTime = endsGroup || (m.isEdited && !m.isDeleted),
                            replyQuote = replyQuoteModel(m, vm.messages),
                            onReplyQuoteClick = { id -> jumpToReply(id) },
                            read = isReadByOpponent(m, vm.opponentLastReadAt),
                            currentUserId = vm.currentUserId,
                            // 예약금 결제는 웹 결제 화면(iOS LessonPaymentWebView 와 같은 경로)으로 넘긴다.
                            onOpenProposalPayment = { pid ->
                                (context as? Activity)?.let {
                                    NativeWebRoute.openWebAndFinish(it, "/lesson-proposals/$pid/payment")
                                }
                            },
                            token = vm.tokenForCards,
                            onLongPress = { reactionTarget = m },
                            onToggleReaction = { emoji -> vm.toggleReaction(m, emoji) },
                            onOpenLink = { url -> openChatLink(url) },
                            onProposalChanged = { vm.reloadContext() },
                            onOpenProvider = { id, isAcademy ->
                                if (isAcademy) {
                                    com.muyeon.app.ui.academy.AcademyProfileActivity.start(context, id)
                                } else {
                                    // 채팅 맥락 — 열람기록 제외·CTA 숨김(iOS src:"chat", hideCta:true).
                                    com.muyeon.app.ui.resume.ResumeActivity.startProfile(context, id, "chat")
                                }
                            },
                            // 네이티브 설문 화면. 내가 보낸 카드(강사)면 열람만, 받은 쪽이면 응답 가능
                            //  — iOS SurveyOpen(canRespond: !isMine) 과 같은 규칙.
                            onOpenSurvey = { did ->
                                surveyLauncher.launch(
                                    com.muyeon.app.ui.survey.SurveyActivity.intent(
                                        context, did, canRespond = m.senderId != vm.currentUserId,
                                    ),
                                )
                            },
                            onOpenLesson = { lid ->
                                lessonLauncher.launch(com.muyeon.app.ui.lesson.LessonActivity.detailIntent(context, lid))
                            },
                            // 설문 응답·수정 알림 탭 → 같은 설문 카드로 스크롤·강조(iOS SurveyUpdateBubble).
                            onSurveyUpdate = { did -> jumpTarget = CardJumpTarget.Survey(did) },
                            highlighted = highlightId == m.id,
                        )
                        }
                    }
                    items(vm.pending.size, key = { "pending-" + vm.pending[it].localId }) { i ->
                        PendingBubble(vm.pending[i]) { vm.retry(vm.pending[i]) }
                    }
                }
            }

            if (vm.isUploadingMedia) {
                Text(
                    "사진 보내는 중…",
                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp,
                    color = MuyeonColors.textSub,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 4.dp),
                )
            }

            // 안내 문구(복사·신고·전송 거절 사유 등) — 견적 화면과 같은 하단 캡슐 토스트.
            vm.toast?.let { com.muyeon.app.ui.quote.ToastBubble(it, Modifier.align(Alignment.BottomCenter)) }
        }

        // AI 답장 추천 칩(프로 · 강사/원장) — 상대의 마지막 메시지에 아직 답하지 않았을 때. 누르면 입력창에 채운다(바로 보내지 않음).
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val lastOther = vm.messages.lastOrNull { it.type != "SYSTEM" }?.takeIf { it.senderId != vm.currentUserId }
        LaunchedEffect(lastOther?.id) {
            vm.onAiReplySent = { intent, text ->
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    com.muyeon.app.ui.assistant.AssistantApi.call(ctx, "POST", "/assistant/chat-replies/used",
                        org.json.JSONObject().put("roomId", vm.roomId).put("intent", intent).put("text", text))
                }
            }
            if (lastOther == null) { vm.aiReplies = emptyList(); return@LaunchedEffect }
            kotlinx.coroutines.delay(600) // 연달아 오는 메시지를 묶어서 한 번만
            val r = com.muyeon.app.ui.assistant.AssistantApi.call(ctx, "POST", "/assistant/chat-replies", org.json.JSONObject().put("roomId", vm.roomId))
            val arr = r.json?.optJSONArray("replies")
            vm.aiReplyIntent = r.json?.optString("intent")?.takeIf { it.isNotEmpty() && it != "null" }
            vm.aiReplies = (0 until (arr?.length() ?: 0)).mapNotNull { arr!!.optJSONObject(it)?.optString("text")?.takeIf { t -> t.isNotBlank() } }
        }
        if (vm.aiReplies.isNotEmpty() && vm.input.isEmpty() && vm.editingMessage == null) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("AI 추천", fontFamily = customFontFamily, fontSize = 12.sp, color = MuyeonColors.textSub)
                vm.aiReplies.forEach { t ->
                    Text(
                        t, fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textHead, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, MuyeonColors.border, RoundedCornerShape(50))
                            .clickable { vm.pickAiReply(t) }.padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("chat.aiReply"),
                    )
                }
            }
        }

        // 빠른 답변 칩 — 상대(강사)가 등록해둔 질문을 탭 한 번으로 전송.
        //  내 첫 메시지 전·입력창 비어 있음·답장/수정 중 아님일 때만(iOS 와 같은 조건).
        if (vm.quickReplies.isNotEmpty() && !vm.hasMyMessage && vm.input.isEmpty() &&
            vm.editingMessage == null && vm.replyingTo == null
        ) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                vm.quickReplies.forEach { q ->
                    Text(
                        q.text,
                        fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                        color = MuyeonColors.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MuyeonColors.primary.copy(alpha = 0.08f))
                            .clickable { vm.sendQuick(q.text) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }

        // 수정 중이면 입력바 대신 수정 카드(iOS MessageEditCard), 아니면 답장 배너 + 입력바.
        val editing = vm.editingMessage
        if (editing != null) {
            MessageEditCard(
                original = editing.content,
                text = vm.input,
                onTextChange = vm::onInputChange,
                onCancel = { vm.cancelCompose() },
                onSave = { vm.send() },
            )
        } else {
            ReplyBanner(vm)
            if (showSurveyHint) {
                ChatSurveyHintBubble(
                    onOpen = { showSurveyHint = false; showSurveyPicker = true },
                    onClose = { showSurveyHint = false },
                )
            }
            ChatInputBar(vm, onAttach = { showSurveyHint = false; showAttach = true })
        }
    }

    if (showAttach) {
        ChatAttachSheet(
            showSurvey = vm.quoteContext?.isTeacher == true,
            // 채택된 견적 방은 회원·강사 모두, 그 밖의 방은 강사·학원 유형만 약속 제안 가능(iOS 와 같은 조건).
            //  최종 검증은 서버가 한다.
            showProposal = vm.isQuoteMatched || vm.quoteContext?.isTeacher == true ||
                ActiveRole.current(context) in setOf(ActiveRole.TEACHER, ActiveRole.ACADEMY),
            onPickImages = { uris -> vm.sendImages(context, uris) },
            onPickVideo = { uri -> vm.sendVideo(context, uri) },
            onSurvey = { showSurveyPicker = true },
            onProposal = { showProposal = true },
            onDismiss = { showAttach = false },
            onCameraDenied = { vm.toast = "카메라 권한을 허용해 주세요." },
        )
    }
    if (showSurveyPicker) {
        SurveyPickerSheet(
            api = remember { com.muyeon.app.ui.survey.SurveyApi(vm.tokenForCards) },
            roomId = vm.roomId,
            recipientId = vm.opponentId,
            onSent = { showSurveyPicker = false; vm.toast = "설문지를 보냈어요." },
            onDismiss = { showSurveyPicker = false },
        )
    }
    if (showProposal) {
        LessonProposalComposer(
            api = remember { LessonProposalApi(vm.tokenForCards) },
            calendarApi = remember { com.muyeon.app.ui.lesson.UserCalendarApi(vm.tokenForCards) },
            roomId = vm.roomId,
            isTeacher = vm.quoteContext?.isTeacher == true,
            // 금액·예약금은 회원 쪽 견적 맥락에서 가져온다(회원 화면의 예약금 안내에만 쓰인다).
            totalPrice = vm.memberBookingContext?.priceAmount ?: 0,
            depositAmount = vm.memberBookingContext?.takeIf { it.paymentMode == "DEPOSIT" }?.depositAmount ?: 0,
            onSent = { showProposal = false; vm.toast = "약속을 제안했어요."; vm.reloadContext() },
            onDismiss = { showProposal = false; vm.reloadContext() },
        )
    }
    if (showCyclesSheet) {
        LessonCyclesSheet(
            cycles = vm.lessonCycles,
            opponentImage = vm.opponentImage,
            waitingProposal = vm.proposalWaiting,
            onTimeline = { c -> showCyclesSheet = false; timelineCycle = c },
            onPrimary = { c -> showCyclesSheet = false; vm.acceptTarget = c; handleProgressPrimary(c.progress, c.isTeacher) },
            onDismiss = { showCyclesSheet = false },
        )
    }
    // 레거시 대표 진행 타임라인 — 역할은 방 단위 quoteContext 기준.
    val legacyProgress = vm.progress
    if (showLegacyTimeline && legacyProgress != null) {
        val qc = vm.quoteContext
        LessonTimelineSheet(
            progress = legacyProgress,
            context = qc,
            category = vm.contextCategory,
            onQuoteDetail = if (qc?.isTeacher == false) { { showLegacyTimeline = false; vm.acceptTarget = null; showAcceptConfirm = true } } else null,
            onSetSchedule = if (qc?.isTeacher == true) {
                { showLegacyTimeline = false; openProgressSchedule(legacyProgress) }
            } else null,
            onOpenCalendar = legacyProgress.lessonId?.let { lid -> { showLegacyTimeline = false; openLesson(lid) } },
            onReview = if (qc?.isTeacher == false) {
                { showLegacyTimeline = false; startReviewFlow() }
            } else {
                { showLegacyTimeline = false; teacherReviewInfo = true }
            },
            reviewDisabledStyle = qc?.isTeacher == true,
            onDismiss = { showLegacyTimeline = false },
        )
    }
    // 사이클별 타임라인 — 역할(강사/회원)은 사이클 기준으로 판정.
    timelineCycle?.let { cycle ->
        LessonTimelineSheet(
            progress = cycle.progress,
            context = cycle.asContext,
            category = cycle.title,
            isProposal = cycle.isProposal,
            onQuoteDetail = if (!cycle.isTeacher && !cycle.isProposal) {
                { timelineCycle = null; vm.acceptTarget = cycle; showAcceptConfirm = true }
            } else null,
            onSetSchedule = if (cycle.isTeacher && !cycle.isProposal) {
                { timelineCycle = null; openProgressSchedule(cycle.progress) }
            } else null,
            onOpenCalendar = cycle.progress.lessonId?.let { lid -> { timelineCycle = null; openLesson(lid) } },
            onReview = if (!cycle.isTeacher) {
                { timelineCycle = null; startReviewFlow() }
            } else {
                { timelineCycle = null; teacherReviewInfo = true }
            },
            reviewDisabledStyle = cycle.isTeacher,
            onDismiss = { timelineCycle = null },
        )
    }
    // 재입장 방 견적 요약 — 고객이면 받은 견적 상세(네이티브 견적 허브)로 이어진다.
    if (showQuoteSummary) {
        val qc = vm.quoteContext
        QuoteContextSummarySheet(
            context = qc,
            category = vm.contextCategory,
            onOpenDetail = if (qc != null && !qc.isTeacher) {
                {
                    showQuoteSummary = false
                    quoteLauncher.launch(
                        com.muyeon.app.ui.quote.QuoteHubActivity.intent(context, isPro = false, quoteId = qc.quoteId),
                    )
                }
            } else null,
            onDismiss = { showQuoteSummary = false },
        )
    }
    // 강사 채택(고객)
    if (showAcceptConfirm) {
        com.muyeon.app.ui.quote.QuoteDialog(
            title = "이 강사로 진행할까요?",
            message = "채택하면 이 요청은 마감되고 다른 견적은 받을 수 없어요.",
            confirmText = "채택하기",
            onConfirm = {
                showAcceptConfirm = false
                scope.launch {
                    val ok = vm.acceptQuote()
                    vm.toast = if (ok) "강사를 채택했어요. 채팅에서 일정을 확정해 주세요."
                    else "채택에 실패했어요. 잠시 후 다시 시도해 주세요."
                }
            },
            onDismiss = { showAcceptConfirm = false },
        )
    }
    // 후기 — 활성유형이 일반이 아니면 전환 안내.
    //  AOS 에는 네이티브 유형 전환 API 가 없어, 확인 시 웹 후기 화면으로 이동하고 유형 확인은 웹이 처리한다.
    if (showReviewSwitch) {
        com.muyeon.app.ui.quote.QuoteDialog(
            title = "후기는 일반 회원 화면에서 작성해요",
            message = "일반 유형으로 전환한 뒤 후기 작성 화면으로 이동합니다.",
            confirmText = "일반 유형으로 전환하고 후기 쓰기",
            onConfirm = { showReviewSwitch = false; openReviewPage() },
            onDismiss = { showReviewSwitch = false },
        )
    }
    // 강사 방향 레슨 — 후기 대상 아님 안내(확인 버튼 하나).
    if (teacherReviewInfo) {
        com.muyeon.app.ui.common.MuyeonDialog(
            title = "이 레슨에서는 후기를 쓸 수 없어요",
            message = "이 레슨에서 회원님은 강사예요. 후기는 수강한 회원이 남길 수 있어요.",
            actions = listOf(com.muyeon.app.ui.common.MuyeonDialogAction("확인")),
            onDismiss = { teacherReviewInfo = false },
        )
    }
    if (confirmBlock) {
        val scope2 = rememberCoroutineScope()
        com.muyeon.app.ui.quote.QuoteDialog(
            "${vm.title}님을 차단할까요?",
            "서로의 채팅 목록에서 사라지고 더 이상 대화할 수 없어요.\n채팅 목록 우측 상단에서 차단을 풀 수 있어요.",
            "차단하기",
            onConfirm = {
                confirmBlock = false
                scope2.launch {
                    vm.api.blockUser(vm.opponentId)
                        // 차단하면 이 방은 목록에서 사라진다 — 방에 남아 있을 이유가 없다.
                        .onSuccess { onBack() }
                        .onFailure { vm.toast = it.message ?: "차단하지 못했어요." }
                }
            },
            onDismiss = { confirmBlock = false },
        )
    }

    if (showReport) {
        ChatReportSheet(
            roomId = vm.roomId,
            opponentName = vm.title,
            token = vm.tokenForCards,
            onDone = { showReport = false; vm.toast = "신고가 접수됐어요." },
            onDismiss = { showReport = false },
        )
    }
    // 메시지 단위 신고(CHAT_MESSAGE, targetId=메시지 id) — 같은 시트 재사용
    reportMessage?.let { m ->
        ChatReportSheet(
            roomId = vm.roomId,
            opponentName = vm.title,
            token = vm.tokenForCards,
            message = m,
            onDone = { reportMessage = null; vm.toast = "신고가 접수됐어요." },
            onDismiss = { reportMessage = null },
        )
    }
    externalLink?.let { url ->
        ChatExternalLinkSheet(
            url = url,
            onOpen = { externalLink = null; openExternal(context, url) },
            onDismiss = { externalLink = null },
        )
    }
    // 길게 누르기 — PACERA 시트(반응 줄 + 답장·복사·편집·삭제·신고·차단). 동작은 무용연 기존 구현 그대로.
    reactionTarget?.let { m ->
        val mine = m.senderId == vm.currentUserId
        ChatMessageActionSheet(
            actions = ChatMessageRules.actions(m, mine, canBlock = vm.opponentId > 0),
            onDismiss = { reactionTarget = null },
            onReact = { emoji -> vm.toggleReaction(m, emoji); reactionTarget = null },
            onReply = { vm.startReply(m); reactionTarget = null },
            onCopy = {
                copyToClipboard(context, m.content)
                // Android 13 이상은 시스템이 복사 확인을 띄우므로 앱 문구는 생략한다(중복 표시 방지).
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                    vm.toast = "메시지를 복사했어요."
                }
                reactionTarget = null
            },
            onEdit = { vm.startEdit(m); reactionTarget = null },
            onDelete = { vm.deleteMessage(m); reactionTarget = null },
            onReport = { reportMessage = m; reactionTarget = null },
            // 차단은 우상단 메뉴와 같은 확인 다이얼로그를 띄운다.
            onBlock = { confirmBlock = true; reactionTarget = null },
        )
    }
    // 이번 달 새 강사·무용수 채팅 한도(전송 거절 RECRUIT_CHAT_LIMIT) — 이력서 화면과 같은 팝업·멤버십 화면
    vm.chatLimit?.let { limit ->
        com.muyeon.app.ui.resume.RecruitChatLimitDialog(
            e = limit,
            onUpgrade = { vm.chatLimit = null; com.muyeon.app.ui.membership.MembershipActivity.start(context) },
            onDismiss = { vm.chatLimit = null },
        )
    }
    vm.toast?.let { msg ->
        LaunchedEffect(msg) { kotlinx.coroutines.delay(2000); vm.toast = null }
    }
}

/** 메시지 복사 — iOS vm.copyToClipboard 대응. */
private fun copyToClipboard(context: android.content.Context, text: String) {
    val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
        as? android.content.ClipboardManager ?: return
    cm.setPrimaryClip(android.content.ClipData.newPlainText("muyeon", text))
}

@Composable
private fun RoomNavBar(
    vm: ChatRoomViewModel,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
) {
    // 활동 상태 문구는 시간이 지나면 바뀐다(접속 중 → N분 전) — 1분마다 기준 시각을 갱신한다.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            now = System.currentTimeMillis()
        }
    }
    val presence = vm.presenceText(maxOf(now, System.currentTimeMillis()))

    Box(
        Modifier.fillMaxWidth().height(48.dp).background(MuyeonColors.surface),
        contentAlignment = Alignment.Center,
    ) {
        // 이름 + 부제(입력 중 / 접속·활동 상태). 탭 → 상대 공개 프로필(iOS 상단바와 같다).
        Column(
            Modifier
                .padding(horizontal = 96.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = vm.opponentId > 0, onClick = onOpenProfile)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                vm.title.ifBlank { "채팅" },
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = MuyeonColors.textHead, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (vm.isOtherTyping) {
                Text(
                    "입력 중…",
                    fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 13.sp,
                    color = MuyeonColors.primary,
                )
            } else if (presence != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (presence == ChatRoomViewModel.PRESENCE_ONLINE) {
                        Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(Color(0xFF34C759)))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        presence,
                        fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 13.sp,
                        color = MuyeonColors.secondary,
                    )
                }
            }
        }
        Box(
            Modifier.align(Alignment.CenterStart).padding(start = 4.dp).size(44.dp).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = MuyeonColors.textHead, modifier = Modifier.size(18.dp))
        }
        Row(Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)) {
            // 방별 알림 음소거 토글
            Box(Modifier.size(44.dp).clickable { vm.toggleMute(!vm.muted) }, contentAlignment = Alignment.Center) {
                Icon(
                    if (vm.muted) Icons.Filled.NotificationsOff else Icons.Filled.Notifications,
                    if (vm.muted) "알림 켜기" else "알림 끄기",
                    tint = if (vm.muted) MuyeonColors.secondary else MuyeonColors.textHead,
                    modifier = Modifier.size(18.dp),
                )
            }
            // 신고·차단 — iOS 우상단 ⋯ 메뉴.
            //  App Store Guideline 1.2 는 UGC·채팅 앱에 신고와 차단을 **둘 다** 요구한다.
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                Box(
                    Modifier.size(44.dp).clickable { menuOpen = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MoreVert, "더보기", tint = MuyeonColors.textHead, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    // 전체 알림 설정 — iOS 방 설정 시트의 '전체 알림 설정'과 같은 화면.
                    DropdownMenuItem(
                        text = {
                            Text("전체 알림 설정", fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textHead)
                        },
                        onClick = { menuOpen = false; onOpenNotificationSettings() },
                    )
                    DropdownMenuItem(
                        text = {
                            Text("신고하기", fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.danger)
                        },
                        onClick = { menuOpen = false; onReport() },
                    )
                    if (vm.opponentId > 0) {
                        DropdownMenuItem(
                            text = {
                                Text("차단하기", fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.danger)
                            },
                            onClick = { menuOpen = false; onBlock() },
                        )
                    }
                }
            }
        }
    }
}

/** 답장 대상 배너 — 입력바 위에 붙는다. 수정은 [MessageEditCard] 가 담당한다. */
@Composable
private fun ReplyBanner(vm: ChatRoomViewModel) {
    val reply = vm.replyingTo ?: return
    Row(
        Modifier.fillMaxWidth().background(Color(0xFFF2F2F7)).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "답장",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                lineHeight = 14.sp, color = MuyeonColors.primary,
            )
            Text(
                reply.content,
                fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                color = MuyeonColors.textSub, maxLines = 1,
            )
        }
        Icon(
            Icons.Filled.Close, "취소", tint = MuyeonColors.secondary,
            modifier = Modifier.size(16.dp).clickable { vm.cancelCompose() },
        )
    }
}

/**
 * 메시지 수정 카드 — iOS MessageEditCard. 입력바 자리에 원문 + 수정 입력 + 원형 ✕/✓.
 *  ✓ 는 내용이 바뀌었고 비어 있지 않을 때만 누를 수 있다. 열리면 입력칸에 바로 포커스.
 */
@Composable
private fun MessageEditCard(
    original: String,
    text: String,
    onTextChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val trimmed = text.trim()
    val changed = trimmed.isNotEmpty() && trimmed != original
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    // 커서를 문장 끝에 두기 위해 TextFieldValue 로 다룬다(원문으로 채운 직후 커서가 맨 앞에 오지 않게).
    var field by remember {
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(text.length)))
    }
    if (field.text != text) {
        field = androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(text.length))
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MuyeonColors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF2F2F7))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "메시지 수정",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.size(28.dp).clip(RoundedCornerShape(50)).background(Color(0xFFE5E5EA))
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, "수정 취소", tint = MuyeonColors.textSub, modifier = Modifier.size(14.dp))
            }
        }
        Text(
            original,
            fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 18.sp,
            color = MuyeonColors.textSub, maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BasicTextField(
                value = field,
                onValueChange = { field = it; if (it.text != text) onTextChange(it.text) },
                textStyle = TextStyle(
                    fontFamily = customFontFamily, fontSize = 16.sp, lineHeight = 21.sp,
                    color = MuyeonColors.textHead,
                ),
                cursorBrush = SolidColor(MuyeonColors.primary),
                maxLines = 5,
                modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                    .focusRequester(focusRequester),
            )
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (changed) Color.Black else Color(0xFFD1D1D6))
                    .clickable(enabled = changed, onClick = onSave),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, "수정 완료", tint = Color.White, modifier = Modifier.size(17.dp))
            }
        }
    }
}

/**
 * 입력바 — PACERA Android `ChatInputBar` 와 같은 모양: [+](무용연 첨부 시트 그대로) · 둥근 입력칸(최소 36, 4줄까지 늘고
 *  그 뒤는 안에서 스크롤, 글자 14) · 36dp 동그란 전송(보낼 수 있으면 주황, 아니면 회색).
 */
@Composable
private fun ChatInputBar(vm: ChatRoomViewModel, onAttach: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MuyeonColors.surface).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        // 첨부(+) — 무용연 첨부 시트(사진·동영상·설문·약속 제안)를 연다.
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(MuyeonColors.groupedBg).clickable(onClick = onAttach),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Add, "첨부", tint = MuyeonColors.textSub, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.weight(1f).heightIn(min = 36.dp, max = 120.dp).clip(RoundedCornerShape(18.dp))
                .background(MuyeonColors.groupedBg).padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (vm.input.isEmpty()) {
                Text("메시지를 입력하세요", fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.hint)
            }
            // AI 추천 칩·드래프트처럼 바깥에서 글이 바뀌면 커서를 글 끝으로(String 판은 맨 앞에 남는다 — 10-23).
            var tfv by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(vm.input, androidx.compose.ui.text.TextRange(vm.input.length))) }
            if (tfv.text != vm.input) tfv = androidx.compose.ui.text.input.TextFieldValue(vm.input, androidx.compose.ui.text.TextRange(vm.input.length))
            BasicTextField(
                value = tfv,
                onValueChange = { tfv = it; if (it.text != vm.input) vm.onInputChange(it.text) },
                textStyle = TextStyle(fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.textHead),
                cursorBrush = SolidColor(MuyeonColors.primary),
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(8.dp))
        val canSend = vm.input.isNotBlank()
        Box(
            Modifier.size(36.dp).clip(CircleShape)
                .background(if (canSend) MuyeonColors.primary else MuyeonColors.groupedBg)
                .clickable(enabled = canSend) { vm.send() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, "전송", tint = if (canSend) Color.White else MuyeonColors.hint, modifier = Modifier.size(18.dp))
        }
    }
}

/** 내 메시지를 상대가 읽었는지 — 상대 lastReadAt 이 메시지 시각 이후면 읽음. */
private fun isReadByOpponent(m: ChatMessage, opponentLastReadAt: Long?): Boolean {
    val read = opponentLastReadAt ?: return false
    val sent = QuoteUi.parseDate(m.createdAt) ?: return false
    return read >= sent
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    opponentImage: String?,
    onOpenProfile: () -> Unit,
    showAvatar: Boolean,
    read: Boolean,
    currentUserId: Int,
    onOpenProposalPayment: (Int) -> Unit,
    token: String?,
    onLongPress: () -> Unit,
    onToggleReaction: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onProposalChanged: () -> Unit,
    onOpenProvider: (Int, Boolean) -> Unit,
    onOpenSurvey: (Int) -> Unit,
    onOpenLesson: (Int) -> Unit,
    onSurveyUpdate: (Int) -> Unit,
    highlighted: Boolean,
    /** PACERA 묶음 — 마지막 말풍선에만 시간. */
    showTime: Boolean = true,
    /** PACERA 답장 인용 — 글 말풍선은 안쪽 위, 사진·카드는 말풍선 위. 누르면 원문으로. */
    replyQuote: ReplyQuoteModel? = null,
    onReplyQuoteClick: (Int) -> Unit = {},
) {
    // ── 말풍선이 아니라 전용 카드/안내로 그리는 타입들 ──
    //  ⚠️ 여기서 안 받으면 `else -> Text(content)` 로 떨어져 JSON 원문이 그대로 노출된다.
    if (!message.isDeleted) {
        when (message.type) {
            "SYSTEM", "QUOTE_REQUEST" -> {
                SystemNoticeBubble(message.type, message.content)
                return
            }
            "SURVEY_UPDATE" -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SurveyUpdateBubble(message.content, onSurveyUpdate)
                }
                return
            }
            "LESSON_CARD" -> {
                // 양쪽 공통(가운데) — 누가 등록했는지는 카드 안 문구로 구분한다.
                LessonCardBubble(message.content, onOpenLesson)
                return
            }
            "QUOTE_CARD" -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Top,
                ) {
                    // 받은 견적 카드도 글 말풍선처럼 왼쪽에 강사 사진(34dp) — 묶음 첫 줄에만, 뒤이은 인사말은 들여쓰기만.
                    if (!isMine) {
                        if (showAvatar) {
                            QuoteAvatar(
                                opponentImage, message.sender?.displayName ?: "상대", 34.dp,
                                modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onOpenProfile),
                            )
                        } else {
                            Spacer(Modifier.width(34.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                        if (!isMine && showAvatar) {
                            message.sender?.displayName?.takeIf { it.isNotBlank() }?.let { name ->
                                Text(name, fontFamily = customFontFamily, fontSize = 11.sp, color = MuyeonColors.textSub, modifier = Modifier.padding(bottom = 2.dp, start = 4.dp))
                            }
                        }
                        // 제공자 본인이 보낸 카드엔 자기 프로필 버튼을 노출하지 않는다.
                        QuoteCardBubble(message.content, showProviderProfile = !isMine, onOpenProvider = onOpenProvider)
                    }
                }
                return
            }
            "SURVEY_CARD" -> {
                Row(
                    Modifier.fillMaxWidth().shake(highlighted),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                ) {
                    SurveyCardBubble(
                        json = message.content,
                        done = message.surveyDone == true,
                        seq = message.surveySeq ?: 0,
                        sentAt = message.surveySentAt,
                        revision = message.surveyRevision ?: 0,
                        onOpen = onOpenSurvey,
                    )
                }
                return
            }
            // 레슨 약속 제안은 전용 카드로 렌더(iOS LessonProposalCardBubble).
            "LESSON_PROPOSAL" -> {
                Row(
                    Modifier.fillMaxWidth().shake(highlighted),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                ) {
                    LessonProposalBubble(
                        contentJson = message.content,
                        isProposer = isMine,
                        currentUserId = currentUserId,
                        token = token,
                        onChanged = onProposalChanged,
                        onOpenPayment = onOpenProposalPayment,
                    )
                }
                return
            }
        }
    }
    // 말풍선 배치는 PACERA ChatBubble 과 같다 — 꼬리 없는 둥근 사각형, 시간·읽음은 말풍선 옆 아래.
    //  내 메시지는 [시간][말풍선], 상대 메시지는 [아바타][말풍선][시간].
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isMine) {
            // 연속된 상대 메시지는 첫 말풍선에만 아바타(34dp), 나머지는 같은 너비만큼 비워 왼쪽 선을 맞춘다.
            if (showAvatar) {
                // 아바타 탭 → 상대 공개 프로필(iOS 상단바·말풍선 아바타와 같은 목적지).
                QuoteAvatar(
                    opponentImage, message.sender?.displayName ?: "상대", 34.dp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onOpenProfile),
                )
            } else {
                Spacer(Modifier.width(34.dp))
            }
            Spacer(Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start, modifier = Modifier.replyHighlight(highlighted)) {
            // PACERA — 묶음 첫 말풍선이면 상대 이름(11sp).
            if (!isMine && showAvatar) {
                message.sender?.displayName?.takeIf { it.isNotBlank() }?.let { name ->
                    Text(name, fontFamily = customFontFamily, fontSize = 11.sp, color = MuyeonColors.textSub, modifier = Modifier.padding(bottom = 2.dp, start = 4.dp))
                }
            }
            // 답장 인용 — 글 말풍선은 안쪽 위에, 사진·동영상 등은 말풍선 위에 따로(PACERA outerQuote).
            val quote = replyQuote?.takeIf { !message.isDeleted }
            if (quote != null && message.type != "TEXT") {
                ReplyQuote(quote, onMine = false, onClick = onReplyQuoteClick, standalone = true)
                Spacer(Modifier.height(4.dp))
            }
            // 이미지 말풍선은 배경 없이 사진만 보여준다(iOS ChatImageBubble — 말풍선 밖).
            val bare = message.type == "IMAGE" && !message.isDeleted
            Row(verticalAlignment = Alignment.Bottom) {
                if (isMine) {
                    BubbleMeta(message, read = read, alignEnd = true, showTime = showTime)
                    Spacer(Modifier.width(4.dp))
                }
                Box(
                    Modifier
                        // 시간 칸은 제 너비(한 줄)를 먼저 갖고, 말풍선은 남은 너비 안에서 최대 260(PACERA 와 같은 배치).
                        .weight(1f, fill = false)
                        .widthIn(max = 260.dp)
                        .then(
                            if (bare) Modifier.clip(RoundedCornerShape(12.dp))
                            else Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isMine) MuyeonColors.primary else Color(0xFFF2F2F7))
                                // 탭은 동작 없음, 길게 누르면 반응·복사·답장·수정·삭제 메뉴(iOS 컨텍스트 메뉴와 같다).
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = onLongPress,
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ),
                ) {
                    when {
                        message.isDeleted -> Text(
                            "삭제된 메시지예요.",
                            fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 20.sp,
                            color = if (isMine) Color.White.copy(alpha = 0.7f) else MuyeonColors.secondary,
                        )
                        message.type == "IMAGE" -> ChatImageBubble(
                            urls = message.imageUrls,
                            // 저장은 내가 보낸 사진만(iOS viewerAllowsSaving = vm.isMine)
                            allowsSaving = isMine,
                            onLongPress = onLongPress,
                        )
                        message.type == "VIDEO" -> ChatVideoBubble(message.imageUrl.orEmpty())
                        else -> Column {
                            quote?.let {
                                ReplyQuote(it, onMine = isMine, onClick = onReplyQuoteClick)
                                Spacer(Modifier.height(6.dp))
                            }
                            // PACERA — 링크는 눌러서 연다(밑줄). 내 말풍선은 글자색 그대로 밑줄만.
                            ChatLinkBodyText(
                                text = message.content,
                                color = if (isMine) Color.White else MuyeonColors.textHead,
                                style = TextStyle(fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 20.sp),
                                linkColor = if (isMine) Color.White else MuyeonColors.primary,
                                onOpenLink = onOpenLink,
                                onLongPress = onLongPress,
                            )
                        }
                    }
                }
                if (!isMine) {
                    Spacer(Modifier.width(4.dp))
                    BubbleMeta(message, read = false, alignEnd = false, showTime = showTime)
                }
            }
            // 텍스트 안 URL 미리보기(카톡식)
            if (!message.isDeleted && message.type == "TEXT") {
                ChatLinkDetector.firstUrl(message.content)?.let { url ->
                    Spacer(Modifier.height(4.dp))
                    LinkPreviewCard(url, onOpenLink)
                }
            }
            // 반응 칩(PACERA ReactionChips) — 지운 메시지엔 없음.
            if (!message.isDeleted) ReactionChips(ChatReactionRules.sorted(message.reactions.orEmpty()), onToggleReaction)
        }
    }
}

/** 말풍선 옆 메타 — 읽음(내 메시지만)·수정됨·시간. alignEnd 는 내 메시지(말풍선 왼쪽에 놓임)일 때 true. */
@Composable
private fun BubbleMeta(message: ChatMessage, read: Boolean, alignEnd: Boolean, showTime: Boolean = true) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        modifier = Modifier.padding(bottom = 2.dp),
    ) {
        if (read) {
            Text(
                "읽음",
                fontFamily = customFontFamily, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1, softWrap = false,
                color = MuyeonColors.primary,
            )
        }
        if (message.isEdited && !message.isDeleted) {
            Text(
                "수정됨",
                fontFamily = customFontFamily, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1, softWrap = false,
                color = MuyeonColors.secondary,
            )
        }
        // PACERA — 시간은 묶음의 마지막 말풍선에만, "오후 3:12"(날짜는 구분선이 보여 준다).
        if (showTime) {
            Text(
                ChatTimeFormat.forBubble(message.createdAt),
                fontFamily = customFontFamily, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1, softWrap = false,
                color = MuyeonColors.secondary,
            )
        }
    }
}

@Composable
private fun PendingBubble(p: ChatRoomViewModel.Pending, onRetry: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Bottom) {
        if (p.failed) {
            Icon(
                Icons.Filled.Refresh, "재전송", tint = MuyeonColors.danger,
                modifier = Modifier.size(16.dp).clickable(onClick = onRetry).padding(end = 2.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Box(
            Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MuyeonColors.primary.copy(alpha = if (p.failed) 0.45f else 0.7f))
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Text(
                p.content.ifEmpty {
                    when (p.type) {
                        "IMAGE" -> "사진"
                        "VIDEO" -> "동영상"
                        else -> ""
                    }
                },
                fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 20.sp,
                color = Color.White, textAlign = TextAlign.Start,
            )
        }
    }
}


/** 링크 프리뷰/도메인 칩 탭 → 외부 브라우저. */
private fun openExternal(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** 강사 유형 전환 안내 — 웹 LessonProviderSwitchView·iOS ChatRoomView 게이트와 같은 문구. */
@Composable
private fun ProviderSwitchGate(onBack: () -> Unit, onSwitch: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MuyeonColors.surface).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("이 대화는 강사 유형에서 확인할 수 있어요.", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(
            "강사 유형으로 전환한 후 이용해 주세요.",
            Modifier.padding(top = 8.dp), fontSize = 14.sp, color = MuyeonColors.textSub,
        )
        Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack) { Text("돌아가기") }
            Button(
                onClick = onSwitch,
                colors = ButtonDefaults.buttonColors(containerColor = MuyeonColors.primary),
            ) { Text("강사 유형으로 전환") }
        }
    }
}

/** 딥링크·설문 알림으로 찾아갈 카드 — 설문(dispatchId) 또는 약속 제안(proposalId). */
private sealed interface CardJumpTarget {
    fun matches(m: ChatMessage): Boolean

    data class Survey(val dispatchId: Int) : CardJumpTarget {
        override fun matches(m: ChatMessage): Boolean =
            m.type == "SURVEY_CARD" && runCatching {
                org.json.JSONObject(m.content).optInt("dispatchId")
            }.getOrDefault(0) == dispatchId
    }

    data class Proposal(val proposalId: Int) : CardJumpTarget {
        override fun matches(m: ChatMessage): Boolean =
            m.type == "LESSON_PROPOSAL" && LessonProposalCard.parse(m.content)?.proposalId == proposalId
    }
}

/** 대상 카드를 찾을 때 이전 메시지를 더 불러오는 최대 횟수(50건 × 20 = 1,000건). */
private const val MAX_JUMP_LOADS = 20

/**
 * 인용 상자로 찾아온 원문 — PACERA iOS 와 같은 값: 주황 12% 둥근(12) 바탕이 0.2초에 켜지고,
 *  1.2초 뒤 꺼질 때 0.3초에 사라진다(켜고 끄는 시점은 [jumpToReply]).
 */
@Composable
private fun Modifier.replyHighlight(active: Boolean): Modifier {
    val a = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(active) {
        a.animateTo(if (active) 0.12f else 0f, androidx.compose.animation.core.tween(durationMillis = if (active) 200 else 300))
    }
    return this.then(Modifier.background(MuyeonColors.primary.copy(alpha = a.value), RoundedCornerShape(12.dp)))
}

/** 카드 강조 — 좌우로 짧게 흔든다(iOS ShakeEffect). [active] 가 true 로 바뀔 때 한 번 재생한다. */
@Composable
private fun Modifier.shake(active: Boolean): Modifier {
    val offset = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        for (x in listOf(-10f, 10f, -8f, 8f, -4f, 4f, 0f)) {
            offset.animateTo(x, androidx.compose.animation.core.tween(durationMillis = 70))
        }
    }
    return this.then(Modifier.graphicsLayer { translationX = offset.value * density })
}
