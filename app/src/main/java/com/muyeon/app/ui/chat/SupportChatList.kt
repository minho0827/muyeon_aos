package com.muyeon.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ★ 2026-11-07 채팅 목록 '상담' 칩 — 무용연 고객센터 상담방 목록(iOS SupportChatListSection.swift 와 같다).
 *  보이는 조건: GET /support-chat/eligibility 의 showChip(= eligible || hasRooms).
 *   eligible = 관리자 '회원 상담 채팅 열기' 스위치(member_support_open) OR 베타 목록 OR 학원 인증 요구 회원.
 *  방 화면은 웹(/support/chat?room=N) — NativeWebRoute.openWebAndFinish(경로)로 연다.
 *  [새 상담] 은 새 문의를 열 수 있을 때(eligible)만 — /support/chat.
 */
data class SupportChatAccess(val eligible: Boolean, val hasRooms: Boolean, val showChip: Boolean) {
    companion object {
        val NONE = SupportChatAccess(false, false, false)
        fun from(o: JSONObject): SupportChatAccess {
            val e = o.optBoolean("eligible", false)
            val h = o.optBoolean("hasRooms", false)
            return SupportChatAccess(e, h, if (o.has("showChip")) o.optBoolean("showChip", e || h) else e || h)
        }
    }
}

data class SupportChatRoom(
    val id: Int,
    val title: String?,
    val status: String?,
    val lastMessage: String?,
    val lastMessageAt: String?,
    val createdAt: String?,
    val unread: Int,
) {
    companion object {
        private fun JSONObject.str(k: String): String? = if (isNull(k)) null else optString(k).ifEmpty { null }
        fun from(o: JSONObject) = SupportChatRoom(
            id = o.optInt("id"),
            title = o.str("title"),
            status = o.str("status"),
            lastMessage = o.str("lastMessage"),
            lastMessageAt = o.str("lastMessageAt"),
            createdAt = o.str("createdAt"),
            unread = o.optInt("unread", 0),
        )
    }
}

/** ChatActivity 가 들고 있는다(방을 다녀와도 유지). */
class SupportChatState(private val api: ChatApi, private val token: String?) {
    var access by mutableStateOf(cachedFor(token) ?: SupportChatAccess.NONE)
        private set
    var rooms by mutableStateOf<List<SupportChatRoom>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var loadFailed by mutableStateOf(false)
        private set
    var refreshing by mutableStateOf(false)
        private set

    /** 실패는 '안 보임'(다음에 다시 묻는다). 60초 안이면 기억한 값. */
    suspend fun refreshAccess(fresh: Boolean = false) {
        if (token.isNullOrEmpty()) { access = SupportChatAccess.NONE; return }
        if (!fresh) cachedFor(token)?.let { access = it; return }
        val v = api.supportEligibility().getOrNull()
        if (v != null) cache = Triple(token, System.currentTimeMillis(), v)
        access = v ?: SupportChatAccess.NONE
    }

    suspend fun loadRooms() {
        if (!access.showChip) { rooms = emptyList(); return }
        if (rooms.isEmpty()) isLoading = true
        api.supportRooms()
            .onSuccess { rooms = it; loadFailed = false }
            .onFailure { if (rooms.isEmpty()) loadFailed = true }
        isLoading = false
    }

    suspend fun pullToRefresh() {
        refreshing = true
        refreshAccess(fresh = true)
        loadRooms()
        refreshing = false
    }

    companion object {
        private var cache: Triple<String, Long, SupportChatAccess>? = null
        private fun cachedFor(token: String?): SupportChatAccess? {
            val c = cache ?: return null
            return if (c.first == token && System.currentTimeMillis() - c.second < 60_000) c.third else null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportChatList(state: SupportChatState, onOpenWebPath: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { state.loadRooms() }

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = { scope.launch { state.pullToRefresh() } },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            if (state.access.eligible) item {
                NewSupportRow { onOpenWebPath("/support/chat") }
                HorizontalDivider(Modifier.padding(start = 76.dp), color = MuyeonColors.border)
            }
            when {
                state.isLoading && state.rooms.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() }
                }
                state.loadFailed && state.rooms.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().heightIn(min = 360.dp), Alignment.Center) {
                        com.muyeon.app.ui.common.MuyeonEmptyState(
                            "상담 목록을 불러오지 못했어요.", "잠시 후 다시 시도해 주세요.",
                            icon = Icons.Outlined.WifiOff, actionTitle = "다시 시도",
                            onAction = { scope.launch { state.loadRooms() } },
                        )
                    }
                }
                state.rooms.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().heightIn(min = 360.dp), Alignment.Center) {
                        com.muyeon.app.ui.common.MuyeonEmptyState(
                            "상담 내역이 없어요",
                            if (state.access.eligible) "궁금한 점이 있으면 [새 상담]으로 물어 보세요." else null,
                            icon = Icons.Outlined.ChatBubbleOutline,
                        )
                    }
                }
            }
            items(state.rooms, key = { it.id }) { room ->
                SupportChatRoomRow(room) { onOpenWebPath("/support/chat?room=${room.id}") }
                HorizontalDivider(Modifier.padding(start = 76.dp), color = MuyeonColors.border)
            }
        }
    }
}

@Composable
private fun NewSupportRow(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MuyeonColors.surface).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(MuyeonColors.primary.copy(alpha = 0.12f)), Alignment.Center) {
            Icon(Icons.Outlined.AddComment, null, tint = MuyeonColors.primary, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("새 상담", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                lineHeight = 19.sp, color = MuyeonColors.textHead)
            Text("무용연 고객센터에 문의하기", fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 17.sp,
                color = MuyeonColors.secondary)
        }
    }
}

@Composable
private fun SupportChatRoomRow(room: SupportChatRoom, onClick: () -> Unit) {
    val unread = room.unread > 0
    Row(
        Modifier.fillMaxWidth().background(MuyeonColors.surface).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFEBEBEB)), Alignment.Center) {
            Icon(Icons.Outlined.HeadsetMic, null, tint = MuyeonColors.secondary, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("무용연 고객센터", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    lineHeight = 19.sp, color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false))
                if (room.status == "CLOSED") {
                    Text("종료", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
                        lineHeight = 12.sp, color = MuyeonColors.secondary,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFE5E5EA))
                            .padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(chatListTime(room.lastMessageAt ?: room.createdAt), fontFamily = customFontFamily, fontSize = 12.sp,
                    lineHeight = 14.sp, color = MuyeonColors.secondary, maxLines = 1)
            }
            Text(
                room.lastMessage ?: room.title.orEmpty(),
                fontFamily = customFontFamily,
                fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp, lineHeight = 17.sp,
                color = if (unread) MuyeonColors.textHead else MuyeonColors.secondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (unread) Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF007AFF)))
    }
}
