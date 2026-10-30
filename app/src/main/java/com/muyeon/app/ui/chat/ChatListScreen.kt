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
import androidx.compose.material.icons.outlined.DoNotDisturbOn
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
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.launch

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
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val rooms = state.rooms
    val filtered = rooms.filter { state.filter.matches(it) }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        // 10-27 목록 화면 틀(ui/common/MuyeonListKit) — ✕ + 왼쪽 굵은 제목 + 캡슐 탭(iOS 와 같다).
        com.muyeon.app.ui.common.MuyeonListHeader("채팅", onClose = onClose) {
            // 차단 해제 진입점 — 설정(MY)이 웹이라 차단이 일어나는 이 화면에 둔다.
            com.muyeon.app.ui.common.MuyeonHeaderIcon(Icons.Outlined.DoNotDisturbOn, "차단한 사용자", onClick = onOpenBlocked)
        }
        if (rooms.isNotEmpty()) {
            com.muyeon.app.ui.common.MuyeonPillTabs(
                ChatRoomFilter.entries.map { com.muyeon.app.ui.common.MuyeonPillTab(it, it.title) },
                state.filter, { state.filter = it }, tagPrefix = "chat.tab",
            )
        }

        when {
            state.isLoading && rooms.isEmpty() -> ChatListSkeleton()
            state.loadFailed && rooms.isEmpty() -> LoadFailed { scope.launch { state.retry() } }
            rooms.isEmpty() && !state.hasMore -> EmptyMessage(Icons.Outlined.ChatBubbleOutline, "진행 중인 채팅이 없어요", "견적·공고·레슨으로 대화를 시작하면 여기에 모여요.")
            filtered.isEmpty() && !state.hasMore -> EmptyMessage(Icons.Outlined.FilterList, "이 탭에는 채팅이 없어요", "다른 탭을 눌러 보세요.")
            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { scope.launch { state.pullToRefresh() } },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (filtered.isEmpty()) item {
                        Text("현재 불러온 대화 중에는 없어요. 이전 대화를 더 확인해 보세요.", Modifier.padding(20.dp))
                    }
                    itemsIndexed(filtered, key = { _, r -> r.roomId }) { idx, room ->
                        // iOS allowsFullSwipe:false — 끝까지 밀어도 자동 실행 안 되고 버튼을 눌러야 나간다.
                        SwipeToLeaveRow(
                            onLeave = {
                                scope.launch {
                                    if (!state.leave(room.roomId)) {
                                        android.widget.Toast.makeText(context, "채팅방을 나가지 못했어요. 잠시 후 다시 시도해 주세요.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
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
    // 10-27 공용 빈 화면 + 다시 시도 버튼(iOS 와 같다).
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        com.muyeon.app.ui.common.MuyeonEmptyState(
            "채팅 목록을 불러오지 못했어요.", "잠시 후 다시 시도해 주세요.",
            icon = Icons.Outlined.WifiOff, actionTitle = "다시 시도", onAction = onRetry,
        )
    }
}

// 10-27 공용 빈 화면(작은 회색 아이콘 + 제목 + 설명) — iOS ChatRoomListSection.emptyState 와 같다.
@Composable
private fun EmptyMessage(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        com.muyeon.app.ui.common.MuyeonEmptyState(title, message, icon = icon)
    }
}
