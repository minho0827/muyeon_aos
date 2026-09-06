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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteEmptyState
import com.muyeon.app.ui.quote.QuoteHubActivity
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.NativeWebRoute
import kotlinx.coroutines.launch

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
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var reachedEnd by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        loading = items.isEmpty()
        reachedEnd = false
        api.list(null, 20, unreadOnly).onSuccess { items = it; reachedEnd = it.size < 20 }
        loading = false
    }

    LaunchedEffect(unreadOnly) { reload() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "알림", onClose = onClose)

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(false to "전체", true to "안 읽음").forEach { (v, label) ->
                val on = unreadOnly == v
                Text(
                    label,
                    fontFamily = customFontFamily,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp, lineHeight = 16.sp,
                    color = if (on) Color.White else MuyeonColors.textSub,
                    modifier = Modifier.clip(RoundedCornerShape(50))
                        .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                        .clickable { unreadOnly = v }.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "모두 읽음",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.primary,
                modifier = Modifier.clickable {
                    scope.launch { api.markAllRead(); reload() }
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
