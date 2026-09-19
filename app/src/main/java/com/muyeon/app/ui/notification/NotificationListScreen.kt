package com.muyeon.app.ui.notification

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.webview.WebCallbacks
import com.muyeon.app.ui.quote.QuoteEmptyState
import com.muyeon.app.ui.quote.QuoteHubActivity
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.NativeWebRoute
import kotlinx.coroutines.launch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size

/**
 * 알림 목록 — iOS `NotificationListView.swift` 이식.
 *  전체/안읽음 탭 + 커서 페이징 + 탭 시 읽음 처리 후 딥링크 이동.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    api: NotificationApi,
    onClose: () -> Unit,
    onOpen: (AppNotification) -> Unit,
) {
    var items by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var unreadOnly by remember { mutableStateOf(false) }
    // 선택된 칩. null = 전체/안읽음(상태 필터), 그 외 = 카테고리 키(서버 필터).
    var category by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var reachedEnd by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        loading = items.isEmpty()
        reachedEnd = false
        api.list(null, 20, unreadOnly, category).onSuccess { items = it; reachedEnd = it.size < 20 }
        loading = false
    }

    LaunchedEffect(unreadOnly, category) { reload() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(
            title = "알림",
            onClose = onClose,
            trailing = {
                Icon(
                    Icons.Outlined.Settings, contentDescription = "알림 설정",
                    tint = MuyeonColors.textHead,
                    modifier = Modifier.padding(end = 12.dp).size(22.dp)
                        .clickable { NotificationSettingsActivity.start(ctx) },
                )
            },
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 상태(전체·안읽음) + 카테고리를 한 줄에 둔다. 서버 계약이 한 번에 하나라
            //  "안 읽은 레슨 알림" 같은 교차 선택을 기대하게 만들지 않는다.
            //  ※ 채팅은 알림함에 이력을 쌓지 않으므로(FCM 직행) 칩에도 두지 않는다.
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                data class Chip(val label: String, val unread: Boolean, val category: String?)
                listOf(
                    Chip("전체", false, null), Chip("안 읽음", true, null),
                    Chip("레슨·예약", false, "LESSON"), Chip("견적·상담", false, "QUOTE"),
                    // 2026-09-19: '공고·지원' 하나를 셋으로 쪼갰다 — 대타만 보고 싶은 강사가 많다.
                    //  (서버는 옛 키 POSTING 도 셋으로 펼쳐 계속 받아 준다 = 구버전 앱 호환)
                    Chip("채용", false, "JOB"), Chip("대타", false, "SUB"),
                    Chip("캐스팅", false, "CASTING"), Chip("활동", false, "ACTIVITY"),
                ).forEach { chip ->
                    val on = unreadOnly == chip.unread && category == chip.category
                    Text(
                        chip.label,
                        fontFamily = customFontFamily,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp, lineHeight = 16.sp,
                        color = if (on) Color.White else MuyeonColors.textSub,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                            .clickable { unreadOnly = chip.unread; category = chip.category }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Text(
                "모두 읽음",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.primary,
                modifier = Modifier.clickable {
                    // 웹 알림 배지도 같이 내려야 한다(iOS .muyeonNotificationsRead → __onNativeNotificationsRead).
                    scope.launch { api.markAllRead(); WebCallbacks.notificationsRead(ctx); reload() }
                },
            )
        }

        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                QuoteEmptyState(
                    Icons.Outlined.NotificationsNone, "알림이 없어요",
                    "새 소식이 오면 여기에 모아드려요.",
                )
            }
            else -> PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { scope.launch { refreshing = true; reload(); refreshing = false } },
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(items, key = { _, n -> n.id }) { idx, n ->
                        NotificationRow(n) {
                            scope.launch {
                                if (!n.isRead) {
                                    api.markRead(n.id)
                                    items = items.map { if (it.id == n.id) it.copy(isRead = true) else it }
                                }
                                onOpen(n)
                            }
                        }
                        if (idx != items.lastIndex) HorizontalDivider(color = MuyeonColors.border)

                        // 마지막 항목 도달 → 다음 페이지(커서 = 마지막 id)
                        if (idx == items.lastIndex && !reachedEnd && !loadingMore) {
                            LaunchedEffect(n.id) {
                                loadingMore = true
                                api.list(n.id, 20, unreadOnly).onSuccess { more ->
                                    items = items + more.filterNot { m -> items.any { it.id == m.id } }
                                    reachedEnd = more.size < 20
                                }
                                loadingMore = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (n.isRead) MuyeonColors.surface else MuyeonColors.primary.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.padding(top = 6.dp).size(7.dp).clip(CircleShape)
                .background(if (n.isRead) Color.Transparent else MuyeonColors.primary),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                n.title,
                fontFamily = customFontFamily,
                fontWeight = if (n.isRead) FontWeight.Medium else FontWeight.Bold,
                fontSize = 14.sp, lineHeight = 18.sp, color = MuyeonColors.textHead,
            )
            n.body?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    it,
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
                    color = MuyeonColors.textSub, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                n.relativeTime,
                fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 14.sp, color = MuyeonColors.secondary,
            )
        }
    }
}

/** 웹 `openNotifications` 브릿지 진입점. */
class NotificationActivity : ComponentActivity() {

    companion object {
        fun start(context: Context) {
            val i = Intent(context, NotificationActivity::class.java)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val api = remember { NotificationApi(TokenManager.getAccessToken(this)) }
            NotificationListScreen(api = api, onClose = { finish() }, onOpen = ::route)
        }
    }

    /**
     * 알림 탭 → 딥링크. data.type 으로 네이티브 화면을 먼저 시도하고,
     *  매핑이 없으면 linkUrl 을 웹 경로로 넘긴다(무반응 금지 규약).
     */
    private fun route(n: AppNotification) {
        val roomId = n.data["roomId"]?.toIntOrNull()
        val quoteId = n.data["quoteId"]?.toIntOrNull()
        when {
            n.data["type"] == "chat_message" && roomId != null -> ChatActivity.startRoom(this, roomId)
            quoteId != null -> QuoteHubActivity.start(
                this, isPro = false, initialTab = 0, quoteId = quoteId,
                responseId = n.data["r"]?.toIntOrNull(),
            )
            !n.linkUrl.isNullOrEmpty() -> NativeWebRoute.openWebAndFinish(this, n.linkUrl)
        }
    }
}
