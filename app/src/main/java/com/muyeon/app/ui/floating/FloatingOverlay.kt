package com.muyeon.app.ui.floating

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.chat.socket.ChatEventBus
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 완료 상태 초록 — iOS Color(red:0.16, green:0.73, blue:0.42). */
internal val VERIFY_GREEN = Color(0xFF29BA6B)

/**
 * 웹뷰 위에 고정되는 네이티브 플로팅 — iOS `FloatingOverlay.swift` 1:1.
 *  - 채팅 플로팅 버튼(우하단, 안읽음 뱃지) → 네이티브 채팅 리스트.
 *  - 인증 상태 책갈피(오른쪽 벽, 상단 30%): 심사중(주황·시계) → 토스트,
 *    완료(초록·체크) → 완료 화면.
 *  로그인·가입 등 인증 화면에서는 둘 다 숨긴다.
 *
 * ⚠️ 웹 라우트와 무관하게 살아 있어야 하므로 웹뷰 위 ComposeView 에 붙인다.
 */
@Composable
fun FloatingOverlay(
    api: FloatingApi,
    onOpenChatList: () -> Unit,
    /** 완료 화면 '시작하기' → 웹에 __onRoleApproved(role) 통지. */
    onRoleApproved: (String) -> Unit,
) {
    var role by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("NONE") }
    var unread by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }
    var showComplete by remember { mutableStateOf(false) }
    var ackTick by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        if (!api.isLoggedIn) {
            // 로그아웃 상태면 뱃지·책갈피 즉시 리셋(옛 계정 잔상 방지).
            role = null; status = "NONE"; unread = 0
            return
        }
        api.verificationStatus()?.let { role = it.role; status = it.status }
        unread = api.unreadCount()
    }

    // 60초 폴링(승인 반영) — iOS Timer.publish(every: 60) 대응.
    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(60_000)
        }
    }
    // 소켓 이벤트(알림·채팅) 즉시 반영 — iOS FloatingSocketManager.onEvent 대응.
    LaunchedEffect(Unit) {
        ChatEventBus.events.collect { refresh() }
    }
    // 로그인 변경 등 외부 신호.
    LaunchedEffect(FloatingState.refreshTick) { refresh() }

    LaunchedEffect(toast) { if (toast != null) { delay(2000); toast = null } }

    val approved = status == "APPROVED"
    val acked = remember(role, ackTick) { role?.let { VerifyAck.isAcked(ctx, it) } ?: false }
    val showBookmark = api.isLoggedIn && role != null &&
        (status == "PENDING" || (approved && !acked))

    Box(Modifier.fillMaxSize()) {
        // 인증 책갈피 — 오른쪽 벽, 상단 30%
        if (showBookmark && !FloatingState.floatingHidden && !FloatingState.isAuthRoute) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = maxHeight * 0.30f - 32.dp)
                        .size(52.dp, 64.dp)
                        .shadow(
                            4.dp,
                            RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                            spotColor = Color.Black.copy(alpha = 0.15f),
                        )
                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                        .background(if (approved) VERIFY_GREEN else MuyeonColors.primary)
                        .clickable {
                            if (approved) showComplete = true else toast = "현재 심사중입니다"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            if (approved) Icons.Filled.Verified else Icons.Filled.Schedule, null,
                            tint = Color.White, modifier = Modifier.size(18.dp),
                        )
                        Text(
                            if (approved) "완료" else "심사중",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                            lineHeight = 12.sp, color = Color.White,
                        )
                    }
                }
            }
        }

        // 채팅 플로팅 — 최상위 탭에서만. 스크롤 다운/웹 모달 시 밀어서 숨김.
        if (api.isLoggedIn && FloatingState.isChatFloatRoute) {
            val hide = FloatingState.hideChatFloat || FloatingState.floatingHidden
            val offset by animateFloatAsState(if (hide) 90f else 0f, tween(220), label = "chatFloat")
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 216.dp)
                    .absoluteOffset(x = offset.dp)
                    .alpha(if (hide) 0f else 1f),
            ) {
                Box(
                    Modifier.size(48.dp)
                        .shadow(6.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.2f))
                        .clip(CircleShape).background(MuyeonColors.primary)
                        .clickable(enabled = !hide) { onOpenChatList() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Forum, "채팅", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                if (unread > 0) {
                    Text(
                        if (unread > 99) "99+" else "$unread",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        lineHeight = 13.sp, color = Color.White, textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.TopEnd)
                            .absoluteOffset(x = 7.dp, y = (-6).dp)
                            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                            .clip(RoundedCornerShape(50)).background(Color.Red)
                            .border(1.5.dp, Color.White, RoundedCornerShape(50))
                            .padding(horizontal = 5.dp)
                            .wrapContentHeight(Alignment.CenterVertically),
                    )
                }
            }
        }

        // 토스트 — 하단 중앙
        toast?.let {
            Text(
                it,
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 17.sp, color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
                    .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
    }

    if (showComplete) {
        val r = role.orEmpty()
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                showComplete = false
                if (r.isNotEmpty()) { VerifyAck.acknowledge(ctx, r); ackTick += 1 }
            },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            VerificationCompleteScreen(role = r) {
                showComplete = false
                if (r.isNotEmpty()) { VerifyAck.acknowledge(ctx, r); ackTick += 1 }
                // 웹뷰를 해당 유형 화면으로 전환(iOS notifyWebRoleApproved).
                scope.launch { onRoleApproved(r) }
            }
        }
    }
}
