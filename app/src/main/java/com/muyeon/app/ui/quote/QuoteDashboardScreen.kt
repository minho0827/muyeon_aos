package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

/**
 * 레슨 견적관리 허브 대시보드 — iOS `QuoteDashboardView.swift` 1:1.
 *   ① 상단 요약: 그룹형(시안)이면 '오늘 할 일' 카드, 아니면 2×2 컬러 통계 타일
 *   ② '일정 확정 필요' 배너(강사) + 다가오는 일정(최대 5, 비면 숨김)
 *   ③ 기능 목록 — group 지정 시 섹션 3열 카드 그리드, 아니면 리스트
 *  데이터: GET /quotes/dashboard?role=teacher|customer (1왕복).
 */

/** 기능 카드/행 1개 — iOS QuoteDashFunction. */
data class QuoteDashFunction(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val group: String? = null,   // 지정 시 섹션 3열 카드 그리드(시안). null 이면 리스트.
    val badge: String? = null,   // 상태 뱃지(자동응답 "켜짐"/"꺼짐")
    val badgeOn: Boolean = false,
    val action: () -> Unit,
)

/** 통계 타일 1개 — iOS DashTile. */
private data class DashTile(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val count: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDashboardScreen(
    api: QuoteApi,
    title: String,
    role: String,                       // "teacher" | "customer"
    functions: List<QuoteDashFunction>,
    onClose: () -> Unit,
    onTileAction: (String) -> Unit,     // today/new/sent/accepted | reservations/unread/open/done
    onUpcomingTap: (QuoteDashUpcoming) -> Unit,
) {
    var data by remember { mutableStateOf<QuoteDashboardData?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() { api.getQuoteDashboard(role).onSuccess { data = it } }

    LaunchedEffect(role) { load() }

    // 기능 카드에 group 이 있으면 시안 섹션 레이아웃(오늘 할 일 카드 + 3열 그리드)
    val isSectioned = functions.any { it.group != null }
    val tiles = remember(data, role) { dashTiles(role, data) }

    Column(Modifier.fillMaxSize().background(MuyeonColors.groupedBg)) {
        // topBar — 제목 17 bold 가운데, 좌측 X 16 semibold(44 터치)
        Box(Modifier.fillMaxWidth().background(MuyeonColors.surface).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
            Text(
                title,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                lineHeight = 20.sp, color = MuyeonColors.textHead,
            )
            Box(
                Modifier.align(Alignment.CenterStart).size(44.dp).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, "닫기", tint = MuyeonColors.textHead, modifier = Modifier.size(16.dp))
            }
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { refreshing = true; load(); refreshing = false } },
            modifier = Modifier.weight(1f),
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (isSectioned) TodayCard(tiles.take(2), onTileAction) else TileGrid(tiles, onTileAction)

                // 강사 — 채택됐지만 날짜 미정인 레슨(놓치기 쉬운 액션) 배너
                val pending = data?.pendingSchedules ?: 0
                if (role == "teacher" && pending > 0) PendingBanner(pending) { onTileAction("pending") }

                val upcoming = data?.upcoming ?: emptyList()
                if (upcoming.isNotEmpty()) UpcomingSection(upcoming, onUpcomingTap)

                if (isSectioned) GroupedFunctionSections(functions) else LegacyFunctionList(functions)
            }
        }
    }
}

/**
 * 타일 정의 — 역할별 4종(미리 알림 톤: 파랑/주황/짙은회색/초록).
 *  아이콘은 iOS SF Symbols 대응: calendar → CalendarMonth / bell → Notifications /
 *  paperplane.fill → Send / tray.and.arrow.down.fill → Inbox / checkmark.circle.fill → CheckCircle.
 */
private fun dashTiles(role: String, d: QuoteDashboardData?): List<DashTile> =
    if (role == "teacher") listOf(
        DashTile("today", "오늘 레슨", Icons.Filled.CalendarMonth, Color(0xFF007AFF), d?.todayLessons ?: 0),
        DashTile("new", "새 견적요청", Icons.Filled.Notifications, Color(0xFFFF9500), d?.newRequests ?: 0),
        DashTile("sent", "대기 중 견적", Icons.AutoMirrored.Filled.Send, Color(0xFF555555), d?.sentPending ?: 0),
        DashTile("accepted", "채택됨", Icons.Filled.CheckCircle, Color(0xFF34C759), d?.accepted ?: 0),
    ) else listOf(
        DashTile("reservations", "다가오는 예약", Icons.Filled.CalendarMonth, Color(0xFF007AFF), d?.upcomingReservations ?: 0),
        DashTile("unread", "새 견적", Icons.Filled.Inbox, Color(0xFFFF9500), d?.unreadQuotes ?: 0),
        DashTile("open", "진행 중 요청", Icons.AutoMirrored.Filled.Send, Color(0xFF555555), d?.openRequests ?: 0),
        DashTile("done", "완료", Icons.Filled.CheckCircle, Color(0xFF34C759), d?.doneLessons ?: 0),
    )

/** 2×2 컬러 통계 타일 — iOS tileGrid(최소높이 86, r16, 색 0.92). */
@Composable
private fun TileGrid(tiles: List<DashTile>, onTileAction: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { tile ->
                    Column(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 86.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(tile.color.copy(alpha = 0.92f))
                            .clickable { onTileAction(tile.id) }
                            .padding(12.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier.size(32.dp).clip(CircleShape).background(Color.White),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(tile.icon, null, tint = tile.color, modifier = Modifier.size(17.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${tile.count}",
                                fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, color = Color.White,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            tile.label,
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            lineHeight = 17.sp, color = Color.White.copy(alpha = 0.95f),
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** 시안 상단 '오늘 할 일' 카드 — iOS todayCard(연한 피치 배경 r16, 지표 2). */
@Composable
private fun TodayCard(metrics: List<DashTile>, onTileAction: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MuyeonColors.primary.copy(alpha = 0.07f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { onTileAction(metrics.firstOrNull()?.id ?: "today") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "오늘 할 일",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = MuyeonColors.textHead,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "자세히 보기",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.textSub,
            )
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = MuyeonColors.textSub, modifier = Modifier.size(11.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            metrics.forEach { tile ->
                // 지표 셀 — 원형 아이콘(46) + (라벨 / N건)
                Row(
                    Modifier.weight(1f).clickable { onTileAction(tile.id) },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(46.dp).clip(CircleShape).background(MuyeonColors.surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(tile.icon, null, tint = MuyeonColors.primary, modifier = Modifier.size(20.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            tile.label,
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                            lineHeight = 16.sp, color = MuyeonColors.textSub,
                        )
                        Text(
                            "${tile.count}건",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                            lineHeight = 24.sp, color = MuyeonColors.textHead,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingBanner(count: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MuyeonColors.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Error, null, tint = MuyeonColors.orange, modifier = Modifier.size(17.dp))
        Text(
            "일정 확정이 필요한 레슨 ${count}건",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead,
        )
        Spacer(Modifier.weight(1f))
        Icon(Icons.Filled.KeyboardArrowRight, null, tint = MuyeonColors.secondary, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun UpcomingSection(items: List<QuoteDashUpcoming>, onTap: (QuoteDashUpcoming) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "다가오는 일정",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = MuyeonColors.textSub,
        )
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MuyeonColors.surface)) {
            items.forEachIndexed { idx, item ->
                Row(
                    Modifier.fillMaxWidth().clickable { onTap(item) }.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            listOfNotNull(item.date, item.time).joinToString(" "),
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                            lineHeight = 14.sp, color = MuyeonColors.primary,
                        )
                        Text(
                            listOfNotNull(item.title, item.counterpart).joinToString(" · "),
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            lineHeight = 17.sp, color = MuyeonColors.textHead,
                        )
                    }
                    Icon(Icons.Filled.KeyboardArrowRight, null, tint = MuyeonColors.secondary, modifier = Modifier.size(12.dp))
                }
                if (idx != items.lastIndex) HorizontalDivider(Modifier.padding(start = 14.dp), color = MuyeonColors.border)
            }
        }
    }
}

/** 기능 리스트(그룹 미지정 시) — iOS legacyFunctionList. */
@Composable
private fun LegacyFunctionList(functions: List<QuoteDashFunction>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "관리 기능",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = MuyeonColors.textSub,
        )
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MuyeonColors.surface)) {
            functions.forEachIndexed { idx, fn ->
                Row(
                    Modifier.fillMaxWidth().clickable { fn.action() }.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(MuyeonColors.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(fn.icon, null, tint = MuyeonColors.primary, modifier = Modifier.size(15.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            fn.title,
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            lineHeight = 18.sp, color = MuyeonColors.textHead,
                        )
                        fn.subtitle?.let {
                            Text(
                                it,
                                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp,
                                lineHeight = 14.sp, color = MuyeonColors.textSub,
                            )
                        }
                    }
                    Icon(Icons.Filled.KeyboardArrowRight, null, tint = MuyeonColors.secondary, modifier = Modifier.size(12.dp))
                }
                if (idx != functions.lastIndex) HorizontalDivider(Modifier.padding(start = 56.dp), color = MuyeonColors.border)
            }
        }
    }
}

/** 섹션 3열 카드 그리드(시안) — iOS groupedFunctionSections. 그룹 등장 순서 유지. */
@Composable
private fun GroupedFunctionSections(functions: List<QuoteDashFunction>) {
    val groups = remember(functions) {
        val seen = linkedSetOf<String>()
        functions.forEach { fn -> fn.group?.let { seen.add(it) } }
        seen.toList()
    }
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        groups.forEach { grp ->
            val cards = functions.filter { it.group == grp }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    grp,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    lineHeight = 20.sp, color = MuyeonColors.textHead,
                )
                // LazyVerticalGrid 는 스크롤 Column 안에서 높이가 불확정이라 고정 행으로 배치.
                cards.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { fn -> FunctionCard(fn, Modifier.weight(1f)) }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** 카드 — 아이콘 위, 제목(굵게), 서브(작은 회색). iOS functionCard(minHeight 108, r14). */
@Composable
private fun FunctionCard(fn: QuoteDashFunction, modifier: Modifier = Modifier) {
    Column(
        modifier
            .heightIn(min = 108.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MuyeonColors.surface)
            .clickable { fn.action() }
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(fn.icon, null, tint = MuyeonColors.primary, modifier = Modifier.height(30.dp).size(24.dp))
        // 상태 뱃지 — 자동응답 켜짐/꺼짐을 카드에서 바로 확인.
        fn.badge?.let { badge ->
            Text(
                badge,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                lineHeight = 12.sp, color = if (fn.badgeOn) Color.White else MuyeonColors.textSub,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (fn.badgeOn) MuyeonColors.primary else Color(0xFFE5E5EA))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
        Text(
            fn.title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead, textAlign = TextAlign.Center,
        )
        fn.subtitle?.let {
            Text(
                it,
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp,
                lineHeight = 13.sp, color = MuyeonColors.textSub, textAlign = TextAlign.Center,
            )
        }
    }
}
