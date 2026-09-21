package com.muyeon.app.ui.notification

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
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
import com.muyeon.app.webview.ActiveRole
import com.muyeon.app.webview.WebCallbacks
import com.muyeon.app.ui.quote.QuoteEmptyState
import com.muyeon.app.ui.quote.QuoteHubActivity
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.NativeWebRoute
import kotlinx.coroutines.launch

/**
 * 알림 목록 — iOS `NotificationListView.swift` 이식.
 *  종류 칩 + 안읽음 토글 + 커서 페이징 + 탭 시 읽음 처리 후 딥링크 이동.
 *
 * ⚠️ 칩 목록(라벨·순서·무엇을 보여줄지)은 서버가 활동유형 기준으로 내려준다.
 *    앱에 표를 두지 말 것 — 알림 설정 화면·웹·iOS 와 즉시 어긋난다.
 * ⚠️ 칩은 목록을 좁힐 뿐이고 '전체'에는 모든 알림이 그대로 있다.
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
    var category by remember { mutableStateOf(NotiCategory.ALL) }
    var chips by remember { mutableStateOf<List<NotiCategory>>(emptyList()) }
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

    /** 칩 재조회. 실패하면 기존 칩을 유지한다 — 칩이 통째로 사라지는 쪽이 더 나쁘다. */
    suspend fun loadChips() {
        val fresh = api.categories()
        if (fresh.isEmpty()) return
        chips = fresh
        // 보던 칩이 사라졌으면(유형 전환 등) '전체'로 되돌린다 — 빈 목록에 갇히지 않게.
        if (category != NotiCategory.ALL && fresh.none { it.key == category }) {
            category = NotiCategory.ALL
        }
    }

    LaunchedEffect(unreadOnly, category) { reload() }
    LaunchedEffect(Unit) { loadChips() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(
            title = "알림",
            onClose = onClose,
            // 알림 설정으로 가는 톱니(iOS·PaceERA 와 같은 자리) — 알림이 시끄럽다고 느낀
            //  사람이 설정을 찾으려고 MY 까지 돌아가지 않게, 느낀 자리에서 바로 연다.
            trailing = {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "알림 설정",
                    tint = MuyeonColors.textHead,
                    modifier = Modifier.size(20.dp)
                        .clickable { NotificationSettingsActivity.start(ctx) },
                )
            },
        )

        // 종류 칩(가로 스크롤) + 안읽음 토글 + 모두 읽음. 칩과 안읽음은 서로 직교한 축이다.
        //  여백·치수는 PaceERA 알림 목록 칩 줄(h16 v10 · 간격 8)과 같다.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryChip("전체", 0, category == NotiCategory.ALL) { category = NotiCategory.ALL }
                chips.forEach { c ->
                    CategoryChip(c.label, c.unread, category == c.key) { category = c.key }
                }
            }
            Text(
                if (unreadOnly) "전체 보기" else "안읽음",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                lineHeight = 16.sp,
                color = if (unreadOnly) MuyeonColors.primary else MuyeonColors.textSub,
                modifier = Modifier.clickable { unreadOnly = !unreadOnly },
            )
            Text(
                if (category == NotiCategory.ALL) "모두 읽음" else "이 종류 읽음",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.primary,
                modifier = Modifier.clickable {
                    // 웹 알림 배지도 같이 내려야 한다(iOS .muyeonNotificationsRead → __onNativeNotificationsRead).
                    scope.launch {
                        api.markAllRead(category)
                        WebCallbacks.notificationsRead(ctx)
                        reload()
                        loadChips()
                    }
                },
            )
        }
        HorizontalDivider(color = MuyeonColors.border)

        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                QuoteEmptyState(
                    Icons.Outlined.NotificationsNone,
                    when {
                        unreadOnly -> "안읽은 알림이 없어요"
                        category != NotiCategory.ALL ->
                            "${chips.firstOrNull { it.key == category }?.label ?: "이 종류"} 알림이 없어요"
                        else -> "알림이 없어요"
                    },
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
                                    // 어느 칩의 숫자가 줄어야 하는지는 서버만 안다(type→카테고리 매핑이 서버에 있다).
                                    loadChips()
                                }
                                onOpen(n)
                            }
                        }
                        if (idx != items.lastIndex) HorizontalDivider(color = MuyeonColors.border)

                        // 마지막 항목 도달 → 다음 페이지(커서 = 마지막 id)
                        if (idx == items.lastIndex && !reachedEnd && !loadingMore) {
                            LaunchedEffect(n.id) {
                                loadingMore = true
                                api.list(n.id, 20, unreadOnly, category).onSuccess { more ->
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

/**
 * 종류 칩 하나 — PaceERA `FilterChip` 치수(14/7 · 13sp) + iOS 무용연 칩 색.
 *
 *  ⚠️ 안읽음 수는 별도 뱃지로 얹지 않고 **글자에 붙인다**. 뱃지를 올리면 캡슐 모양이
 *     깨지고, 같은 화면의 iOS 칩과도 달라진다.
 *  ※ 미선택 배경은 PaceERA(테두리)가 아니라 iOS 무용연과 같은 연회색으로 둔다 —
 *     두 무용연 앱을 나란히 놓았을 때 같아 보이는 쪽이 더 중요하다.
 */
@Composable
private fun CategoryChip(label: String, unread: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) MuyeonColors.primary else Color(0xFFF2F2F7))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            if (unread > 0) "$label $unread" else label,
            fontFamily = customFontFamily,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp, lineHeight = 16.sp,
            color = if (selected) Color.White else MuyeonColors.textSub,
        )
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
            val api = remember {
                // 활동유형은 서버가 칩 구성을 정하는 기준이다(X-Active-Type).
                NotificationApi(TokenManager.getAccessToken(this), ActiveRole.current(this))
            }
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
