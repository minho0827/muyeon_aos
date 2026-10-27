package com.muyeon.app.ui.quote

import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import kotlinx.coroutines.launch

/**
 * 견적 허브 화면 — iOS `QuoteHubView.swift` + `MyQuotesListView.swift` + `SentQuotesView.swift` 1:1.
 *  관점 탭으로 분리(다중역할 사용자의 견적 혼재 해소).
 *   - [받은 견적] (전원): 내가 고객으로서 올린 견적요청 + 강사들이 보낸 견적
 *   - [보낸 견적] (강사·원장 = isPro 만): 내가 고객 요청에 보낸 견적
 *
 * ⚠️ iOS 와 수치를 맞춘다: 탭 15sp bold / 인디케이터 2dp / 행 아바타 52 / 행 좌우 16 상하 8.
 */
@Composable
fun QuoteHubScreen(
    api: QuoteApi,
    isPro: Boolean,
    initialTab: Int,
    onClose: () -> Unit,
    onOpenReceived: (Int) -> Unit,
    onOpenSent: (SentQuoteItem) -> Unit,
    onOpenRecommendBlocks: () -> Unit,
    /** 값이 바뀌면 목록을 다시 읽는다 — 상세에서 돌아올 때 부모가 결과 키(QUOTES)로 올린다. */
    reloadSignal: Int = 0,
) {
    var tab by remember { mutableIntStateOf(if (isPro) initialTab else 0) }

    // 10-27 목록 화면 틀(ui/common/MuyeonListKit) — ✕ + 왼쪽 굵은 제목 + 캡슐 탭, 회색 바탕 위 흰 카드(iOS 와 같다).
    Column(Modifier.fillMaxSize().background(com.muyeon.app.ui.common.MuyeonList.pageBg)) {
        com.muyeon.app.ui.common.MuyeonListHeader("레슨 요청 내역", onClose = onClose) {
            // 추천 제외 해제 진입점 — 채팅의 '차단한 사용자'와 같은 이유로, 제외가 일어나는
            //  추천 카드와 같은 화면 계층에 둔다(설정(MY)은 웹이라 여기가 유일한 자리).
            com.muyeon.app.ui.common.MuyeonHeaderIcon(Icons.Outlined.VisibilityOff, "추천 제외한 강사", onClick = onOpenRecommendBlocks)
        }
        if (isPro) {
            QuoteTabBar(tab = tab, titles = listOf("받은 제안", "보낸 제안"), onSelect = { tab = it })
        }
        if (isPro && tab == 1) {
            SentQuotesList(api = api, onOpen = onOpenSent, reloadSignal = reloadSignal)
        } else {
            RequestedQuotesList(api = api, onOpen = onOpenReceived, reloadSignal = reloadSignal)
        }
    }
}

/** 상단 내비바 — iOS navigationBar(제목 17 bold, 좌측 X 16 semibold, 높이 44). */
@Composable
fun QuoteNavBar(
    title: String,
    onClose: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    /** 우측 액션 슬롯(선택) — 없으면 종전과 동일. */
    trailing: (@Composable () -> Unit)? = null,
) {
    // 10-27 공용 제목줄(ui/common/MuyeonListKit)로 — ‹/✕ + 왼쪽 굵은 제목. 이 부품을 쓰는 화면이 모두 함께 바뀐다(iOS 와 같다).
    //  뒤로 단추 이름은 예전과 같게("뒤로"/"닫기") 둔다.
    com.muyeon.app.ui.common.MuyeonListHeader(
        title, onBack = onBack, onClose = if (onBack == null) onClose else null,
        backTag = if (onBack != null) "뒤로" else "닫기",
    ) { trailing?.invoke() }
}

/** 10-27 밑줄 탭 → 공용 캡슐 탭(MuyeonPillTabs, 고름=검정 + 아래 구분선). iOS QuoteTabBar 와 같다. */
@Composable
fun QuoteTabBar(tab: Int, titles: List<String>, onSelect: (Int) -> Unit) {
    com.muyeon.app.ui.common.MuyeonPillTabs(
        titles.mapIndexed { i, t -> com.muyeon.app.ui.common.MuyeonPillTab(i, t) }, tab, onSelect, tagPrefix = "quote.tab",
    )
}

// MARK: - 받은 견적(고객 관점)

/** iOS `RequestedQuotesList` 1:1. GET /quotes/me. 행 스와이프 삭제(진행중이면 취소 후 삭제). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestedQuotesList(api: QuoteApi, onOpen: (Int) -> Unit, reloadSignal: Int = 0) {
    var quotes by remember { mutableStateOf<List<MyQuoteSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var nextCursor by remember { mutableStateOf<String?>(null) }
    var pageLoading by remember { mutableStateOf(false) }
    var generation by remember { mutableStateOf(0) }
    suspend fun load(reset: Boolean = true) {
        if (!reset && (pageLoading || nextCursor == null)) return
        if (reset) generation++
        val current = generation
        pageLoading = true
        api.getMyQuotesPage(if (reset) null else nextCursor).onSuccess { page ->
            if (current == generation) {
                quotes = if (reset) page.items else (quotes + page.items).distinctBy { it.id }
                nextCursor = page.nextCursor
            }
        }.onFailure { error ->
            if (current == generation) toast = error.message
        }
        if (current == generation) pageLoading = false
    }

    // 최초 + 부모 신호(reloadSignal)마다. 목록이 있으면 스켈레톤 없이 조용히 갈아끼운다.
    LaunchedEffect(reloadSignal) { isLoading = quotes.isEmpty(); load(); isLoading = false }

    // iOS delete(): OPEN 이면 먼저 취소한 뒤 삭제. 매칭 건은 서버가 소프트 삭제(내 목록 숨김).
    suspend fun delete(q: MyQuoteSummary) {
        if (q.status == "OPEN" || q.status == null) api.cancelQuote(q.id)
        api.deleteQuote(q.id)
            .onSuccess { quotes = quotes.filterNot { it.id == q.id } }
            .onFailure {
                toast = it.message?.ifEmpty { null } ?: "삭제에 실패했어요. 잠시 후 다시 시도해 주세요."
                load()
            }
    }

    if (quotes.isEmpty() && !isLoading) {
        QuoteEmptyState(
            icon = Icons.Outlined.Inbox,
            title = "받은 제안이 없어요",
            message = "견적을 요청하면 강사들의 견적이 여기에 쌓여요.",
            modifier = Modifier.fillMaxSize().wrapContentHeight(Alignment.CenterVertically),
        )
    } else {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { refreshing = true; load(); refreshing = false } },
            modifier = Modifier.fillMaxSize(),
        ) {
            // 10-27 회색 바탕 위 흰 카드 한 장씩(줄 사이 선 없음).
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(quotes, key = { _, q -> q.id }) { _, q ->
                    Box(Modifier.clip(com.muyeon.app.ui.common.MuyeonList.cardShape)) {
                        SwipeToDeleteRow(onDelete = { scope.launch { delete(q) } }) {
                            MyQuoteRow(quote = q, onClick = { onOpen(q.id) })
                        }
                    }
                }
                if (nextCursor != null) item {
                    TextButton(enabled = !pageLoading, onClick = { scope.launch { load(false) } }) {
                        Text("더 보기")
                    }
                }
            }
        }
    }

    if (toast != null) {
        com.muyeon.app.ui.common.MuyeonDialog(title = toast ?: "", actions = listOf(com.muyeon.app.ui.common.MuyeonDialogAction("확인")), onDismiss = { toast = null })
    }
}


/** iOS `.swipeActions(edge:.trailing)` 대응 — 좌측 스와이프로만 삭제(allowsFullSwipe: false). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); false } else false
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MuyeonColors.danger).padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, "삭제", tint = androidx.compose.ui.graphics.Color.White)
            }
        },
    ) { content() }
}

/** 내 견적요청 목록 1칸 — iOS MyQuoteRow 1:1. */
@Composable
fun MyQuoteRow(quote: MyQuoteSummary, onClick: () -> Unit) {
    // 만료(cron flip 전 gap 포함) → '견적 마감'
    val effectiveStatus = when {
        quote.status == "EXPIRED" || (quote.expired == true && quote.status == "OPEN") -> "EXPIRED"
        else -> quote.status ?: "OPEN"
    }
    // 지정(1:1) 요청이면 대상 강사 기준, 아니면 응답자(브로드캐스트) 기준.
    val avatarImages = if (quote.isDirect) listOf(quote.targetTeacher?.image) else quote.avatarImages
    val avatarFallback = (if (quote.isDirect) quote.targetTeacher?.displayName else quote.topResponder?.displayName) ?: "강사"
    val subtitle = if (quote.isDirect) {
        val name = quote.targetTeacher?.displayName ?: "강사"
        val progress = when {
            (quote.responseCount ?: 0) > 0 -> "견적 도착"
            effectiveStatus == "EXPIRED" -> "응답 없음"
            effectiveStatus == "MATCHED" -> "매칭 완료"
            else -> "응답 대기중"
        }
        listOfNotNull(quote.region, "$name 강사 지정 요청", progress).joinToString(" · ")
    } else {
        listOfNotNull(quote.region, "받은 제안 ${quote.responseCount ?: 0}").joinToString(" · ")
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(MuyeonColors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuoteAvatarStack(avatarImages, avatarFallback, 52.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${QuoteUi.categoryLabel(quote.categoryId)} 레슨",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (quote.isDirect) QuotePill("지정", MuyeonColors.primary)
                QuotePill(QuoteUi.statusLabel(effectiveStatus), QuoteUi.statusColor(effectiveStatus))
                Spacer(Modifier.weight(1f))
                Text(
                    QuoteUi.relativeTime(quote.lastResponseAt ?: quote.createdAt),
                    fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp,
                    lineHeight = 14.sp, color = MuyeonColors.secondary, maxLines = 1,
                )
            }
            Text(
                subtitle,
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.textSub,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Filled.KeyboardArrowRight, null,
            tint = MuyeonColors.chevron, modifier = Modifier.size(16.dp),
        )
    }
}

// MARK: - 보낸 견적(강사·원장 관점)

/** iOS `SentQuotesList` 1:1. GET /quotes/sent. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentQuotesList(api: QuoteApi, onOpen: (SentQuoteItem) -> Unit, reloadSignal: Int = 0) {
    var items by remember { mutableStateOf<List<SentQuoteItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var toast by remember { mutableStateOf<String?>(null) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var pageLoading by remember { mutableStateOf(false) }
    var generation by remember { mutableStateOf(0) }
    suspend fun load(reset: Boolean = true) {
        if (!reset && (pageLoading || nextCursor == null)) return
        if (reset) generation++
        val current = generation
        pageLoading = true
        api.getSentQuotesPage(if (reset) null else nextCursor).onSuccess { page ->
            if (current == generation) {
                items = if (reset) page.items else (items + page.items).distinctBy { it.id }
                nextCursor = page.nextCursor
            }
        }.onFailure { error ->
            if (current == generation) toast = error.message
        }
        if (current == generation) pageLoading = false
    }


    // 최초 + 부모 신호(reloadSignal)마다. 목록이 있으면 스켈레톤 없이 조용히 갈아끼운다.
    LaunchedEffect(reloadSignal) { isLoading = items.isEmpty(); load(); isLoading = false }

    if (items.isEmpty() && !isLoading) {
        QuoteEmptyState(
            icon = Icons.Outlined.Send,
            title = "보낸 제안이 없어요",
            message = "수강생 요청에 제안을 보내면 여기에 쌓여요.",
            modifier = Modifier.fillMaxSize().wrapContentHeight(Alignment.CenterVertically),
        )
    } else {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { refreshing = true; load(); refreshing = false } },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(items, key = { _, it -> it.id }) { _, item ->
                    Box(Modifier.clip(com.muyeon.app.ui.common.MuyeonList.cardShape)) {
                        SentQuoteRow(item = item, onClick = { onOpen(item) })
                    }
                }
                if (nextCursor != null) item {
                    TextButton(enabled = !pageLoading, onClick = { scope.launch { load(false) } }) {
                        Text("더 보기")
                    }
                }
            }
        }
    }
    if (toast != null) {
        com.muyeon.app.ui.common.MuyeonDialog(title = toast ?: "", actions = listOf(com.muyeon.app.ui.common.MuyeonDialogAction("확인")), onDismiss = { toast = null })
    }
}
/** 내 응답 상태 배지(요청 마감이면 마감 우선) — iOS SentQuoteRow.badge / SentQuoteDetailView.badge 공용. */
internal fun sentQuoteBadge(item: SentQuoteItem): Pair<String, androidx.compose.ui.graphics.Color> = when {
    item.status == "ACCEPTED" -> "채택됨" to MuyeonColors.green   // 대기중(primary)과 구분
    item.status == "REJECTED" -> "거절됨" to MuyeonColors.textSub
    item.quoteStatus == "EXPIRED" || item.quoteStatus == "CANCELED" -> "마감" to MuyeonColors.textSub
    else -> "대기중" to MuyeonColors.primary
}

/** 보낸 견적 1칸 — iOS SentQuoteRow 1:1. */
@Composable
fun SentQuoteRow(item: SentQuoteItem, onClick: () -> Unit) {
    val (badgeText, badgeColor) = sentQuoteBadge(item)
    Row(
        Modifier
            .fillMaxWidth()
            .background(MuyeonColors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuoteAvatar(item.customer?.image, item.customer?.displayName ?: "회원", 52.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${QuoteUi.categoryLabel(item.categoryId)} 레슨",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                QuotePill(badgeText, badgeColor)
                Spacer(Modifier.weight(1f))
                Text(
                    QuoteUi.relativeTime(item.createdAt),
                    fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp,
                    lineHeight = 14.sp, color = MuyeonColors.secondary, maxLines = 1,
                )
            }
            Text(
                listOfNotNull(
                    item.customer?.displayName,
                    item.region,
                    "제시 " + QuoteUi.priceText(item.priceAmount, item.priceUnit, item.price),
                ).joinToString(" · "),
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.textSub,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Filled.KeyboardArrowRight, null,
            tint = MuyeonColors.chevron, modifier = Modifier.size(16.dp),
        )
    }
}
