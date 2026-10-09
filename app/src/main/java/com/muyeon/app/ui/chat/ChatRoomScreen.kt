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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
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

/**
 * 채팅방 — iOS `ChatRoomView.swift` + `ChatRoomView+Rendering.swift` 이식(코어).
 *  말풍선(좌/우) · 낙관 전송 · 읽음표시 · 입력중 · 답장/수정 · 위로 스크롤 페이징.
 *
 * ⚠️ iOS 수치: 버블 라운드 18 / 내 버블 primary·흰글씨 / 상대 버블 F2F2F7 / 본문 15 /
 *   시간 11 secondary / 아바타 32 / 입력바 상단 구분선 + 전송 버튼 원형 34.
 */
@Composable
fun ChatRoomScreen(
    vm: ChatRoomViewModel,
    onBack: () -> Unit,
    initialSurveyDispatchId: Int? = null,   // 설문 응답 푸시 진입 — 이 설문 카드로 스크롤·강조
    initialProposalId: Int? = null,         // 약속 제안 푸시·웹 진입 — 이 제안 카드로 스크롤·강조
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showAttach by remember { mutableStateOf(false) }
    var showProposal by remember { mutableStateOf(false) }
    var showSurveyPicker by remember { mutableStateOf(false) }
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

    // 새 메시지/전송 → 최하단으로.
    //  메시지 수가 아니라 마지막 메시지 id 를 기준으로 한다 — 이전 메시지를 위에 붙일 때는 내려가지 않는다.
    LaunchedEffect(vm.messages.lastOrNull()?.id, vm.pending.size) {
        val last = vm.messages.size + vm.pending.size - 1
        if (last >= 0) scope.launch { listState.animateScrollToItem(last) }
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
            onCyclePrimary = { handleProgressPrimary(it.progress, it.isTeacher) },
            onLegacyTimeline = { showLegacyTimeline = true },
            onLegacyPrimary = { handleProgressPrimary(it, vm.quoteContext?.isTeacher == true) },
            onProposalChanged = { vm.reloadContext() },
            onOpenProposalPayment = { pid ->
                (context as? Activity)?.let { NativeWebRoute.openWebAndFinish(it, "/lesson-proposals/$pid/payment") }
            },
            onMemberSchedule = { showProposal = true },
            onQuoteHeader = { onQuoteHeader() },
            onOpenLesson = { openLesson(it) },
            onAcceptQuote = { showAcceptConfirm = true },
            onMatchedAction = {
                // iOS 와 같은 분기 — 이 배너는 progress 가 없을 때만 보이므로 실제로는 약속 제안 작성이 열린다.
                val p = vm.progress
                if (vm.isTeacherSide && p != null) openProgressSchedule(p) else showProposal = true
            },
        )

        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFFF7F7F8))) {
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
                        MessageBubble(
                            message = m,
                            isMine = m.senderId == vm.currentUserId,
                            opponentImage = vm.opponentImage,
                            onOpenProfile = { openOpponentProfile() },
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
                            onOpenLink = { url -> openExternal(context, url) },
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

        // 빠른 답변 칩 — 상대(강사)가 등록해둔 질문을 탭 한 번으로 전송.
        if (vm.quickReplies.isNotEmpty() && vm.input.isEmpty()) {
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
                            .clickable { vm.onInputChange(q.text); vm.send() }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }

        ReplyOrEditBanner(vm)
        ChatInputBar(vm, onAttach = { showAttach = true })
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
            onTimeline = { c -> showCyclesSheet = false; timelineCycle = c },
            onPrimary = { c -> showCyclesSheet = false; handleProgressPrimary(c.progress, c.isTeacher) },
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
            onQuoteDetail = if (qc?.isTeacher == false) { { showLegacyTimeline = false; showAcceptConfirm = true } } else null,
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
                { timelineCycle = null; showAcceptConfirm = true }
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
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { teacherReviewInfo = false },
            title = {
                Text(
                    "이 레슨에서는 후기를 쓸 수 없어요",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                )
            },
            text = {
                Text(
                    "이 레슨에서 회원님은 강사예요. 후기는 수강한 회원이 남길 수 있어요.",
                    fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { teacherReviewInfo = false }) {
                    Text("확인", fontFamily = customFontFamily, color = MuyeonColors.primary)
                }
            },
        )
    }
    if (confirmBlock) {
        val scope2 = rememberCoroutineScope()
        com.muyeon.app.ui.quote.QuoteDialog(
            "${vm.title}님을 차단할까요?",
            "서로의 채팅 목록에서 사라지고 더 이상 대화할 수 없어요.\n채팅 목록 우측 상단에서 차단을 해제할 수 있습니다.",
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
            onDone = { showReport = false; vm.toast = "신고가 접수되었어요." },
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
            onDone = { reportMessage = null; vm.toast = "신고가 접수되었어요." },
            onDismiss = { reportMessage = null },
        )
    }
    reactionTarget?.let { m ->
        MessageActionSheet(
            message = m,
            isMine = m.senderId == vm.currentUserId,
            onPickEmoji = { emoji -> vm.toggleReaction(m, emoji); reactionTarget = null },
            onCopy = {
                copyToClipboard(context, m.content)
                // Android 13 이상은 시스템이 복사 확인을 띄우므로 앱 문구는 생략한다(중복 표시 방지).
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                    vm.toast = "메시지를 복사했어요."
                }
                reactionTarget = null
            },
            onReply = { vm.replyingTo = m; reactionTarget = null },
            onEdit = { vm.editingMessage = m; vm.onInputChange(m.content); reactionTarget = null },
            onDelete = { vm.deleteMessage(m); reactionTarget = null },
            onReport = { reportMessage = m; reactionTarget = null },
            // 차단은 우상단 메뉴와 같은 확인 다이얼로그를 띄운다(상대 id 를 모르면 숨김).
            onBlock = if (vm.opponentId > 0) { { confirmBlock = true; reactionTarget = null } } else null,
            onDismiss = { reactionTarget = null },
        )
    }
    vm.toast?.let { msg ->
        LaunchedEffect(msg) { kotlinx.coroutines.delay(2000); vm.toast = null }
    }
}

/**
 * 메시지 길게 누르기 — 이모지 반응(카톡식 6종) + 동작 목록.
 *
 * ⚠️ 종전에는 이모지 반응만 있어서 **복사·삭제를 할 방법이 아예 없었다**
 *   (deleteMessage 는 뷰모델에 있는데 호출부가 없었다).
 *   iOS 컨텍스트 메뉴(복사/답장/수정/삭제)와 같은 구성으로 맞춘다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionSheet(
    message: ChatMessage,
    isMine: Boolean,
    onPickEmoji: (String) -> Unit,
    onCopy: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    onBlock: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    // 삭제된 메시지나 카드형에는 복사·수정이 의미가 없다.
    val isText = message.type == "TEXT" && !message.isDeleted

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf("👍", "❤️", "😂", "😮", "😢", "🙏").forEach { e ->
                Text(
                    e, fontSize = 30.sp, lineHeight = 36.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onPickEmoji(e) }.padding(6.dp),
                )
            }
        }
        HorizontalDivider(color = MuyeonColors.border)
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            if (isText) MessageAction("복사", MuyeonColors.textHead, onCopy)
            if (!message.isDeleted) MessageAction("답장", MuyeonColors.textHead, onReply)
            if (isMine && isText) MessageAction("수정", MuyeonColors.textHead, onEdit)
            if (isMine && !message.isDeleted) MessageAction("삭제", MuyeonColors.danger, onDelete)
            // 상대 메시지 — 신고·차단(Apple 1.2 / 구글 UGC 정책: 문제 메시지에서 바로 신고·차단)
            if (!isMine && !message.isDeleted) MessageAction("신고하기", MuyeonColors.danger, onReport)
            if (!isMine) onBlock?.let { MessageAction("이 사용자 차단", MuyeonColors.danger, it) }
        }
    }
}

@Composable
private fun MessageAction(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
        lineHeight = 18.sp, color = color,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    )
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

/** 답장/수정 대상 배너 — 입력바 위에 붙는다. */
@Composable
private fun ReplyOrEditBanner(vm: ChatRoomViewModel) {
    val reply = vm.replyingTo
    val edit = vm.editingMessage
    if (reply == null && edit == null) return
    Row(
        Modifier.fillMaxWidth().background(Color(0xFFF2F2F7)).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (edit != null) "메시지 수정" else "답장",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                lineHeight = 14.sp, color = MuyeonColors.primary,
            )
            Text(
                (edit ?: reply)?.content.orEmpty(),
                fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                color = MuyeonColors.textSub, maxLines = 1,
            )
        }
        Icon(
            Icons.Filled.Close, "취소", tint = MuyeonColors.secondary,
            modifier = Modifier.size(16.dp).clickable { vm.replyingTo = null; vm.editingMessage = null },
        )
    }
}

@Composable
private fun ChatInputBar(vm: ChatRoomViewModel, onAttach: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MuyeonColors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 첨부(+) — iOS 입력바 좌측 '+' 버튼.
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(50)).background(Color(0xFFF2F2F7))
                .clickable(onClick = onAttach),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, "첨부", tint = MuyeonColors.textSub, modifier = Modifier.size(18.dp))
        }
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF2F2F7))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (vm.input.isEmpty()) {
                Text(
                    "메시지를 입력하세요",
                    fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 20.sp,
                    color = MuyeonColors.secondary,
                )
            }
            BasicTextField(
                value = vm.input,
                onValueChange = vm::onInputChange,
                textStyle = TextStyle(
                    fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 20.sp,
                    color = MuyeonColors.textHead,
                ),
                cursorBrush = SolidColor(MuyeonColors.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val canSend = vm.input.isNotBlank()
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(50))
                .background(if (canSend) MuyeonColors.primary else Color(0xFFD1D1D6))
                .clickable(enabled = canSend) { vm.send() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, "전송", tint = Color.White, modifier = Modifier.size(17.dp))
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
                    // 제공자 본인이 보낸 카드엔 자기 프로필 버튼을 노출하지 않는다.
                    QuoteCardBubble(message.content, showProviderProfile = !isMine, onOpenProvider = onOpenProvider)
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
            // 아바타 탭 → 상대 공개 프로필(iOS 상단바·말풍선 아바타와 같은 목적지).
            QuoteAvatar(
                opponentImage, message.sender?.displayName ?: "상대", 32.dp,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onOpenProfile),
            )
            Spacer(Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            // 답장 인용
            message.replyTo?.let { r ->
                Text(
                    "${r.senderName ?: "상대"}: ${r.content.orEmpty()}",
                    fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 14.sp,
                    color = MuyeonColors.secondary, maxLines = 1,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            // 이미지 말풍선은 배경 없이 사진만 보여준다(iOS ChatImageBubble — 말풍선 밖).
            val bare = message.type == "IMAGE" && !message.isDeleted
            Row(verticalAlignment = Alignment.Bottom) {
                if (isMine) {
                    BubbleMeta(message, read = read, alignEnd = true)
                    Spacer(Modifier.width(4.dp))
                }
                Box(
                    Modifier
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
                            "삭제된 메시지입니다.",
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
                        else -> Text(
                            message.content,
                            fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 20.sp,
                            color = if (isMine) Color.White else MuyeonColors.textHead,
                        )
                    }
                }
                if (!isMine) {
                    Spacer(Modifier.width(4.dp))
                    BubbleMeta(message, read = false, alignEnd = false)
                }
            }
            // 텍스트 안 URL 미리보기(카톡식)
            if (!message.isDeleted && message.type == "TEXT") {
                ChatLinkDetector.firstUrl(message.content)?.let { url ->
                    Spacer(Modifier.height(4.dp))
                    LinkPreviewCard(url, onOpenLink)
                }
            }
            // 이모지 반응 집계
            message.reactions?.takeIf { it.isNotEmpty() }?.let { list ->
                Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    list.forEach { r ->
                        Text(
                            "${r.emoji} ${r.count}",
                            fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 14.sp,
                            color = if (r.mine) MuyeonColors.primary else MuyeonColors.textSub,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (r.mine) MuyeonColors.primary.copy(alpha = 0.12f) else Color(0xFFF2F2F7)
                                )
                                .clickable { onToggleReaction(r.emoji) }
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 말풍선 옆 메타 — 읽음(내 메시지만)·수정됨·시간. alignEnd 는 내 메시지(말풍선 왼쪽에 놓임)일 때 true. */
@Composable
private fun BubbleMeta(message: ChatMessage, read: Boolean, alignEnd: Boolean) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        modifier = Modifier.padding(bottom = 2.dp),
    ) {
        if (read) {
            Text(
                "읽음",
                fontFamily = customFontFamily, fontSize = 10.sp, lineHeight = 12.sp,
                color = MuyeonColors.primary,
            )
        }
        if (message.isEdited && !message.isDeleted) {
            Text(
                "수정됨",
                fontFamily = customFontFamily, fontSize = 10.sp, lineHeight = 12.sp,
                color = MuyeonColors.secondary,
            )
        }
        Text(
            chatListTime(message.createdAt),
            fontFamily = customFontFamily, fontSize = 10.sp, lineHeight = 12.sp,
            color = MuyeonColors.secondary,
        )
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
