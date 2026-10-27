package com.muyeon.app.ui.quote

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.result.launchScreen
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.membership.MembershipActivity
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 자동견적 발송 내역 — iOS `AutoQuoteHistoryView.swift` / 웹 `/autoQuoteHistory` 와 같은 구성.
 *  멤버십의 "자동견적 N회 남음"이 어느 요청에 쓰였는지 보여준다(배포 이후 발송분부터).
 *   - GET /quotes/auto-sent    : 보낸 견적(커서, 첫 페이지에 이번 달 요약)
 *   - GET /quotes/auto-skipped : 횟수 소진으로 못 보낸 요청(커서)
 *  목록은 채팅 서랍처럼 20건씩 — 끝 근처까지 스크롤하면 before=nextCursor 로 이어 붙인다.
 *  웹 `openAutoQuoteHistory` 브릿지(data.month 선택) · 멤버십 · 견적 자동응답 화면에서 진입.
 */

// MARK: - 모델(서버 응답 키 그대로)

/** auto-sent 첫 페이지 요약 — 멤버십 "N회 남음"과 같은 숫자. limit/remaining -1 = 무제한. */
data class AutoSentSummary(
    val period: String?,
    val used: Int,
    val limit: Int?,
    val remaining: Int?,
    val skippedCount: Int,
)

data class AutoSentItem(
    val responseId: Int,
    val quoteId: Int,
    val sentAt: String?,
    val categoryId: String?,
    val region: String?,
    val customer: QuoteCustomer?,
    val status: String?,          // SENT | ACCEPTED | REJECTED
    val readAt: String?,
    val chatRoomId: Int?,
    val quoteStatus: String?,     // OPEN | MATCHED | CLOSED | EXPIRED | CANCELED
    val templateTitle: String?,
) {
    /** 기존 보낸 견적 상세로 넘길 때의 폴백(가격·메시지 없음) — 상세는 /quotes/sent 항목을 우선한다. */
    fun toSentQuoteItem() = SentQuoteItem(
        id = responseId, quoteId = quoteId, price = null, priceAmount = null, priceUnit = null,
        message = null, status = status, chatRoomId = chatRoomId, createdAt = sentAt,
        categoryId = categoryId, region = region, quoteStatus = quoteStatus, customer = customer,
    )

    companion object {
        fun from(o: JSONObject) = AutoSentItem(
            responseId = o.optInt("responseId"), quoteId = o.optInt("quoteId"),
            sentAt = o.stringOrNull("sentAt"), categoryId = o.stringOrNull("categoryId"),
            region = o.stringOrNull("region"), customer = QuoteCustomer.from(o.optJSONObject("customer")),
            status = o.stringOrNull("status"), readAt = o.stringOrNull("readAt"),
            chatRoomId = o.intOrNull("chatRoomId"), quoteStatus = o.stringOrNull("quoteStatus"),
            templateTitle = o.stringOrNull("templateTitle"),
        )
    }
}

data class AutoSentPage(val summary: AutoSentSummary?, val nextCursor: Int?, val items: List<AutoSentItem>) {
    companion object {
        fun from(o: JSONObject): AutoSentPage {
            // 요약은 첫 페이지에만 — used 가 없으면 요약 없음으로 본다.
            val summary = o.intOrNull("used")?.let { used ->
                AutoSentSummary(
                    period = o.stringOrNull("period"), used = used,
                    limit = o.intOrNull("limit"), remaining = o.intOrNull("remaining"),
                    skippedCount = o.intOrNull("skippedCount") ?: 0,
                )
            }
            return AutoSentPage(
                summary, o.intOrNull("nextCursor"),
                o.optJSONArray("items")?.map(AutoSentItem::from) ?: emptyList(),
            )
        }
    }
}

data class AutoSkippedItem(val id: Int, val quoteId: Int, val createdAt: String?, val categoryId: String?, val region: String?) {
    companion object {
        fun from(o: JSONObject) = AutoSkippedItem(
            o.optInt("id"), o.optInt("quoteId"), o.stringOrNull("createdAt"),
            o.stringOrNull("categoryId"), o.stringOrNull("region"),
        )
    }
}

data class AutoSkippedPage(val nextCursor: Int?, val items: List<AutoSkippedItem>) {
    companion object {
        fun from(o: JSONObject) = AutoSkippedPage(
            o.intOrNull("nextCursor"),
            o.optJSONArray("items")?.map(AutoSkippedItem::from) ?: emptyList(),
        )
    }
}

// MARK: - 상태

/** 월 칩 1개 — ym = "YYYY-MM". */
data class AutoQuoteMonth(val ym: String, val label: String)

object AutoQuoteMonths {
    private val KST: TimeZone = TimeZone.getTimeZone("Asia/Seoul")

    fun current(): String = recent(1).first().ym

    /** 최근 n개월(이번 달, 지난 달, "N월" …). */
    fun recent(n: Int = 6): List<AutoQuoteMonth> {
        val cal = Calendar.getInstance(KST)
        return (0 until n).map { i ->
            val c = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, -i) }
            val ym = String.format(Locale.US, "%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
            val label = when (i) {
                0 -> "이번 달"
                1 -> "지난 달"
                else -> "${c.get(Calendar.MONTH) + 1}월"
            }
            AutoQuoteMonth(ym, label)
        }
    }

    fun isValid(ym: String?): Boolean = ym != null && Regex("^\\d{4}-\\d{2}$").matches(ym)
}

/** 화면 상태 — 월이 바뀌면 진행 중이던 응답은 버린다(month 비교). */
class AutoQuoteHistoryState(private val api: QuoteApi, initialMonth: String) {
    var month by mutableStateOf(initialMonth)
        private set
    var summary by mutableStateOf<AutoSentSummary?>(null)
    var sent by mutableStateOf<List<AutoSentItem>>(emptyList())
    var sentLoading by mutableStateOf(true)
    var sentLoadingMore by mutableStateOf(false)
    var skipped by mutableStateOf<List<AutoSkippedItem>>(emptyList())
    var skippedLoading by mutableStateOf(false)
    var skippedLoadingMore by mutableStateOf(false)
    var toast by mutableStateOf<String?>(null)
    private var sentCursor: Int? = null
    private var skippedCursor: Int? = null
    private var skippedLoaded = false

    val skippedCount: Int get() = summary?.skippedCount ?: 0

    fun selectMonth(ym: String) {
        if (ym == month) return
        month = ym
        summary = null
        sent = emptyList(); sentCursor = null
        skipped = emptyList(); skippedCursor = null; skippedLoaded = false
    }

    suspend fun loadSent() {
        val m = month
        sentLoading = sent.isEmpty()
        api.getAutoSentQuotes(m).onSuccess { page ->
            if (m != month) return@onSuccess
            page.summary?.let { summary = it }
            sent = page.items
            sentCursor = page.nextCursor
        }.onFailure { (it as? ApiMessageException)?.message?.let { msg -> toast = msg } }
        if (m == month) sentLoading = false
    }

    suspend fun loadMoreSent() {
        val cursor = sentCursor ?: return
        if (sentLoadingMore || sentLoading) return
        val m = month
        sentLoadingMore = true
        api.getAutoSentQuotes(m, before = cursor).onSuccess { page ->
            if (m != month) return@onSuccess
            val known = sent.map { it.responseId }.toSet()
            sent = sent + page.items.filter { it.responseId !in known }
            sentCursor = page.nextCursor
        }
        sentLoadingMore = false
    }

    suspend fun loadSkipped(force: Boolean = false) {
        if (skippedLoaded && !force) return
        val m = month
        skippedLoading = skipped.isEmpty()
        api.getAutoSkippedQuotes(m).onSuccess { page ->
            if (m != month) return@onSuccess
            skipped = page.items
            skippedCursor = page.nextCursor
            skippedLoaded = true
        }.onFailure { (it as? ApiMessageException)?.message?.let { msg -> toast = msg } }
        if (m == month) skippedLoading = false
    }

    suspend fun loadMoreSkipped() {
        val cursor = skippedCursor ?: return
        if (skippedLoadingMore || skippedLoading) return
        val m = month
        skippedLoadingMore = true
        api.getAutoSkippedQuotes(m, before = cursor).onSuccess { page ->
            if (m != month) return@onSuccess
            val known = skipped.map { it.id }.toSet()
            skipped = skipped + page.items.filter { it.id !in known }
            skippedCursor = page.nextCursor
        }
        skippedLoadingMore = false
    }
}

// MARK: - Activity

/**
 * 웹 `openAutoQuoteHistory` 브릿지 진입점(강사·학원 전용, 무용수 차단).
 *  결과 키 없음(조회 화면) — 여기서 연 멤버십·채팅의 결과는 ResultActivity 가 부모로 흘려보낸다.
 */
class AutoQuoteHistoryActivity : com.muyeon.app.result.ResultActivity() {

    companion object {
        private const val EXTRA_MONTH = "month"

        fun intent(context: Context, month: String? = null): Intent =
            Intent(context, AutoQuoteHistoryActivity::class.java)
                .apply { if (AutoQuoteMonths.isValid(month)) putExtra(EXTRA_MONTH, month) }

        fun start(context: Context, month: String? = null) = context.launchScreen(intent(context, month))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ActiveRole.allowLessonProvider(this)) { finish(); return }
        val initialMonth = intent.getStringExtra(EXTRA_MONTH)?.takeIf { AutoQuoteMonths.isValid(it) }
            ?: AutoQuoteMonths.current()

        setContent {
            val nav = rememberNavController()
            com.muyeon.app.analytics.TrackNavigation("AutoQuoteHistory", nav)
            val api = remember { QuoteApi(TokenManager.getAccessToken(this)) }
            val state = remember { AutoQuoteHistoryState(api, initialMonth) }
            // 상세는 항목을 통째로 넘긴다(QuoteHubActivity 의 sentDetail 과 동일한 이유).
            var sentDetail by remember { mutableStateOf<SentQuoteItem?>(null) }

            NavHost(nav, startDestination = "list") {
                composable("list") {
                    AutoQuoteHistoryScreen(
                        api = api,
                        state = state,
                        onClose = { finish() },
                        onOpenSent = { item -> sentDetail = item; nav.navigate("sent") },
                    )
                }
                composable("sent") {
                    val item = sentDetail
                    if (item == null) nav.popBackStack()
                    else SentQuoteDetailScreen(
                        api = api,
                        item = item,
                        onBack = { nav.popBackStack() },
                        onOpenChat = { roomId ->
                            if (roomId > 0) ChatActivity.startRoom(this@AutoQuoteHistoryActivity, roomId)
                            else Toast.makeText(this@AutoQuoteHistoryActivity, "채팅방을 여는 데 실패했어요.", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }
    }
}

// MARK: - 화면

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoQuoteHistoryScreen(
    api: QuoteApi,
    state: AutoQuoteHistoryState,
    onClose: () -> Unit,
    onOpenSent: (SentQuoteItem) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val months = remember { AutoQuoteMonths.recent(6) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    var opening by remember { mutableStateOf(false) }
    val sentListState = rememberLazyListState()
    val skippedListState = rememberLazyListState()

    // 월이 바뀌면(최초 포함) 요약+보낸 견적을 다시 읽는다. 목록이 이미 있으면(상세 복귀) 건너뛴다.
    LaunchedEffect(state.month) {
        if (state.sent.isEmpty()) {
            state.loadSent()
            sentListState.scrollToItem(0)
        }
    }
    // 못 보낸 요청이 없으면 탭 자체를 숨긴다.
    val showTabs = state.skippedCount > 0
    LaunchedEffect(showTabs) { if (!showTabs) tab = 0 }
    LaunchedEffect(tab, state.month) { if (tab == 1) state.loadSkipped() }

    state.toast?.let { msg ->
        LaunchedEffect(msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            state.toast = null
        }
    }

    fun openSent(item: AutoSentItem) {
        if (opening) return
        scope.launch {
            opening = true
            // 상세의 '내가 보낸 견적' 카드(금액·메시지)는 /quotes/sent 항목에 있다 — 없으면 요약 항목으로 폴백.
            val full = api.getSentQuotes().getOrNull()?.firstOrNull { it.id == item.responseId }
            opening = false
            onOpenSent(full ?: item.toSentQuoteItem())
        }
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        // 10-27 목록 화면 틀(ui/common/MuyeonListKit) — ✕ + 왼쪽 굵은 제목(iOS 와 같다).
        com.muyeon.app.ui.common.MuyeonListHeader("자동견적 발송 내역", onClose = onClose)

        SummaryCard(
            summary = state.summary,
            isCurrentMonth = state.month == months.first().ym,
            monthLabel = months.firstOrNull { it.ym == state.month }?.label ?: state.month,
            onOpenMembership = { MembershipActivity.start(context) },
        )
        MonthChips(months, state.month) { ym -> state.selectMonth(ym) }

        if (showTabs) {
            QuoteTabBar(
                tab = tab,
                titles = listOf("보낸 견적", "못 보낸 요청 ${state.skippedCount}"),
                onSelect = { tab = it },
            )
        } else {
            HorizontalDivider(color = MuyeonColors.border)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    scope.launch {
                        refreshing = true
                        state.loadSent()
                        if (tab == 1) state.loadSkipped(force = true)
                        refreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (tab == 1 && showTabs) {
                    SkippedList(state, skippedListState)
                } else {
                    SentList(state, sentListState, onOpen = ::openSent)
                }
            }
            if (opening) {
                CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.align(Alignment.Center))
            }
        }

        if (tab == 1 && showTabs) {
            HorizontalDivider(color = MuyeonColors.border)
            Text(
                "멤버십 등급 올리기",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MuyeonColors.primary)
                    .clickable { MembershipActivity.start(context) }
                    .padding(vertical = 15.dp),
            )
        }
    }
}

/** 이번 달 사용량 — 멤버십 화면의 "N회 남음"과 같은 숫자. */
@Composable
private fun SummaryCard(
    summary: AutoSentSummary?,
    isCurrentMonth: Boolean,
    monthLabel: String,
    onOpenMembership: () -> Unit,
) {
    val limit = summary?.limit ?: 0
    val noPlan = summary != null && limit == 0
    val text = when {
        summary == null -> " "
        noPlan -> "자동견적은 멤버십에서 이용할 수 있어요"
        limit < 0 -> "$monthLabel 자동견적 ${summary.used}회 사용 · 무제한"
        else -> buildString {
            append("$monthLabel 자동견적 ${summary.used} / ${limit}회 사용")
            if (isCurrentMonth) {
                val remaining = summary.remaining ?: maxOf(0, limit - summary.used)
                append(if (remaining < 0) " · 무제한" else " · ${remaining}회 남음")
            }
        }
    }
    Row(
        Modifier
            .padding(horizontal = 16.dp).padding(top = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MuyeonColors.primary.copy(alpha = 0.07f))
            .then(if (noPlan) Modifier.clickable(onClick = onOpenMembership) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
        )
        if (noPlan) {
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = MuyeonColors.chevron, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun MonthChips(months: List<AutoQuoteMonth>, selected: String, onSelect: (String) -> Unit) {
    // 10-27 월 칩 → 공용 캡슐(작은 크기, 고름=검정). iOS 와 같다.
    Box(Modifier.padding(vertical = 12.dp)) {
        com.muyeon.app.ui.common.MuyeonPillTabs(
            months.map { com.muyeon.app.ui.common.MuyeonPillTab(it.ym, it.label) }, selected, onSelect,
            tagPrefix = "autoquote.month", showDivider = false, compact = true,
        )
    }
}

/** 끝에서 4칸 안쪽이 보이면 다음 페이지 — QuoteBrowse 의 마지막 카드 LaunchedEffect 와 같은 역할. */
@Composable
private fun InfiniteScroll(listState: LazyListState, onLoadMore: suspend () -> Unit) {
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (last, total) -> if (total > 0 && last >= total - 4) onLoadMore() }
    }
}

@Composable
private fun SentList(state: AutoQuoteHistoryState, listState: LazyListState, onOpen: (AutoSentItem) -> Unit) {
    InfiniteScroll(listState) { state.loadMoreSent() }
    val groups = remember(state.sent) { state.sent.groupBy { dayKey(it.sentAt) } }

    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        if (state.sentLoading && state.sent.isEmpty()) {
            item(key = "loading") { CenterSpinner(Modifier.fillParentMaxSize()) }
        } else if (state.sent.isEmpty()) {
            item(key = "empty") {
                QuoteEmptyState(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "아직 자동견적이 나간 요청이 없어요",
                    message = "자동견적을 켜두면 조건에 맞는 요청에 견적이 자동으로 나가고, 여기에 쌓여요.",
                    modifier = Modifier.fillParentMaxSize().wrapContentHeight(Alignment.CenterVertically),
                )
            }
        } else {
            groups.forEach { (day, rows) ->
                item(key = "h-$day") { DateHeader(dayLabel(rows.first().sentAt)) }
                itemsIndexed(rows, key = { _, it -> "s-${it.responseId}" }) { idx, row ->
                    AutoSentRow(row, onClick = { onOpen(row) })
                    if (idx != rows.lastIndex) RowDivider(80.dp)
                }
            }
            if (state.sentLoadingMore) item(key = "more") { BottomSpinner() }
        }
    }
}

@Composable
private fun SkippedList(state: AutoQuoteHistoryState, listState: LazyListState) {
    InfiniteScroll(listState) { state.loadMoreSkipped() }
    val groups = remember(state.skipped) { state.skipped.groupBy { dayKey(it.createdAt) } }

    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        if (state.skippedLoading && state.skipped.isEmpty()) {
            item(key = "loading") { CenterSpinner(Modifier.fillParentMaxSize()) }
        } else if (state.skipped.isEmpty()) {
            item(key = "empty") {
                QuoteEmptyState(
                    icon = Icons.Outlined.Inbox,
                    title = "못 보낸 요청이 없어요",
                    message = "이번 달 자동견적 횟수가 남아 있으면 모두 자동으로 보내져요.",
                    modifier = Modifier.fillParentMaxSize().wrapContentHeight(Alignment.CenterVertically),
                )
            }
        } else {
            groups.forEach { (day, rows) ->
                item(key = "h-$day") { DateHeader(dayLabel(rows.first().createdAt)) }
                itemsIndexed(rows, key = { _, it -> "k-${it.id}" }) { idx, row ->
                    AutoSkippedRow(row)
                    if (idx != rows.lastIndex) RowDivider(16.dp)
                }
            }
            if (state.skippedLoadingMore) item(key = "more") { BottomSpinner() }
        }
    }
}

/** 자동 발송 견적 배지 — 채택 > 마감 > 상담 중 > 열람 > 대기. */
internal fun autoSentBadge(item: AutoSentItem): Pair<String, Color> = when {
    item.status == "ACCEPTED" -> "채택됨" to MuyeonColors.green
    item.status == "REJECTED" ||
        item.quoteStatus in setOf("CLOSED", "EXPIRED", "CANCELED") -> "마감" to MuyeonColors.textSub
    item.chatRoomId != null -> "상담 중" to MuyeonColors.orange
    item.readAt != null -> "열람함" to MuyeonColors.info
    else -> "대기중" to MuyeonColors.primary
}

/** 보낸 견적 1칸 — SentQuoteRow 와 같은 치수(아바타 52, 좌우 16 상하 12). */
@Composable
private fun AutoSentRow(item: AutoSentItem, onClick: () -> Unit) {
    val (badgeText, badgeColor) = autoSentBadge(item)
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
                    item.customer?.displayName ?: "회원",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                QuotePill(badgeText, badgeColor)
                Spacer(Modifier.weight(1f))
                Text(
                    QuoteUi.relativeTime(item.sentAt),
                    fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp,
                    lineHeight = 14.sp, color = MuyeonColors.secondary, maxLines = 1,
                )
            }
            Text(
                listOfNotNull("${QuoteUi.categoryLabel(item.categoryId)} 레슨", item.region, item.templateTitle)
                    .joinToString(" · "),
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.textSub,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.Filled.KeyboardArrowRight, null, tint = MuyeonColors.chevron, modifier = Modifier.size(16.dp))
    }
}

/** 못 보낸 요청 1칸 — 요청자 정보는 없다(견적이 안 나갔으므로). */
@Composable
private fun AutoSkippedRow(item: AutoSkippedItem) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MuyeonColors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                listOfNotNull("${QuoteUi.categoryLabel(item.categoryId)} 레슨", item.region).joinToString(" · "),
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Text(
                QuoteUi.relativeTime(item.createdAt),
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp,
                lineHeight = 14.sp, color = MuyeonColors.secondary, maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            "이번 달 자동견적 횟수를 모두 써서 보내지 못했어요",
            fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub,
        )
    }
}

@Composable
private fun DateHeader(text: String) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
        lineHeight = 16.sp, color = MuyeonColors.textSub,
        modifier = Modifier
            .fillMaxWidth()
            .background(MuyeonColors.groupedBg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun RowDivider(start: androidx.compose.ui.unit.Dp) {
    HorizontalDivider(
        Modifier.background(MuyeonColors.surface).padding(start = start),
        color = MuyeonColors.border,
    )
}

@Composable
private fun CenterSpinner(modifier: Modifier) {
    Box(modifier, Alignment.Center) { CircularProgressIndicator(color = MuyeonColors.primary) }
}

@Composable
private fun BottomSpinner() {
    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), Alignment.Center) {
        CircularProgressIndicator(color = MuyeonColors.primary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
    }
}

// MARK: - 날짜 묶음(오늘/어제/M월 d일) — 기기 시간대 기준.

private fun dayKey(iso: String?): String {
    val t = QuoteUi.parseDate(iso) ?: return "unknown"
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(t))
}

private fun dayLabel(iso: String?): String {
    val t = QuoteUi.parseDate(iso) ?: return ""
    val key = dayKey(iso)
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val now = Calendar.getInstance()
    if (key == fmt.format(now.time)) return "오늘"
    now.add(Calendar.DAY_OF_MONTH, -1)
    if (key == fmt.format(now.time)) return "어제"
    return SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(t))
}
