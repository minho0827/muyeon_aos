package com.muyeon.app.ui.chat

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
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
fun ChatRoomScreen(vm: ChatRoomViewModel, onBack: () -> Unit) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showAttach by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var reactionTarget by remember { mutableStateOf<ChatMessage?>(null) }

    LaunchedEffect(vm.roomId) { vm.start() }

    // 새 메시지/전송 → 최하단으로
    LaunchedEffect(vm.messages.size, vm.pending.size) {
        val last = vm.messages.size + vm.pending.size - 1
        if (last >= 0) scope.launch { listState.animateScrollToItem(last) }
    }

    // 위로 끝까지 → 이전 페이지
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { if (it <= 1) vm.loadMore() }
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        RoomNavBar(vm, onBack, onReport = { showReport = true })

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
                        item {
                            Box(Modifier.fillMaxWidth().padding(8.dp), Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = MuyeonColors.primary, strokeWidth = 2.dp)
                            }
                        }
                    }
                    items(vm.messages.size) { i ->
                        val m = vm.messages[i]
                        MessageBubble(
                            message = m,
                            isMine = m.senderId == vm.currentUserId,
                            opponentImage = vm.opponentImage,
                            read = isReadByOpponent(m, vm.opponentLastReadAt),
                            isTeacherSide = vm.quoteContext?.isTeacher == true,
                            token = vm.tokenForCards,
                            onReply = { vm.replyingTo = m },
                            onLongPress = { reactionTarget = m },
                            onEdit = { vm.editingMessage = m; vm.onInputChange(m.content) },
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
                                com.muyeon.app.ui.survey.SurveyActivity.start(
                                    context, did, canRespond = m.senderId != vm.currentUserId,
                                )
                            },
                            onOpenLesson = { lid ->
                                com.muyeon.app.ui.lesson.LessonActivity.startDetail(context, lid)
                            },
                        )
                    }
                    items(vm.pending.size) { i -> PendingBubble(vm.pending[i]) { vm.retry(vm.pending[i]) } }
                }
            }

            if (vm.isOtherTyping) {
                Text(
                    "입력 중…",
                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp,
                    color = MuyeonColors.textSub,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 4.dp),
                )
            }
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
            showProposal = vm.quoteContext?.isTeacher == true,
            onPickImages = { uris -> vm.sendImages(context, uris) },
            onPickVideo = { uri -> vm.sendVideo(context, uri) },
            onSurvey = { vm.toast = "설문지는 곧 제공될 기능이에요." },
            onProposal = { vm.toast = "레슨 약속잡기는 곧 제공될 기능이에요." },
            onDismiss = { showAttach = false },
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
    reactionTarget?.let { m ->
        MessageActionSheet(
            message = m,
            isMine = m.senderId == vm.currentUserId,
            onPickEmoji = { emoji -> vm.toggleReaction(m, emoji); reactionTarget = null },
            onCopy = {
                copyToClipboard(context, m.content)
                vm.toast = "메시지를 복사했어요."
                reactionTarget = null
            },
            onReply = { vm.replyingTo = m; reactionTarget = null },
            onEdit = { vm.editingMessage = m; vm.onInputChange(m.content); reactionTarget = null },
            onDelete = { vm.deleteMessage(m); reactionTarget = null },
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
private fun RoomNavBar(vm: ChatRoomViewModel, onBack: () -> Unit, onReport: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(44.dp).background(MuyeonColors.surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            vm.title.ifBlank { "채팅" },
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
            lineHeight = 20.sp, color = MuyeonColors.textHead,
        )
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
            // 신고 — iOS 우상단 ⋯ 메뉴
            Box(Modifier.size(44.dp).clickable(onClick = onReport), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MoreVert, "더보기", tint = MuyeonColors.textHead, modifier = Modifier.size(18.dp))
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
    read: Boolean,
    isTeacherSide: Boolean,
    token: String?,
    onReply: () -> Unit,
    onLongPress: () -> Unit,
    onEdit: () -> Unit,
    onToggleReaction: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onProposalChanged: () -> Unit,
    onOpenProvider: (Int, Boolean) -> Unit,
    onOpenSurvey: (Int) -> Unit,
    onOpenLesson: (Int) -> Unit,
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
                    SurveyUpdateBubble(message.content, onOpenSurvey)
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
                    LessonProposalBubble(message.content, isTeacherSide, token, onProposalChanged)
                }
                return
            }
        }
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isMine) {
            QuoteAvatar(opponentImage, message.sender?.displayName ?: "상대", 32.dp)
            Spacer(Modifier.width(6.dp))
        } else if (read) {
            Text(
                "읽음",
                fontFamily = customFontFamily, fontSize = 10.sp, lineHeight = 12.sp,
                color = MuyeonColors.primary, modifier = Modifier.padding(end = 4.dp, bottom = 2.dp),
            )
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
            Box(
                Modifier
                    .widthIn(max = 260.dp)
                    .then(
                        if (bare) Modifier.clip(RoundedCornerShape(12.dp))
                        else Modifier
                            // 카톡식 꼬리 말풍선 — 수신은 좌상단, 발신은 우상단(iOS ChatBubbleShape).
                            .clip(ChatBubbleShape(mine = isMine))
                            .background(if (isMine) MuyeonColors.primary else Color(0xFFF2F2F7))
                            .combinedClickable(
                                onClick = { if (isMine) onEdit() else onReply() },
                                onLongClick = onLongPress,
                            )
                            // 꼬리가 차지하는 폭만큼 그쪽 여백을 더해 글자가 붙지 않게 한다.
                            .padding(
                                start = if (isMine) 14.dp else 14.dp + BUBBLE_TAIL,
                                end = if (isMine) 14.dp + BUBBLE_TAIL else 14.dp,
                                top = 9.dp, bottom = 9.dp,
                            ),
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
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (message.isEdited && !message.isDeleted) {
                    Text(
                        "수정됨",
                        fontFamily = customFontFamily, fontSize = 10.sp, lineHeight = 12.sp,
                        color = MuyeonColors.secondary,
                    )
                }
                Text(
                    chatListTime(message.createdAt),
                    fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 13.sp,
                    color = MuyeonColors.secondary,
                )
            }
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
                p.content,
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
