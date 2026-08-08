package com.muyeon.app.ui.quote

import androidx.compose.animation.animateColorAsState
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
) {
    var tab by remember { mutableIntStateOf(if (isPro) initialTab else 0) }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "견적 요청 내역", onClose = onClose)
        if (isPro) {
            QuoteTabBar(tab = tab, titles = listOf("받은 견적", "보낸 견적"), onSelect = { tab = it })
            HorizontalDivider(color = MuyeonColors.border)
        }
        if (isPro && tab == 1) {
            SentQuotesList(api = api, onOpen = onOpenSent)
        } else {
            RequestedQuotesList(api = api, onOpen = onOpenReceived)
        }
    }
}

/** 상단 내비바 — iOS navigationBar(제목 17 bold, 좌측 X 16 semibold, 높이 44). */
@Composable
fun QuoteNavBar(title: String, onClose: (() -> Unit)? = null, onBack: (() -> Unit)? = null) {
    Box(
        Modifier.fillMaxWidth().height(44.dp).background(MuyeonColors.surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
            lineHeight = 20.sp, color = MuyeonColors.textHead,
        )
        val leading = onBack ?: onClose
        if (leading != null) {
            Box(
                Modifier.align(Alignment.CenterStart).padding(start = 4.dp).size(44.dp).clickable(onClick = leading),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    // iOS 는 네비게이션 기본 back(chevron) / 모달은 xmark — Android 관례에 맞춰 ArrowBack.
                    if (onBack != null) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Close,
                    contentDescription = if (onBack != null) "뒤로" else "닫기",
                    tint = MuyeonColors.textHead,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** pacera식 언더라인 탭바 — iOS QuoteTabBar 1:1(선택 탭 primary + 하단 2dp 인디케이터, 상단 10). */
@Composable
fun QuoteTabBar(tab: Int, titles: List<String>, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        titles.forEachIndexed { idx, title ->
            val on = tab == idx
            val color by animateColorAsState(
                if (on) MuyeonColors.primary else MuyeonColors.textSub, tween(200), label = "tabText",
            )
            Column(
                Modifier.weight(1f).clickable { onSelect(idx) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    title,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = color,
                )
                Box(
                    Modifier.fillMaxWidth().height(2.dp)
                        .background(if (on) MuyeonColors.primary else androidx.compose.ui.graphics.Color.Transparent)
                )
            }
        }
    }
}

// MARK: - 받은 견적(고객 관점)

/** iOS `RequestedQuotesList` 1:1. GET /quotes/me. 행 스와이프 삭제(진행중이면 취소 후 삭제). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestedQuotesList(api: QuoteApi, onOpen: (Int) -> Unit) {
    var quotes by remember { mutableStateOf<List<MyQuoteSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        api.getMyQuotes().onSuccess { quotes = it }
    }

    LaunchedEffect(Unit) { isLoading = true; load(); isLoading = false }

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
            title = "받은 견적이 없어요",
            message = "견적을 요청하면 강사들의 견적이 여기에 쌓여요.",
            modifier = Modifier.fillMaxSize().wrapContentHeight(Alignment.CenterVertically),
        )
    } else {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { refreshing = true; load(); refreshing = false } },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(quotes, key = { _, q -> q.id }) { idx, q ->
                    SwipeToDeleteRow(onDelete = { scope.launch { delete(q) } }) {
                        MyQuoteRow(quote = q, onClick = { onOpen(q.id) })
                    }
                    // iOS List(.plain) 기본 구분선 — 행 좌측 인셋(16 + 아바타 52 + 간격 12)에 맞춘다.
                    if (idx != quotes.lastIndex) RowSeparator()
                }
            }
        }
    }

    if (toast != null) {
        AlertDialog(
            onDismissRequest = { toast = null },
            text = { Text(toast ?: "", fontFamily = customFontFamily, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { toast = null }) { Text("확인") } },
        )
    }
}

/** iOS List(.plain) 행 구분선 — 좌측 인셋 = 좌우여백 16 + 아바타 52 + 간격 12. */
@Composable
private fun RowSeparator() {
    HorizontalDivider(
        Modifier.background(MuyeonColors.surface).padding(start = 80.dp),
        color = MuyeonColors.border,
    )
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
        listOfNotNull(quote.region, "받은 견적 ${quote.responseCount ?: 0}").joinToString(" · ")
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
fun SentQuotesList(api: QuoteApi, onOpen: (SentQuoteItem) -> Unit) {
    var items by remember { mutableStateOf<List<SentQuoteItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() { api.getSentQuotes().onSuccess { items = it } }

    LaunchedEffect(Unit) { isLoading = true; load(); isLoading = false }

    if (items.isEmpty() && !isLoading) {
        QuoteEmptyState(
            icon = Icons.Outlined.Send,
            title = "보낸 견적이 없어요",
            message = "받은 견적요청에 응답하면 여기에 쌓여요.",
            modifier = Modifier.fillMaxSize().wrapContentHeight(Alignment.CenterVertically),
        )
    } else {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { refreshing = true; load(); refreshing = false } },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(items, key = { _, it -> it.id }) { idx, item ->
                    SentQuoteRow(item = item, onClick = { onOpen(item) })
                    if (idx != items.lastIndex) RowSeparator()
                }
            }
        }
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
