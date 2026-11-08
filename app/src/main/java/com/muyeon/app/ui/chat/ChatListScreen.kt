package com.muyeon.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.EmptyView

/**
 * 채팅방 리스트 — iOS `ChatListView.swift` 1:1.
 *  상태는 [ChatListState] 가 들고 있다(ChatActivity 에서 생성 — 방을 다녀와도 유지).
 *  이 함수는 렌더링만 한다.
 *
 * ⚠️ iOS 수치: 아바타 48 / 제목 16 semibold / 미리보기 14 / 시간 12 / 안읽음 점 8 / 행 상하 4+.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    state: ChatListState,
    onClose: () -> Unit,
    onOpenRoom: (Int, String) -> Unit,
    onOpenBlocked: () -> Unit,
    /** ★ 2026-11-07 '상담' 칩 — null 이거나 showChip=false 면 예전 그대로. */
    support: SupportChatState? = null,
    onOpenWebPath: (String) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val rooms = state.rooms
    val filtered = rooms.filter { state.filter.matches(it) }
    // 열린 방은 종전 순서 그대로, 잠긴 방은 맨 아래 '종료된 대화' 구역(기본 접힘, 펼침 상태는 기기에 기억).
    val sections = ChatRoomSections.split(filtered)
    var closedExpanded by remember { mutableStateOf(ChatClosedSectionPref.isExpanded(context)) }
    // 스와이프로 [나가기]가 드러난 방 — 한 번에 하나만(다른 방을 밀거나 누르면 닫힌다).
    var openRoomId by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        // 10-27 목록 화면 틀(ui/common/MuyeonListKit) — ✕ + 왼쪽 굵은 제목 + 캡슐 탭(iOS 와 같다).
        com.muyeon.app.ui.common.MuyeonListHeader("채팅", onClose = onClose) {
            // 차단 해제 진입점 — 설정(MY)이 웹이라 차단이 일어나는 이 화면에 둔다.
            com.muyeon.app.ui.common.MuyeonHeaderIcon(Icons.Outlined.DoNotDisturbOn, "차단한 사용자", onClick = onOpenBlocked)
        }
        val showSupport = support?.access?.showChip == true
        LaunchedEffect(showSupport) {
            if (!showSupport && state.filter == ChatRoomFilter.SUPPORT) state.filter = ChatRoomFilter.ALL
        }
        if (rooms.isNotEmpty() || showSupport) {
            com.muyeon.app.ui.common.MuyeonPillTabs(
                ChatRoomFilter.entries.filter { it != ChatRoomFilter.SUPPORT || showSupport }
                    .map { com.muyeon.app.ui.common.MuyeonPillTab(it, it.title) },
                state.filter, { state.filter = it }, tagPrefix = "chat.tab",
            )
        }

        when {
            support != null && showSupport && state.filter == ChatRoomFilter.SUPPORT -> SupportChatList(support, onOpenWebPath)
            state.isLoading && rooms.isEmpty() -> ChatListSkeleton()
            state.loadFailed && rooms.isEmpty() -> LoadFailed { scope.launch { state.retry() } }
            rooms.isEmpty() && !state.hasMore -> EmptyView(stringResource(R.string.chat_list_empty), content = stringResource(R.string.chat_list_empty_desc))
            filtered.isEmpty() && !state.hasMore -> EmptyView(stringResource(R.string.chat_list_empty_tab), content = stringResource(R.string.chat_list_empty_tab_desc))
            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { scope.launch { state.pullToRefresh() } },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (filtered.isEmpty()) item {
                        EmptyView(stringResource(R.string.chat_list_empty_loaded))
                    }
                    // 한 줄(스와이프 나가기 + 방 줄) — 열린 방·종료된 대화 구역이 같이 쓴다.
                    val roomRow: @Composable (ChatRoomSummary) -> Unit = { room ->
                        // iOS allowsFullSwipe:false — 밀면 [나가기]가 드러나고, 그 버튼을 눌러야 나간다(확인 없음, iOS 와 같다).
                        SwipeToLeaveRow(
                            open = openRoomId == room.roomId,
                            onOpenChange = { o -> openRoomId = if (o) room.roomId else if (openRoomId == room.roomId) null else openRoomId },
                            onLeave = {
                                openRoomId = null
                                scope.launch {
                                    if (!state.leave(room.roomId)) {
                                        android.widget.Toast.makeText(context, "채팅방을 나가지 못했어요. 잠시 후 다시 시도해 주세요.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        ) {
                            ChatRoomRow(room) {
                                // 어느 방이든 버튼이 드러나 있으면 누름은 닫기만 한다.
                                if (openRoomId != null) openRoomId = null else onOpenRoom(room.roomId, room.displayTitle)
                            }
                        }
                    }
                    val divider: @Composable () -> Unit = {
                        HorizontalDivider(
                            Modifier.background(MuyeonColors.surface).padding(start = 76.dp),
                            color = MuyeonColors.border,
                        )
                    }
                    itemsIndexed(sections.open, key = { _, r -> r.roomId }) { idx, room ->
                        roomRow(room)
                        if (idx != sections.open.lastIndex) divider()
                    }
                    if (sections.closed.isNotEmpty()) {
                        item(key = "closedHeader") {
                            ClosedSectionHeader(
                                count = sections.closed.size,
                                expanded = closedExpanded,
                                onToggle = {
                                    closedExpanded = !closedExpanded
                                    ChatClosedSectionPref.setExpanded(context, closedExpanded)
                                },
                            )
                        }
                        if (closedExpanded) {
                            itemsIndexed(sections.closed, key = { _, r -> r.roomId }) { idx, room ->
                                roomRow(room)
                                if (idx != sections.closed.lastIndex) divider()
                            }
                        }
                    }
                    if (state.hasMore || state.pageFailed) item {
                        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (state.pageFailed) Text("이전 대화를 불러오지 못했어요.")
                            androidx.compose.material3.TextButton(
                                enabled = !state.loadingMore,
                                onClick = { scope.launch { state.loadMore() } },
                            ) { Text(if (state.loadingMore) "불러오는 중…" else if (state.pageFailed) "다시 시도" else "이전 대화 더 보기") }
                        }
                    }
                }
            }
        }
    }
}

/**
 * iOS `.swipeActions(edge:.trailing, allowsFullSwipe:false)` — 왼쪽으로 밀면 오른쪽에 빨간 [나가기](아이콘+글자)가 드러난다.
 *  끝까지 밀어도 자동으로 나가지 않는다(버튼을 눌러야 나감). 반쯤 넘기면 열리고, 덜 밀면 닫힌다. 다시 밀거나 줄을 누르면 닫힌다.
 */
@Composable
private fun SwipeToLeaveRow(open: Boolean, onOpenChange: (Boolean) -> Unit, onLeave: () -> Unit, content: @Composable () -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val actionWidth = 84.dp
    val maxPx = with(density) { actionWidth.toPx() }
    val offset = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    // 바깥(다른 줄을 열거나 눌러 닫음)에서 바뀐 열림 상태를 따라간다.
    LaunchedEffect(open) { offset.animateTo(if (open) -maxPx else 0f) }
    Box(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // 뒤 — 오른쪽 끝 빨간 [나가기]
        Column(
            Modifier.align(Alignment.CenterEnd).width(actionWidth).fillMaxHeight().background(MuyeonColors.danger)
                .clickable(enabled = open, onClick = onLeave),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.ExitToApp, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text("나가기", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
        }
        // 앞 — 방 줄(가로로 밀림)
        Box(
            Modifier.offset { androidx.compose.ui.unit.IntOffset(offset.value.toInt(), 0) }
                .draggable(
                    orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                    state = androidx.compose.foundation.gestures.rememberDraggableState { d ->
                        scope.launch { offset.snapTo((offset.value + d).coerceIn(-maxPx, 0f)) }
                    },
                    onDragStopped = {
                        val toOpen = offset.value < -maxPx / 2
                        offset.animateTo(if (toOpen) -maxPx else 0f)
                        onOpenChange(toOpen)
                    },
                ),
        ) { content() }
    }
}

/** 채팅방 한 칸 — iOS ChatRoomRow 1:1. */
@Composable
fun ChatRoomRow(room: ChatRoomSummary, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MuyeonColors.surface)
            // 대화 종료 방은 흐리게 — 바탕(불투명 surface)은 그대로 두고 내용만(뒤의 빨간 [나가기]가 비치지 않게).
            .alpha(if (room.locked) LOCKED_ROW_ALPHA else 1f)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuoteAvatar(room.opponent?.image, room.displayTitle, 48.dp)

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    room.displayTitle,
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    lineHeight = 19.sp, color = MuyeonColors.textHead,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                if (room.muted == true) {
                    Icon(Icons.Filled.NotificationsOff, null, tint = MuyeonColors.secondary, modifier = Modifier.size(11.dp))
                }
                room.roleBadge?.let { badge ->
                    val isPro = room.myQuoteRole == "pro"
                    Text(
                        badge,
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
                        lineHeight = 12.sp, color = if (isPro) MuyeonColors.primary else MuyeonColors.secondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isPro) MuyeonColors.primary.copy(alpha = 0.12f) else Color(0xFFE5E5EA))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                // 대화 종료 7일 이내(열린 방) — 'D-N' 경고 pill(역할 배지 '보낸 제안'과 같은 포인트색 pill).
                room.lockWarnPill()?.let { pill ->
                    Text(
                        pill,
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
                        lineHeight = 12.sp, color = MuyeonColors.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MuyeonColors.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("chat.row.lockPill"),
                    )
                }
                room.statusPill?.let { pill ->
                    Text(
                        pill,
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
                        lineHeight = 12.sp, color = MuyeonColors.secondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFE5E5EA))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    chatListTime(room.lastMessageAt),
                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp,
                    color = MuyeonColors.secondary, maxLines = 1,
                )
            }
            // 인스타그램식 — 안읽음이면 미리보기 진하게
            Text(
                room.lastMessage.orEmpty(),
                fontFamily = customFontFamily,
                fontWeight = if (room.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp, lineHeight = 17.sp,
                color = if (room.unreadCount > 0) MuyeonColors.textHead else MuyeonColors.secondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        // 안읽음 점 — 음소거 방은 회색(카톡식)
        if (room.unreadCount > 0) {
            Box(
                Modifier.size(8.dp).clip(CircleShape)
                    .background(if (room.muted == true) Color.Gray else Color(0xFF007AFF)),
            )
        }
    }
}

/** 종료된 대화 줄 투명도. */
private const val LOCKED_ROW_ALPHA = 0.55f

/** '종료된 대화 (N)' 구역 머리 — 누르면 펼치기/접기. 글자는 목록 보조색(시간·미리보기와 같은 secondary). */
@Composable
private fun ClosedSectionHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MuyeonColors.groupedBg)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("chat.closedSection"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            ChatRoomSections.closedTitle(count),
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.secondary, modifier = Modifier.weight(1f),
        )
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            if (expanded) "접기" else "펼치기",
            tint = MuyeonColors.secondary, modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * 목록 시간 — iOS ChatRoomRow.timeText.
 *  ⚠️ 하루가 넘으면 "M.d"(견적 화면의 "yyyy.MM.dd"와 다름). 원본 규칙 유지.
 */
fun chatListTime(iso: String?): String {
    val time = QuoteUi.parseDate(iso) ?: return ""
    val diff = (System.currentTimeMillis() - time) / 1000.0
    if (diff < 60) return "방금"
    if (diff < 3600) return "${(diff / 60).toInt()}분 전"
    if (diff < 86400) return "${(diff / 3600).toInt()}시간 전"
    return java.text.SimpleDateFormat("M.d", java.util.Locale.KOREA).format(java.util.Date(time))
}

/** 최초 로딩 스켈레톤 — 아바타+2줄 8행(카톡식). */
@Composable
private fun ChatListSkeleton() {
    Column(Modifier.fillMaxSize()) {
        repeat(8) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFE5E5EA)))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.width(120.dp).height(13.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE5E5EA)))
                    Box(Modifier.width(200.dp).height(11.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF2F2F7)))
                }
            }
        }
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit) {
    // 공용 빈 목록 안내 + 다시 시도 — 탭 줄 아래 목록 맨 위.
    EmptyView(
        stringResource(R.string.chat_list_failed), content = stringResource(R.string.common_try_later),
        buttonText = stringResource(R.string.common_retry), onButton = onRetry,
    )
}


