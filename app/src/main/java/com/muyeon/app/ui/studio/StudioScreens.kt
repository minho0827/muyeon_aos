package com.muyeon.app.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.lesson.LessonTimeFmt
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteEmptyState
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import org.json.JSONObject

/**
 * 스튜디오 운영 — iOS `Studio/StudioMembersView` · `StudioSalesView` · `StudioScheduleView` ·
 *  `StudioMemberDetailView` · `StudioSettingsSheet` 이식.
 */

// ============================================================
// 회원 관리
// ============================================================

private class StudioCursorState<T>(private val keyOf: (T) -> Any) {
    var items by mutableStateOf<List<T>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var busy by mutableStateOf(false)
        private set
    var hasMore by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var cursor: String? = null
    private var key = ""
    private var generation = 0
    private val seen = mutableSetOf<String>()

    suspend fun load(queryKey: String, reset: Boolean, fetch: suspend (String?) -> Result<StudioApi.Page<T>>) {
        if (!reset && (busy || !hasMore || queryKey != key)) return
        if (reset) {
            generation++; key = queryKey; items = emptyList(); cursor = null; seen.clear(); hasMore = false; loading = true
        }
        val version = generation
        busy = true; error = null
        try {
            val result = fetch(if (reset) null else cursor)
            currentCoroutineContext().ensureActive()
            if (version != generation) return
            result.mapCatching { page ->
                require(!page.hasMore || (!page.nextCursor.isNullOrBlank() && page.nextCursor !in seen))
                page
            }.onSuccess { page ->
                items = (items + page.items).distinctBy(keyOf)
                cursor = page.nextCursor
                page.nextCursor?.let { seen.add(it) }
                hasMore = page.hasMore
            }.onFailure { error = "목록을 불러오지 못했어요." }
        } finally {
            if (version == generation) { busy = false; loading = false }
        }
    }
}

@Composable
private fun StudioPageFooter(busy: Boolean, hasMore: Boolean, error: String?, load: () -> Unit) {
    if (hasMore || error != null) Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        error?.let { Text(it, color = MuyeonColors.textSub) }
        androidx.compose.material3.TextButton(enabled = !busy, onClick = load) {
            Text(if (busy) "불러오는 중…" else if (error != null) "다시 시도" else "더 보기")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioMembersScreen(api: StudioApi, onClose: () -> Unit, onOpenMember: (Int) -> Unit) {
    val page = remember { StudioCursorState<StudioMemberSummary> { it.memberId } }
    val members = page.items
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("all") }
    var refreshing by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun load(reset: Boolean = true) = page.load("$tab\u0000$query", reset) { cursor ->
        api.membersPage(query.ifEmpty { null }, tab, cursor)
    }
    LaunchedEffect(query, tab) { kotlinx.coroutines.delay(300); load() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        // 10-27 목록 화면 틀(ui/common/MuyeonListKit) — ‹ + 왼쪽 굵은 제목 + 캡슐 탭(고름=검정).
        com.muyeon.app.ui.common.MuyeonListHeader("회원 관리", onBack = onClose) {
            // iOS StudioMembersView 툴바의 설정 진입점.
            com.muyeon.app.ui.common.MuyeonHeaderIcon(Icons.Outlined.Settings, "스튜디오 설정") { showSettings = true }
        }
        com.muyeon.app.ui.common.MuyeonPillTabs(
            listOf("all" to "전체 회원", "lead" to "상담 문의").map { (v, label) -> com.muyeon.app.ui.common.MuyeonPillTab(v, label) },
            tab, { tab = it }, tagPrefix = "studio.tab", showDivider = false, compact = true,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("이름·연락처 검색", fontFamily = customFontFamily, fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        when {
            page.loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            members.isEmpty() && page.error == null && !page.hasMore -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                QuoteEmptyState(Icons.Outlined.Groups, "회원이 없어요", "수강권을 발급하면 여기에 모여요.")
            }
            else -> PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { scope.launch { refreshing = true; load(); refreshing = false } },
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                    items(members, key = { it.memberId }) { m ->
                        MemberRow(m) { onOpenMember(m.memberId) }
                        HorizontalDivider(Modifier.padding(start = 20.dp), color = MuyeonColors.border)
                    }
                    item {
                        StudioPageFooter(page.busy, page.hasMore, page.error) {
                            scope.launch { load(reset = members.isEmpty()) }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        StudioSettingsSheet(api = api, onDismiss = { showSettings = false })
    }
}

@Composable
private fun MemberRow(m: StudioMemberSummary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuoteAvatar(null, m.name, 44.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    m.name,
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                )
                if (m.isLead) {
                    Text(
                        "상담",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                        lineHeight = 12.sp, color = MuyeonColors.orange,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(MuyeonColors.orange.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                m.grade?.takeIf { it.isNotEmpty() }?.let {
                    Text(
                        it,
                        fontFamily = customFontFamily, fontSize = 10.sp, lineHeight = 12.sp, color = MuyeonColors.textSub,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF2F2F7))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                listOfNotNull(
                    m.pass?.remainText,
                    m.pass?.expireText,
                    "예약 ${m.reservationCount}회",
                ).joinToString(" · "),
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ============================================================
// 회원 상세
// ============================================================

@Composable
fun StudioMemberDetailScreen(api: StudioApi, memberId: Int, onClose: () -> Unit) {
    var detail by remember { mutableStateOf<StudioMemberDetail?>(null) }
    var memo by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var toast by remember { mutableStateOf<String?>(null) }
    var showIssuePass by remember { mutableStateOf(false) }
    var cancelTarget by remember { mutableStateOf<StudioPass?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        api.member(memberId).onSuccess { detail = it; memo = it.memo.orEmpty() }
    }

    LaunchedEffect(memberId) {
        reload()
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "회원 상세", onBack = onClose)

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            return@Column
        }
        val d = detail ?: return@Column

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuoteAvatar(null, d.name, 52.dp)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        d.name,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        lineHeight = 20.sp, color = MuyeonColors.textHead,
                    )
                    Text(
                        listOfNotNull(d.phone, d.grade).joinToString(" · "),
                        fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textSub,
                    )
                }
            }

            // 이용회원 ↔ 상담고객 — iOS 헤더 카드의 세그먼트. 종전 AOS 엔 전환 수단이 없어
            //  상담고객으로 들어온 회원을 앱에서 이용회원으로 돌릴 수 없었다.
            MemberLeadToggle(isLead = d.leadStatus == "LEAD") { lead ->
                scope.launch {
                    api.updateMember(memberId, null, null, if (lead) "LEAD" else "ACTIVE")
                        .onSuccess { reload() }.onFailure { toast = it.message }
                }
            }

            StudioSection("수강권 ${d.passes.size}건", action = "발급" to { showIssuePass = true }) {
                if (d.passes.isEmpty()) {
                    Text(
                        "발급된 수강권이 없어요. '발급'을 눌러 수강권을 등록해 주세요.",
                        fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub,
                    )
                }
                d.passes.forEach { p ->
                    // 취소·만료 건은 흐리게 — iOS opacity 0.6.
                    val inactive = p.status != "ACTIVE"
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp)).padding(12.dp)
                            .alpha(if (inactive) 0.6f else 1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                p.productName,
                                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                lineHeight = 17.sp,
                                color = if (inactive) MuyeonColors.textSub else MuyeonColors.textHead,
                                modifier = Modifier.weight(1f),
                            )
                            if (inactive) {
                                Text(
                                    if (p.status == "CANCELED") "취소됨" else "만료",
                                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                                    lineHeight = 14.sp, color = MuyeonColors.textSub,
                                )
                            } else {
                                Text(
                                    "수강권 취소",
                                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                                    lineHeight = 15.sp, color = MuyeonColors.danger,
                                    modifier = Modifier.clickable { cancelTarget = p },
                                )
                            }
                        }
                        Text(
                            listOfNotNull(
                                p.remainText, p.expireText,
                                StudioDateFmt.won(p.price).takeIf { p.price > 0 },
                            ).joinToString(" · "),
                            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp, color = MuyeonColors.textSub,
                        )
                    }
                }
            }

            StudioSection("최근 예약·출석") {
                if (d.recentReservations.isEmpty()) {
                    Text("최근 예약이 없어요.", fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub)
                }
                d.recentReservations.forEach { r ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                r.title ?: "레슨",
                                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                lineHeight = 17.sp, color = MuyeonColors.textHead,
                            )
                            Text(
                                listOfNotNull(r.date, r.startTime?.let { LessonTimeFmt.ampm(it) }).joinToString(" "),
                                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                                color = MuyeonColors.textSub,
                            )
                        }
                        // 출석 배지 — 종전엔 상태가 안 보여 결석 여부를 알 수 없었다.
                        Text(
                            r.attendance.label,
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            lineHeight = 15.sp, color = Color.White,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(r.attendance.tint)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            StudioSection("메모") {
                OutlinedTextField(
                    value = memo, onValueChange = { memo = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                )
                Text(
                    "메모 저장",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    lineHeight = 17.sp, color = Color.White, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(MuyeonColors.primary)
                        .clickable {
                            scope.launch {
                                api.updateMember(memberId, memo, null, null)
                                    .onSuccess { toast = "저장했어요." }.onFailure { toast = it.message }
                            }
                        }
                        .padding(vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showIssuePass) {
        IssuePassSheet(
            onSubmit = { body ->
                showIssuePass = false
                scope.launch {
                    api.issuePass(memberId, body)
                        .onSuccess { reload(); toast = "수강권을 발급했어요." }
                        .onFailure { toast = it.message ?: "발급하지 못했어요." }
                }
            },
            onDismiss = { showIssuePass = false },
        )
    }

    cancelTarget?.let { p ->
        com.muyeon.app.ui.quote.QuoteDialog(
            "수강권 취소", "'${p.productName}'을 취소할까요? 되돌릴 수 없어요.", "취소하기",
            onConfirm = {
                cancelTarget = null
                scope.launch {
                    api.updatePass(p.id, JSONObject().put("status", "CANCELED"))
                        .onSuccess { reload() }.onFailure { toast = it.message ?: "취소하지 못했어요." }
                }
            },
            onDismiss = { cancelTarget = null },
        )
    }

    toast?.let { msg ->
        com.muyeon.app.ui.quote.QuoteDialog("알림", msg, "확인", onConfirm = { toast = null }, onDismiss = { toast = null })
    }
}

/** 이용회원 ↔ 상담고객 세그먼트 — iOS 헤더 카드의 Picker(.segmented). */
@Composable
private fun MemberLeadToggle(isLead: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(MuyeonColors.groupedBg).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(false to "이용회원", true to "상담고객").forEach { (lead, label) ->
            val on = isLead == lead
            Text(
                label,
                fontFamily = customFontFamily,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 14.sp, lineHeight = 17.sp,
                color = if (on) MuyeonColors.textHead else MuyeonColors.textSub,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(7.dp))
                    .background(if (on) MuyeonColors.surface else Color.Transparent)
                    .clickable(enabled = !on) { onChange(lead) }
                    .padding(vertical = 7.dp),
            )
        }
    }
}

// ============================================================
// 매출
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioSalesScreen(api: StudioApi, onClose: () -> Unit) {
    var summary by remember { mutableStateOf<SalesSummary?>(null) }
    val page = remember { StudioCursorState<StudioSale> { it.id } }
    var refreshing by remember { mutableStateOf(false) }
    val (from, to) = remember { monthRange() }
    val scope = rememberCoroutineScope()
    var summaryGeneration by remember { mutableIntStateOf(0) }
    suspend fun load(reset: Boolean = true) {
        if (reset) { summaryGeneration++; summary = null }
        val version = summaryGeneration
        page.load("$from/$to", reset) fetch@{ cursor ->
            if (reset) {
                val totals = api.salesSummary(from, to)
                currentCoroutineContext().ensureActive()
                if (totals.isFailure) return@fetch Result.failure(totals.exceptionOrNull()!!)
                if (version != summaryGeneration) return@fetch Result.failure(kotlinx.coroutines.CancellationException())
                summary = totals.getOrNull()
            }
            api.salesPage(from, to, cursor)
        }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "매출 현황", onBack = onClose)
        if (page.loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = MuyeonColors.primary) }
        } else PullToRefreshBox(isRefreshing = refreshing,
            onRefresh = { scope.launch { refreshing = true; try { load() } finally { refreshing = false } } },
            modifier = Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                item {
                    Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(MuyeonColors.primary.copy(alpha = 0.07f)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$from ~ $to", fontFamily = customFontFamily, fontSize = 12.sp, color = MuyeonColors.textSub)
                        Text(summary?.let { StudioDateFmt.won(it.total) } ?: "—", fontFamily = customFontFamily,
                            fontWeight = FontWeight.Bold, fontSize = 26.sp, color = MuyeonColors.textHead)
                        if ((summary?.unpaidTotal ?: 0) > 0) Text("미수금 ${StudioDateFmt.won(summary!!.unpaidTotal)}", color = MuyeonColors.danger)
                    }
                }
                item {
                    StudioSection("항목별") {
                        (summary?.byCategory ?: emptyList()).forEach { c ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("${c.label} ${c.count}건", Modifier.weight(1f), fontFamily = customFontFamily, fontSize = 14.sp)
                                Text(StudioDateFmt.won(c.amount), fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                item { Text(if (page.hasMore) "불러온 매출 내역 ${page.items.size}건" else "매출 내역 ${page.items.size}건", fontWeight = FontWeight.Bold) }
                items(page.items, key = { it.id }) { sale ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(sale.title, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MuyeonColors.textHead)
                            Text("${sale.categoryLabel} · ${QuoteUi.relativeTime(sale.soldAt)}", fontFamily = customFontFamily, fontSize = 11.sp, color = MuyeonColors.secondary)
                        }
                        Text(StudioDateFmt.won(sale.amount), fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold)
                    }
                }
                item { StudioPageFooter(page.busy, page.hasMore, page.error) { scope.launch { load(reset = page.items.isEmpty()) } } }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

// ============================================================
// 일정
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScheduleScreen(api: StudioApi, onClose: () -> Unit) {
    var data by remember { mutableStateOf<StudioScheduleData?>(null) }
    var noshowConsumes by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    val (from, to) = remember { monthRange() }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        api.schedule(from, to).onSuccess { data = it }
        api.getSettings().onSuccess { noshowConsumes = it }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "일정 관리", onBack = onClose)

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            return@Column
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { refreshing = true; load(); refreshing = false } },
            modifier = Modifier.weight(1f),
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // 노쇼 차감 설정 — 수강권 회차 차감 여부
                Row(
                    Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF7F7F7)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "노쇼 시 수강권 차감",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            lineHeight = 17.sp, color = MuyeonColors.textHead,
                        )
                        Text(
                            "결석 처리하면 횟수권에서 1회가 빠져요.",
                            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp, color = MuyeonColors.textSub,
                        )
                    }
                    Switch(
                        checked = noshowConsumes,
                        onCheckedChange = { v ->
                            noshowConsumes = v
                            scope.launch { api.updateSettings(v).onFailure { noshowConsumes = !v } }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
                    )
                }

                StudioSection("수업 ${data?.sessions?.size ?: 0}건") {
                    (data?.sessions ?: emptyList()).forEach { s ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    s.title ?: "수업",
                                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                                    lineHeight = 17.sp, color = MuyeonColors.textHead,
                                )
                                Text(
                                    listOfNotNull(s.date, s.startTime).joinToString(" "),
                                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp, color = MuyeonColors.textSub,
                                )
                            }
                            Text(
                                "${s.reservedCount}/${s.capacity}",
                                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                lineHeight = 16.sp, color = MuyeonColors.primary,
                            )
                        }
                    }
                }

                StudioSection("개인 일정 ${data?.blocks?.size ?: 0}건") {
                    (data?.blocks ?: emptyList()).forEach { b ->
                        Text(
                            listOfNotNull(b.date, if (b.allDay) "종일" else b.startTime, b.title).joinToString(" · "),
                            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.body,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/** 이번 달 1일 ~ 말일. */
private fun monthRange(): Pair<String, String> {
    val cal = com.muyeon.app.ui.lesson.kstCalendar()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val from = com.muyeon.app.ui.lesson.kstYmd.format(Date(cal.timeInMillis))
    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    return from to com.muyeon.app.ui.lesson.kstYmd.format(Date(cal.timeInMillis))
}

@Composable
private fun StudioSection(
    title: String,
    /** 우측 액션(라벨 to 동작) — 수강권 '발급' 처럼 섹션 헤더에 붙는 버튼. */
    action: Pair<String, () -> Unit>? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
            )
            action?.let { (label, onClick) ->
                Text(
                    "+ $label",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.primary,
                    modifier = Modifier.clickable(onClick = onClick),
                )
            }
        }
        content()
    }
}
