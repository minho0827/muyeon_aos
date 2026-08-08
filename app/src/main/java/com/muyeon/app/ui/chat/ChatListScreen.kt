package com.muyeon.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.chat.socket.ChatEvent
import com.muyeon.app.chat.socket.ChatEventBus
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.launch

/**
 * 채팅방 리스트 — iOS `ChatListView.swift` 1:1.
 *  GET /chat/rooms + 소켓 실시간 갱신(chat-room-added 증분 upsert / room-updated 안전망 재조회).
 *
 * ⚠️ iOS 수치: 아바타 48 / 제목 16 semibold / 미리보기 14 / 시간 12 / 안읽음 점 8 / 행 상하 4+.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    api: ChatApi,
    initialFilter: ChatRoomFilter,
    onClose: () -> Unit,
    onOpenRoom: (Int, String) -> Unit,
) {
    var rooms by remember { mutableStateOf<List<ChatRoomSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }   // 진입 즉시 스켈레톤(빈화면 깜빡임 방지)
    var loadFailed by remember { mutableStateOf(false) } // '채팅 없음'으로 위장하지 않고 재시도 노출
    var refreshing by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(initialFilter) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        api.getRooms()
            .onSuccess { list ->
                rooms = list.sortedByDescending { it.lastMessageAt ?: "" }
                loadFailed = false
            }
            .onFailure { if (rooms.isEmpty()) loadFailed = true }   // 캐시 있으면 조용히 유지
        isLoading = false
    }

    LaunchedEffect(Unit) { load() }

    // 소켓 — 증분 upsert(재조회 없음) + 안전망 재조회.
    LaunchedEffect(Unit) {
        ChatEventBus.events.collect { e ->
            when (e) {
                is ChatEvent.ChatRoomAdded -> {
                    rooms = (listOf(e.room) + rooms.filterNot { it.roomId == e.room.roomId })
                        .sortedByDescending { it.lastMessageAt ?: "" }
                }
                // 요약 없이 오는 갱신(edit/delete 등)은 전체 재조회.
                is ChatEvent.RoomUpdated -> load()
                else -> Unit
            }
        }
    }

    val filtered = rooms.filter { filter.matches(it) }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "채팅", onClose = onClose)
        if (rooms.isNotEmpty()) {
            ChatFilterSegmented(filter) { filter = it }
        }

        when {
            isLoading && rooms.isEmpty() -> ChatListSkeleton()
            loadFailed && rooms.isEmpty() -> LoadFailed { scope.launch { isLoading = true; load() } }
            rooms.isEmpty() -> EmptyMessage(Icons.Outlined.ChatBubbleOutline, "진행 중인 채팅이 없습니다.")
            filtered.isEmpty() -> EmptyMessage(Icons.Outlined.FilterList, "해당하는 채팅이 없습니다.")
            else -> PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { scope.launch { refreshing = true; load(); refreshing = false } },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(filtered, key = { _, r -> r.roomId }) { idx, room ->
                        // iOS allowsFullSwipe:false — 끝까지 밀어도 자동 실행 안 되고 버튼을 눌러야 나간다.
                        SwipeToLeaveRow(
                            onLeave = {
                                // 낙관적 제거 후 서버 반영, 실패 시 복구.
                                rooms = rooms.filterNot { it.roomId == room.roomId }
                                scope.launch { api.leaveRoom(room.roomId).onFailure { load() } }
                            },
                        ) {
                            ChatRoomRow(room) { onOpenRoom(room.roomId, room.displayTitle) }
                        }
                        if (idx != filtered.lastIndex) {
                            HorizontalDivider(
                                Modifier.background(MuyeonColors.surface).padding(start = 76.dp),
                                color = MuyeonColors.border,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** iOS Picker(.segmented) 대응 — 전체/내 견적요청/견적 응답/일반·문의. */
@Composable
private fun ChatFilterSegmented(current: ChatRoomFilter, onSelect: (ChatRoomFilter) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 12.dp).padding(top = 8.dp, bottom = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEFEFF0))
            .padding(2.dp),
    ) {
        ChatRoomFilter.entries.forEach { f ->
            val on = f == current
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (on) MuyeonColors.surface else Color.Transparent)
                    .clickable { onSelect(f) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    f.title,
                    fontFamily = customFontFamily,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 12.sp, lineHeight = 14.sp, color = MuyeonColors.textHead,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** iOS `.swipeActions(edge:.trailing, allowsFullSwipe:false)` — 나가기. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToLeaveRow(onLeave: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { v -> if (v == SwipeToDismissBoxValue.EndToStart) { onLeave(); false } else false },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MuyeonColors.danger).padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.ExitToApp, "나가기", tint = Color.White)
            }
        },
    ) { content() }
}

/** 채팅방 한 칸 — iOS ChatRoomRow 1:1. */
@Composable
fun ChatRoomRow(room: ChatRoomSummary, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MuyeonColors.surface)
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
                if (room.isQuoteExpired) {
                    Text(
                        "견적 마감",
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
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.WifiOff, null, tint = MuyeonColors.secondary, modifier = Modifier.size(36.dp))
        Text(
            "채팅 목록을 불러오지 못했어요.",
            fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 17.sp, color = MuyeonColors.secondary,
        )
        Text(
            "다시 시도",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            lineHeight = 17.sp, color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(50)).background(MuyeonColors.primary)
                .clickable(onClick = onRetry).padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun EmptyMessage(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = MuyeonColors.secondary, modifier = Modifier.size(40.dp))
        Text(text, fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 17.sp, color = MuyeonColors.secondary)
    }
}
