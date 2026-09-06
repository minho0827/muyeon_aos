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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteEmptyState
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * 스튜디오 운영 — iOS `Studio/StudioMembersView` · `StudioSalesView` · `StudioScheduleView` ·
 *  `StudioMemberDetailView` · `StudioSettingsSheet` 이식.
 */

// ============================================================
// 회원 관리
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioMembersScreen(api: StudioApi, onClose: () -> Unit, onOpenMember: (Int) -> Unit) {
    var members by remember { mutableStateOf<List<StudioMemberSummary>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("ACTIVE") }   // ACTIVE | LEAD
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = members.isEmpty()
        api.members(query.ifEmpty { null }, tab).onSuccess { members = it }
        loading = false
    }

    LaunchedEffect(tab) { load() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "회원 관리", onBack = onClose)

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("ACTIVE" to "수강 회원", "LEAD" to "상담 문의").forEach { (v, label) ->
                val on = tab == v
                Text(
                    label,
                    fontFamily = customFontFamily,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                    color = if (on) Color.White else MuyeonColors.textSub,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
                        .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                        .clickable { tab = v }.padding(vertical = 8.dp),
                )
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("이름·연락처 검색", fontFamily = customFontFamily, fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        LaunchedEffect(query) {
            kotlinx.coroutines.delay(300)   // 입력 디바운스
            load()
        }

        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            members.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
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
                }
            }
        }
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
    val scope = rememberCoroutineScope()

    LaunchedEffect(memberId) {
        api.member(memberId).onSuccess { detail = it; memo = it.memo.orEmpty() }
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

            StudioSection("수강권 ${d.passes.size}건") {
                if (d.passes.isEmpty()) {
                    Text("발급된 수강권이 없어요.", fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub)
                }
                d.passes.forEach { p ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            p.productName,
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            lineHeight = 17.sp, color = MuyeonColors.textHead,
                        )
                        Text(
                            listOfNotNull(p.remainText, p.expireText, StudioDateFmt.won(p.price)).joinToString(" · "),
                            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp, color = MuyeonColors.textSub,
                        )
                    }
                }
            }

            StudioSection("최근 예약") {
                if (d.recentReservations.isEmpty()) {
                    Text("최근 예약이 없어요.", fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub)
                }
                d.recentReservations.forEach { r ->
                    Text(
                        listOfNotNull(r.date, r.startTime, r.title).joinToString(" · "),
                        fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.body,
                    )
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

    toast?.let { msg ->
        com.muyeon.app.ui.quote.QuoteDialog("알림", msg, "확인", onConfirm = { toast = null }, onDismiss = { toast = null })
    }
}

// ============================================================
// 매출
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioSalesScreen(api: StudioApi, onClose: () -> Unit) {
    var summary by remember { mutableStateOf<SalesSummary?>(null) }
    var sales by remember { mutableStateOf<List<StudioSale>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    val (from, to) = remember { monthRange() }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        api.salesSummary(from, to).onSuccess { summary = it }
        api.sales(from, to).onSuccess { sales = it }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "매출 현황", onBack = onClose)

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
                Column(
                    Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(MuyeonColors.primary.copy(alpha = 0.07f)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("$from ~ $to", fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp, color = MuyeonColors.textSub)
                    Text(
                        StudioDateFmt.won(summary?.total ?: 0),
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp,
                        lineHeight = 31.sp, color = MuyeonColors.textHead,
                    )
                    val unpaid = summary?.unpaidTotal ?: 0
                    if (unpaid > 0) {
                        Text(
                            "미수금 ${StudioDateFmt.won(unpaid)}",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            lineHeight = 16.sp, color = MuyeonColors.danger,
                        )
                    }
                }

                StudioSection("항목별") {
                    (summary?.byCategory ?: emptyList()).forEach { c ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${c.label} ${c.count}건",
                                fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 17.sp,
                                color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                            )
                            Text(
                                StudioDateFmt.won(c.amount),
                                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                lineHeight = 17.sp, color = MuyeonColors.textHead,
                            )
                        }
                    }
                }

                StudioSection("매출 내역 ${sales.size}건") {
                    sales.forEach { s ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    s.title,
                                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                                    lineHeight = 17.sp, color = MuyeonColors.textHead,
                                )
                                Text(
                                    "${s.categoryLabel} · ${QuoteUi.relativeTime(s.soldAt)}",
                                    fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 14.sp,
                                    color = MuyeonColors.secondary,
                                )
                            }
                            Text(
                                StudioDateFmt.won(s.amount),
                                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                lineHeight = 17.sp, color = MuyeonColors.textHead,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
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
private fun StudioSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
        )
        content()
    }
}
